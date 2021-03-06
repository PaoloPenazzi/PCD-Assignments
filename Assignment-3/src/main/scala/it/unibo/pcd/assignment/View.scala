package it.unibo.pcd.assignment

import akka.actor.typed.*
import akka.actor.typed.scaladsl.Behaviors

import java.awt.event.{ActionEvent, WindowAdapter, WindowEvent}
import java.awt.{BorderLayout, Graphics, Graphics2D, RenderingHints}
import javax.swing.{JButton, JFrame, JPanel, SwingUtilities}
import scala.collection.mutable

trait View:
  def display(bodies: mutable.Seq[Body], virtualTime: Double, iteration: Int, bounds: Boundary): Unit

  def updateScale(k: Double): Unit

  def start(): Unit

object View:
  def apply(controller: ViewController): View = new ViewFrame(controller)

  private class ViewFrame(controller: ViewController) extends JFrame, View :
    val simulationPanel: SimulationPanel = SimulationPanel(620, (620 * 0.9).toInt)
    val controlPanel: ControlPanel = ControlPanel(620, (620 * 0.1).toInt, controller)

    override def start(): Unit =
      setSize(620, 620)
      setLayout(new BorderLayout())
      setTitle("Bodies Simulation")
      setResizable(true)
      simulationPanel.setup()
      getContentPane.add(simulationPanel)
      controlPanel.setup()
      getContentPane.add(controlPanel)
      addWindowListener(new WindowAdapter() {
        override def windowClosing(ev: WindowEvent): Unit = System.exit(-1)

        override def windowClosed(ev: WindowEvent): Unit = System.exit(-1)
      })
      setVisible(true)

    override def display(bodies: mutable.Seq[Body], virtualTime: Double, iteration: Int, bounds: Boundary): Unit =
      simulationPanel.bodies = bodies
      simulationPanel.virtualTime = virtualTime
      simulationPanel.iteration = iteration
      simulationPanel.boundary = bounds
      repaint()

    override def updateScale(k: Double): Unit = simulationPanel.updateScale(k)

class SimulationPanel(width: Int, height: Int) extends JPanel :
  val dx: Int = width / 2 - 20
  val dy: Int = height / 2 - 20
  var bodies: mutable.Seq[Body] = mutable.Seq.empty
  var virtualTime: Double = 0
  var iteration: Int = 0
  var boundary: Boundary = Boundary(0, 0, 0, 0)
  var scale: Double = 1

  def setup(): Unit = setSize(width, height)

  def updateScale(k: Double): Unit = scale = scale * k

  override def paint(g: Graphics): Unit =
    if (bodies.nonEmpty)
      val g2: Graphics2D = g.asInstanceOf
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
      g2.clearRect(0, height, width, height)
      val x0 = getXCoordinate(boundary.x0)
      val y0 = getYCoordinate(boundary.y0)
      val wd = getXCoordinate(boundary.x1) - x0
      val ht = y0 - getYCoordinate(boundary.y1)
      g2.drawRect(x0, y0 - ht, wd, ht)
      var radius = (10 * scale).toInt
      if radius < 1 then radius = 1
      bodies.foreach(b => g2.drawOval(getXCoordinate(b.position.x), getYCoordinate(b.position.y), radius, radius))
      val time: String = String.format("%.2f", virtualTime)
      g2.drawString("Bodies: " + bodies.size + " - virtualTime: " + time
        + " - iteration: " + iteration + " (+ for zoom in, - for zoom out)", 2, 45)

  private def getYCoordinate(y: Double): Int = (dy - y * dy * scale).toInt

  private def getXCoordinate(x: Double): Int = (dx + x * dx * scale).toInt

class ControlPanel(width: Int, height: Int, controller: ViewController) extends JPanel :
  var buttonsList: List[JButton] = List.empty

  def setup(): Unit =
    setSize(width, height)
    setFocusable(true)
    setFocusTraversalKeysEnabled(false)
    requestFocusInWindow
    buttonsList = buttonsList :+ new JButton("PLAY")
    buttonsList = buttonsList :+ new JButton("PAUSE")
    buttonsList = buttonsList :+ new JButton("+")
    buttonsList = buttonsList :+ new JButton("-")
    buttonsList.foreach(b => {
      add(b)
      b.addActionListener(controller.actionPerformed(_))
    })

class ViewController(actor: ActorRef[Message]):
  val view: View = View(this)

  def actionPerformed(event: ActionEvent): Unit =
    event.getSource.asInstanceOf[JButton].getText match
      case "PLAY" => actor ! ResumeSimulation()
      case "PAUSE" => actor ! StopSimulation()
      case "+" => view.updateScale(1.1)
      case "-" => view.updateScale(0.9)
      case _ => throw new IllegalStateException()

  def startView(): Unit = view.start()

  def display(bodies: mutable.Seq[Body], virtualTime: Double, iteration: Int, bounds: Boundary): Unit =
    SwingUtilities.invokeLater(() => {
      view.display(bodies, virtualTime, iteration, bounds)
    })

object ViewActor:
  var view: Option[ViewController] = None

  def apply(father: ActorRef[Message]): Behavior[Message] =
    Behaviors.receive { (_, message) =>
      message match
        case UpdateGUI(bodies, virtualTime, iteration, bounds) =>
          view.get.display(bodies, virtualTime, iteration, bounds)
          Behaviors.same
        case StartGUI() =>
          view = Some(new ViewController(father))
          view.get.startView()
          Behaviors.same
        case _ =>
          throw IllegalStateException()
    }