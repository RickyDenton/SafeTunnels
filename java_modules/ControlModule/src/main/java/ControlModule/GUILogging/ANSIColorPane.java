/* A JTextPane used for printing formatted text in the Control Module GUI Log window */

package ControlModule.GUILogging;

/* ================================== IMPORTS ================================== */

/* -------------------------- Java Standard Libraries -------------------------- */
import javax.swing.*;
import javax.swing.text.*;
import java.awt.Color;

/* ============================== CLASS DEFINITION ============================== */
public class ANSIColorPane extends JTextPane
 {
  /* ========================== GUI LOG WINDOW COLORS ========================== */
  static final Color GUI_COLOR_DBG = new Color(148,148,148);
  static final Color GUI_COLOR_INFO = new Color(51,133,254);
  static final Color GUI_COLOR_WARNING = new Color(242,108,3);
  static final Color GUI_COLOR_ERROR = new Color(203,46,0);
  static final Color GUI_COLOR_FATAL = new Color(241,57,0);
  static final Color cReset = Color.getHSBColor( 0.000f, 0.000f, 1.000f );

  /* ============================== PRIVATE METHODS ============================== */

  /**
   * Appends a string of a given color to the GUI "Log" window
   * @param newStr The string to be appended to the GUI "Log" window
   * @param color  The color of the string to be appended
   */
  private void appendToLog(String newStr, Color color)
   {
    // Initialize an AttributeSet for setting the text's foreground color
    AttributeSet attribSet = StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);

    // Apply the attribute set
    setCharacterAttributes(attribSet, false);

    // Set the caret to the current document length
    setCaretPosition(getDocument().getLength());

    // Append the string at the caret's position
    replaceSelection(newStr);
   }


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * Constructor, disabling the JTextPane editing functionalities
   * (which are temporarily reactivated by the
   *  ANSIColorPaneOutputStream write() method whenever needed)
   */
  public ANSIColorPane()
   { setEditable(false); }


  /**
   * @param ANSIColorID An ANSI color identifier
   * @return The "Color" object associated with the ANSI color identifier
   */
  public Color ANSIColorToColor(String ANSIColorID)
   {
    switch(ANSIColorID)
     {
      case "246":
       return GUI_COLOR_DBG;
      case "33":
       return GUI_COLOR_INFO;
      case "202":
       return GUI_COLOR_WARNING;
      case "160":
       return GUI_COLOR_ERROR;
      case "9":
       return GUI_COLOR_FATAL;
      default:
       return cReset;
     }
   }


  /**
   * Appends an ANSI-formatted colored string to the GUI "Log" window
   * @param ANSIString The ANSI-formatted colored string to be appended
   */
  public void logANSIStr(String ANSIString)
   {
    // The index of the 'm' terminating character of an ANSI escape sequence
    int mIndex;

    // The ANSI color identifier in the ANSI escape sequence
    String ANSIColorID;

    // The Color object associated with the ANSI escape sequence color
    Color ANSIColor;

    // The index of the last character of the string to be appended to
    // the log window (excluding the final 'COLOR_RST' escape sequence)
    int endStringAppendIndex;

    // The string to be appended to the GUI Log window
    String strToLog;

    // Only append non-null strings
    if(ANSIString.length() > 0)
     {
      // If there is no ANSI escape sequence in
      // the passed string, just print it in black
      if(!ANSIString.startsWith("\033[38;5;"))
       appendToLog(ANSIString,Color.BLACK);

       // Otherwise, if there is an ANSI
       // escape sequence in the passed string
      else
       {
        /* ----------------- ANSI Color Retrieval ----------------- */

        // Retrieve the index of the ANSI escape
        // sequence 'm' terminating character
        mIndex = ANSIString.indexOf("m");

        // Extract the color identifier
        // from the ANSI escape sequence
        ANSIColorID = ANSIString.substring(7,mIndex);

        // Instantiate the Color object
        // associated with such ANSI color
        ANSIColor = ANSIColorToColor(ANSIColorID);

        /* ----------------- Log String Retrieval ----------------- */

        // Determine the index of the last character of the string to be appended
        // to the log window (excluding the final 'COLOR_RST' escape sequence)
        endStringAppendIndex = ANSIString.indexOf("[0");

        // If the final 'COLOR_RST' escape sequence
        // was not found, consider the entire string
        if(endStringAppendIndex==-1)
         endStringAppendIndex = ANSIString.length();

        // Extract the string to be appended to the GUI Log window
        strToLog = ANSIString.substring(mIndex+1,endStringAppendIndex-1);

        // Append the string to the GUI Log window
        appendToLog(strToLog,ANSIColor);
       }
     }
   }
 }