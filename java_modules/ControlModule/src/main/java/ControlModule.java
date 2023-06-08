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

  public ControlModule()
   {
    setTitle("SafeTunnels Control Module");
    setSize(800,600);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setVisible(true);
    
    //mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    //systemPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
    //devicesPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

    //sensorsLabelPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
    //actuatorsLabelPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

    /*
    devicesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    logPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    */
    setContentPane(mainPanel);



    SensorWidget s = new SensorWidget(); // 1st form
    sensorsListPanel.add(s);


   }

  public static void main(String[] args)
   { new ControlModule(); }
 }
