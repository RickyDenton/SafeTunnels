/* An output stream used to redirect stdout to the GUI Log window */

package ControlModule.GUILogging;

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
   * in the GUI log window at a given offset
   * @param buffer The array of bytes to be written
   * @param offset The offset the array must be written to
   * @param length The number of bytes to be written
   */
  @Override
  public void write(byte[] buffer,int offset,int length)
   {
    // Convert the array of bytes to be written in the GUI window to a string
    final String strToWrite = new String(buffer,offset,length);

    // Set the operations to be performed by the GUI as soon as it is ready
    SwingUtilities.invokeLater(() ->
       {
        // Enable the GUI Log window editing
        logWindow.setEditable(true);

        // Write the String to the GUI Log window
        logWindow.logANSIStr(strToWrite);

        // Disable the GUI Log window editing
        logWindow.setEditable(false);
       });
   }


  /**
   * Write a character in the GUI log window
   * @param charRes the character representation as an integer
   */
  @Override
  public void write(int charRes)
   { write(new byte[]{(byte)charRes},0,1); }
 }