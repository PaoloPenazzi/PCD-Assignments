package distributed

import java.awt.event.{WindowAdapter, WindowEvent}
import java.awt.{BorderLayout, Graphics, Graphics2D, RenderingHints}
import javax.swing.{JFrame, JPanel}

trait View:
  def display(city: CityGrid): Unit
  def start(): Unit

object View:
  def apply(width: Int, height: Int): View = new ViewFrame(width, height)

  private class ViewFrame(width: Int, height: Int) extends JFrame, View :
    val cityPanel = new CityPanel(width, height)

    override def start(): Unit =
      setSize(width, height)
      setLayout(new BorderLayout())
      setTitle("Smart City Simulation")
      setLocationRelativeTo(null)
      cityPanel.setup()
      add(cityPanel)
      addWindowListener(new WindowAdapter() {
        override def windowClosing(ev: WindowEvent): Unit = System.exit(-1)
        override def windowClosed(ev: WindowEvent): Unit = System.exit(-1)
      })
      setVisible(true)

    override def display(city: CityGrid): Unit =
      cityPanel.city = Option(city)
      repaint()

class CityPanel(width: Int, height: Int) extends JPanel:
  var iteration: Int = 0
  var city: Option[CityGrid] = None
  val dx: Int = width / 2 - 20
  val dy: Int = height / 2 - 20
  var scale: Double = 1

  //private def getYCoordinate(y: Double): Int = (dy - y * dy * scale).toInt
  //private def getXCoordinate(x: Double): Int = (dx + x * dx * scale).toInt
  override def paint(g: Graphics): Unit =
    if city.isDefined
    then
      val g2: Graphics2D = g.asInstanceOf
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
      g2.clearRect(0, 0, 2 * width, 2 * height)
      g2.drawRect(50, 50, city.get.bounds.x1.toInt, city.get.bounds.y1.toInt)
      for x <- city.get.zones
        do g2.drawRect(x.bounds.x0.toInt + 50,
          x.bounds.y0.toInt + 50,
          x.bounds.x1.toInt - x.bounds.x0.toInt,
          x.bounds.y1.toInt - x.bounds.y0.toInt)
      for x <- city.get.sensors
        do g2.drawOval(x.x + 50, x.y + 50, 5, 5)


  def setup(): Unit =
    setSize(width + 100, height + 100)
    setVisible(true)

