package distributed

import distributed.CityGrid
import distributed.Message
import distributed.ViewCommand
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector, SupervisorStrategy, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.util.Timeout
import scala.util.{Random, Success}
import concurrent.duration.DurationInt
import scala.collection.mutable.ListBuffer

sealed trait SensorCommand extends Message
case class Update() extends SensorCommand
case class ReconnectToGUI() extends SensorCommand
case class IsSensorInAlarmRequest(replyTo: ActorRef[SensorCommand]) extends SensorCommand
case class IsSensorInAlarmResponse(sensorState: SensorState) extends SensorCommand
case class FireStationRegistered(fireStation: List[ActorRef[FireStationCommand]]) extends SensorCommand
case class OtherSensorRegistered(otherSensors: List[ActorRef[SensorCommand]]) extends SensorCommand
case class MyZoneSensorRequest(sensorToReply: ActorRef[SensorCommand], zone: String) extends SensorCommand
case class MyZoneSensorResponse(sensorRef: ActorRef[SensorCommand]) extends SensorCommand
case class MyStationResponse(ref: ActorRef[FireStationCommand]) extends SensorCommand
case class GetSensorInfo(ctx: ActorRef[ViewCommand | Receptionist.Listing]) extends SensorCommand

enum SensorState {
  case OK
  case Warning
  case Disconnected
}

