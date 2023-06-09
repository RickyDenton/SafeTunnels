import javax.swing.*;


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
  private JPanel actuator1Panel;
  private JPanel actuator1HeaderPanel;
  private JLabel actuator1Name;
  private JLabel actuator1ConnStateImg;
  private JPanel sensorsListPanel;
  private JSlider slider1;
  private JButton OFFButton;
  private JButton ONButton;
  private JButton ALERTButton;
  private JButton EMERButton;

  public ControlModule()
   {
    setTitle("SafeTunnels Control Module");
    setSize(425,600);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setVisible(true);
    setResizable(false);
    setContentPane(mainPanel);




/*

      sensor2C02ImageLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/C02Icon.png")));
      sensor2C02ImagePanel.add(sensor2C02ImageLabel);
*/

   }

  public static void main(String[] args)
   { new ControlModule(); }
 }
