package distributed

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import distributed.Message
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector, SupervisorStrategy, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import distributed.Message
import concurrent.duration.DurationInt

sealed trait FireStationCommand extends Message
case class Alarm() extends FireStationCommand
case class StartAssistance() extends FireStationCommand
case class EndAssistance() extends FireStationCommand
case class GetInfo(ctx: ActorRef[Message]) extends FireStationCommand
case class sensorInAlarm() extends FireStationCommand

object FireStationActor:
  enum Status:
    case Busy
    case Normal

  var viewActor: Option[ActorRef[Message]] = None
  var status: Status = Status.Normal

  def apply(position: (Int, Int),
            id: String): Behavior[FireStationCommand] = Behaviors.setup(ctx => {
    ctx.system.receptionist ! Receptionist.Register(ServiceKey[FireStationCommand](id), ctx.self)
    Behaviors.withTimers(timers => {
      Behaviors.receiveMessage(msg => {
        msg match
          case GetInfo(ctx) =>
            viewActor = Some(ctx)
            ctx ! StationInfo(position)
            Behaviors.same
          case Alarm() =>
            println(id + ": Alarm Received")
            timers.startSingleTimer(StartAssistance(), 3000.millis)
            Behaviors.same
          case StartAssistance() =>
            println(id + ": Assistance Started")
            status = Status.Busy
            viewActor.get ! StartAssistance()
            busyBehavior
          case _ => throw IllegalStateException()
      })
    })
  })

  def busyBehavior: Behavior[FireStationCommand] = Behaviors.receive((ctx, msg) => {
    msg match
      case EndAssistance() =>
        status = Status.Busy
        // TODO Change behaviour
        Behaviors.same
      case _ => ???
  })