object SensorActor:

  // qua ci vanno le variabile che sono UNICHE per tutti i sensori
  val sensorKey: ServiceKey[SensorCommand] = ServiceKey[SensorCommand]("sensor")
  var viewActor: Option[ActorRef[ViewCommand | Receptionist.Listing]] = None

  // in una prima fase il sensore non sa nulla sugli altri sensori e quindi in fase di istanziazione dovrà
  // farsi dare il riferimento (in questa fase supponiamo che i sensori non muoiano)

  // update avviene ogni 15/20 secondi così il sensore ha il tempo ricevere tutti i messaggi e non rischia nel frattempo
  // di morire
  // una volta che i sensori di una zona hanno il riferimento di tutti gli altri i casi sono:
  // -> sensore OK non fa nulla
  // -> sensore WARNING invia messaggio di warning ai sensori della stessa zona chiedendogli come sono messi loro e
  //    quando arrivano i messaggi controlla se sono in warning o no:
  //      -> se più della metà in warning invia allarme alla caserma
  //      -> se non sono più di metà in warning non fa nulla
  //      -> se non riceve risposta da qualcuno entro 5 secondi lo da per morto e fa i conti in base ai sensori di cui ha risposta
  // -> sensore DISCONNECTED non fa nulla e non risponde a nessuno
  //

  def sensorRead: Double = Random.between(0.0, 10.5)

  def apply(position: (Int, Int),
            zone: String,
            fireStation: Option[ActorRef[FireStationCommand]] = None,
            otherSensor: ListBuffer[ActorRef[SensorCommand]] = ListBuffer.empty): Behavior[SensorCommand] =
    Behaviors.setup (context => {
      context.system.receptionist ! Receptionist.Register(sensorKey, context.self)
      context.spawnAnonymous(manageFireStation(context.self))
      context.spawnAnonymous(manageOtherSensor(context.self))
      Behaviors.withTimers(timer => {
        sensorLogic(position, zone, context, timer, fireStation)
      })
    })

  private def manageFireStation(sendReplyTo: ActorRef[SensorCommand]): Behavior[Receptionist.Listing] =
    Behaviors.setup (context => {
      context.system.receptionist ! Receptionist.Subscribe(FireStationActor.fireStationKey, context.self)
      Behaviors.receiveMessage {
        case msg: Receptionist.Listing =>
          sendReplyTo ! FireStationRegistered(msg.serviceInstances(FireStationActor.fireStationKey).toList)
          Behaviors.same
      }
    })

  private def manageOtherSensor(sendReplyTo: ActorRef[SensorCommand]): Behavior[Receptionist.Listing] =
    Behaviors.setup (context => {
      context.system.receptionist ! Receptionist.Subscribe(SensorActor.sensorKey, context.self)
      Behaviors.receiveMessage {
        case msg: Receptionist.Listing =>
          sendReplyTo ! OtherSensorRegistered(msg.serviceInstances(SensorActor.sensorKey).toList)
          Behaviors.same
      }
    })

  private def sensorLogic(position: (Int, Int),
                  zone: String,
                  ctx: ActorContext[SensorCommand],
                  timer: TimerScheduler[SensorCommand],
                  fireStation: Option[ActorRef[FireStationCommand]] = None,
                  otherSensor: ListBuffer[ActorRef[SensorCommand]] = ListBuffer.empty): Behavior[SensorCommand] =
    implicit val timeout: Timeout = 10.seconds
    var level = 0.0
    var sensorResponse = ListBuffer.empty[SensorState]
    Behaviors.receiveMessage(msg => {
      msg match

        case OtherSensorRegistered(other) =>
          other.filter(!otherSensor.contains(_)).foreach( _ ! MyZoneSensorRequest(ctx.self, zone))
          Behaviors.same

        case MyZoneSensorRequest(sensorToReply, zn) =>
          if (zone == zn) {
            sensorToReply ! MyZoneSensorResponse(ctx.self)
          }
          Behaviors.same

        case MyZoneSensorResponse(sensorRef) =>
          otherSensor += sensorRef
          sensorLogic(position, zone, ctx, timer, fireStation, otherSensor)
          Behaviors.same

        case FireStationRegistered(listings) =>
          if fireStation.isEmpty then listings.foreach(_ ! MyZoneRequest(ctx.self, zone))
          Behaviors.same

        case MyStationResponse(fireStationRef) =>
          sensorLogic(position, zone, ctx, timer, Some(fireStationRef), otherSensor)

        case Update() =>
          level = sensorRead
          level match

            case level if level <= 8 =>
              println("Sensor" + zone + " - OK " + level)
              viewActor.get ! SensorUpdate(position, false)
              timer.startSingleTimer(Update(), 15000.millis)
              Behaviors.same

            case level if level <= 10 =>
              println("Sensor" + zone + " - WARNING(" + level + ")" + ctx.self)
              viewActor.get ! SensorUpdate(position, true)
              timer.startSingleTimer(Update(), 15000.millis)
              otherSensor.foreach( sens =>
                if sens != ctx.self
                then
                  ctx.ask(sens,  _ => IsSensorInAlarmRequest(ctx.self)){
                    case Success(IsSensorInAlarmResponse(sensorState)) => IsSensorInAlarmResponse(sensorState)
                    case _ => IsSensorInAlarmResponse(SensorState.Disconnected)
                  }
              )

              Behaviors.same

            case _ =>
              println("Sensor" + zone + " - DISCONNECTED")
              viewActor.get ! SensorDisconnected(position)
              Thread.sleep(15000)
              ctx.self ! ReconnectToGUI()
              Behaviors.same

        case IsSensorInAlarmRequest(replyTo) =>
          println("I'm sensor: "+ctx.self +" I must reply to: " + replyTo + " and this is my level" + level)
          replyTo ! IsSensorInAlarmResponse(if level <= 8 then SensorState.OK else SensorState.Warning)
          Behaviors.same

        case IsSensorInAlarmResponse(sensorState) =>
          println("I'm sensor: "+ctx.self +" and i have this reply: " + sensorState)
          sensorResponse += sensorState
          if sensorResponse.size == otherSensor.size - 1
          then
            println("Ho tutte le risposte: " + sensorResponse)
            if sensorResponse.count(_ == SensorState.Warning) / sensorResponse.size > 0.5
            then
              fireStation.get ! Alarm(zone)
              viewActor.get ! AlarmView(zone)
            sensorResponse = ListBuffer.empty
          Behaviors.same

        case ReconnectToGUI() =>
          println("Sensor" + zone + " - RECONNECTED")
          viewActor.get ! SensorReconnected(position)
          ctx.self ! Update()
          Behaviors.same

        case GetSensorInfo(context) =>
          viewActor = Some(context)
          context ! SensorInfo(position)
          timer.startSingleTimer(Update(), 15000.millis)
          Behaviors.same
    })