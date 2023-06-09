/* An output stream used to redirect stdout to the GUI Log window */

package GUILogging;

/* ================================== IMPORTS ================================== */

/* -------------------------- Java Standard Libraries -------------------------- */
import javax.swing.*;
import java.io.OutputStream;


/* ============================== CLASS DEFINITION ============================== */
public class ANSIColorPaneOutputStream extends OutputStream
 {
  // The GUI Log window component
  private final ANSIColorPane logWindow;

  /* ============================= PUBLIC METHODS ============================= */

  /**
   * Constructor, initializing the output stream and the 'logWindow' attribute
   */
  public ANSIColorPaneOutputStream(ANSIColorPane logWindow)
   { this.logWindow = logWindow; }


  /**
   * Writes an array of bytes of a given length
   * at a given offset in the GUI log window
   * @param buffer The array of bytes to be written
   * @param offset The offset the array must be written to
   * @param length The number of bytes to be written
   */
  @Override
  public void write(byte[] buffer,int offset,int length)
   {
    final String strToWrite = new String(buffer,offset,length);
    SwingUtilities.invokeLater(() -> logWindow.logANSIStr(strToWrite));
   }


  /**
   * Write a character in the GUI log window
   * @param charRes the character representation as an integer
   */
  @Override
  public void write(int charRes)
   { write(new byte[]{(byte)charRes},0,1); }
 }