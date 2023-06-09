import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class ControlModule extends JFrame
 {
  private JPanel mainPanel;
  private JCheckBox automaticModeCheckBox;
  private JPanel systemPanel;
  private JLabel stateLabel;
  private JPanel logPanel;
  private JLabel LogLabel;
  private JPanel devicesPanel;
  private JTextArea LogArea;
  private JPanel logLabelPanel;
  private JPanel devicesListsPanel;
  private JPanel actuatorsListPanel;
  private JPanel actuator2Panel;
  private JPanel actuator2HeaderPanel;
  private JLabel actuator2Name;
  private JLabel actuator2ConnStateImg;
  private JPanel actuator2ImagesPanel;
  private JPanel actuator2FanImagePanel;
  private JLabel Actuator2FanImageLabel;
  private JPanel actuator2LightImagePanel;
  private JLabel actuator2LightImageLabel;
  private JPanel actuator2CommandsPanel;
  private JPanel actuator2FanCommandPanel;
  private JSlider actuator2FanCommandSlider;
  private JPanel actuator2LightCommandPanel;
  private JButton actuator2LightCommandButtonOFF;
  private JButton actuator2LightCommandButtonALERT;
  private JButton actuator2LightCommandButtonEMERGENCY;
  private JButton actuator2LightCommandButtonON;
  private JPanel actuator2ValuesPanel;
  private JPanel actuator2FanRelSpeedValuePanel;
  private JLabel actuator2FanRelSpeedValueLabel;
  private JPanel actuator2LightStateValuePanel;
  private JLabel actuator2LightStateValueLabel;
  private JPanel actuator1Panel;
  private JPanel actuator1HeaderPanel;
  private JLabel actuator1Name;
  private JLabel actuator1ConnStateImg;
  private JPanel actuator1ImagesPanel;
  private JPanel actuator1LightImagePanel;
  private JLabel actuator1LightImageLabel;
  private JPanel actuator1CommandsPanel;
  private JPanel actuator1FanCommandPanel;
  private JSlider actuator1FanCommandSlider;
  private JPanel actuator1LightCommandPanel;
  private JButton actuator1LightCommandButtonOFF;
  private JButton actuator1LightCommandButtonALERT;
  private JButton actuator1LightCommandButtonEMERGENCY;
  private JButton actuator1LightCommandButtonON;
  private JPanel actuator1ValuesPanel;
  private JPanel actuator1FanRelSpeedValuePanel;
  private JLabel actuator1FanRelSpeedValueLabel;
  private JPanel actuator1LightStateValuePanel;
  private JLabel actuator1LightStateValueLabel;
  private JPanel sensorsListPanel;

  public ControlModule()
   {
    setTitle("SafeTunnels Control Module");
    setSize(800,600);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setVisible(true);

    setContentPane(mainPanel);




/*

      sensor2C02ImageLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/C02Icon.png")));
      sensor2C02ImagePanel.add(sensor2C02ImageLabel);
*/

   }

  public static void main(String[] args)
   { new ControlModule(); }
 }
