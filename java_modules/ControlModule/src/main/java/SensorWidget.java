import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class SensorWidget extends JPanel
 {
  private JPanel sensorPanel;
  private JPanel C02Panel;
  private JPanel C02ValuePanel;
  private JPanel C02ImagePanel;
  private JLabel C02ValueLabel;
  private JLabel C02ImageLabel;
  private JPanel tempPanel;
  private JPanel tempValuePanel;
  private JLabel tempValueLabel;
  private JPanel tempImagePanel;
  private JLabel tempImageLabel;

  public SensorWidget()
   {
    super();
    /*
    JFrame testFrame = new JFrame();
    testFrame.setTitle("testFrame");
    testFrame.setSize(200,100);
    testFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    testFrame.setVisible(true);

    testFrame.setContentPane(this);
    */

    setSize(200,100);
    setVisible(true);
    ImageIcon imgThisImg = new ImageIcon("../resources/C02Icon.png");
    C02ImageLabel.setIcon(imgThisImg);


    //BufferedImage C02Image = ImageIO.read(new File("../resources/C02Icon.png"));



    //JLabel picLabel = new JLabel(new ImageIcon(myPicture));
    //C02ImagePanel.add(picLabel);

   }
  
  public static void main(String[] args)
   { new SensorWidget(); }
 }
