import javax.swing.*;


public class ControlModule extends JFrame
 {
  private JPanel mainPanel;
  private JCheckBox automaticModeCheckBox;
  private JPanel systemPanel;
  private JLabel stateLabel;
  private JPanel logPanel;
  private JLabel LogLabel;
  private JLabel sensorsLabel;
  private JPanel devicesLabels;
  private JLabel actuatorsLabel;
  private JPanel devicesPanel;
  private JPanel devicesListsPanel;
  private JPanel actuatorsListPanel;
  private JPanel sensorsListPanel;
  private JTextArea LogArea;
  private JPanel actuatorsLabelPanel;
  private JPanel sensorsLabelPanel;
  private JPanel logLabelPanel;
  private JPanel sensor2Panel;
  private JPanel sensor2HeaderPanel;
  private JLabel sensor2Name;
  private JLabel sensor2ConnStateImg;
  private JPanel sensor2ImagesPanel;
  private JPanel sensor2TempImagePanel;
  private JPanel sensor2C02ImagePanel;
  private JLabel sensor2TempImageLabel;
  private JLabel sensor2C02ImageLabel;
  private JPanel sensor2C02ValuePanel;
  private JLabel sensor2C02ValueLabel;
  private JPanel sensor2ValuesPanel;
  private JPanel sensor2TempValuePanel;
  private JLabel sensor2TempValueLabel;
  private JPanel sensor1Panel;
  private JPanel sensor1HeaderPanel;
  private JLabel sensor1Name;
  private JLabel sensor1ConnStateImg;
  private JPanel sensor1ImagesPanel;
  private JPanel sensor1TempImagePanel;
  private JPanel sensor1C02ImagePanel;
  private JLabel sensor1TempImageLabel;
  private JLabel sensor1TempC02Label;
  private JPanel sensor1ValuesPanel;
  private JPanel sensor1C02ValuePanel;
  private JLabel sensor1C02ValueLabel;
  private JPanel sensor1TempValuePanel;
  private JLabel sensor1TempValueLabel;
  private JPanel actuator2Panel;
  private JPanel actuator2HeaderPanel;
  private JLabel actuator2Name;
  private JLabel actuator2ConnStateImg;
  private JPanel actuator2ImagesPanel;
  private JPanel actuator2CommandsPanel;
  private JPanel actuator2FanImagePanel;
  private JLabel Actuator2FanImageLabel;
  private JPanel actuator2LightImagePanel;
  private JLabel actuator2LightImageLabel;
  private JPanel actuator2ValuesPanel;
  private JPanel actuator2FanRelSpeedValuePanel;
  private JPanel actuator2LightStateValuePanel;
  private JLabel actuator2FanRelSpeedValueLabel;
  private JLabel actuator2LightStateValueLabel;
  private JPanel actuator2FanCommandPanel;
  private JPanel actuator2LightCommandPanel;
  private JSlider actuator2FanCommandSlider;
  private JButton actuator2LightCommandButtonOFF;
  private JButton actuator2LightCommandButtonALERT;
  private JButton actuator2LightCommandButtonEMERGENCY;
  private JButton actuator2LightCommandButtonON;
  private JPanel actuator1Panel;
  private JPanel actuator1HeaderPanel;
  private JLabel actuator1Name;
  private JLabel actuator1ConnStateImg;
  private JPanel actuator1ImagesPanel;
  private JPanel actuator1FanImagePanel;
  private JLabel Actuator1FanImageLabel;
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

  public ControlModule()
   {
    setTitle("SafeTunnels Control Module");
    setSize(800,600);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setVisible(true);
    
    mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    systemPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
    devicesPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

    /*
    devicesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    logPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    */

    setContentPane(mainPanel);

    
   }

  public static void main(String[] args)
   { new ControlModule(); }
 }
