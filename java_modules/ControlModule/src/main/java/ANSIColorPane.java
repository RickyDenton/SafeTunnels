import javax.swing.*;
import javax.swing.text.*;
import java.awt.Color;

public class ANSIColorPane extends JTextPane
 {
  static final Color GUI_COLOR_DBG = new Color(148,148,148);
  static final Color GUI_COLOR_INFO = new Color(51,133,254);
  static final Color GUI_COLOR_WARNING = new Color(242,108,3);
  static final Color GUI_COLOR_ERROR = new Color(203,46,0);
  static final Color GUI_COLOR_FATAL = new Color(241,57,0);
  static final Color cBlack    = new Color(0,0,0);
  static final Color cReset    = Color.getHSBColor( 0.000f, 0.000f, 1.000f );

 public ANSIColorPane()
  {}

 public void append(Color c, String s) {
  StyleContext sc = StyleContext.getDefaultStyleContext();
  AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
  int len = getDocument().getLength(); // same value as getText().length();
  setCaretPosition(len);  // place caret at the end (with no selection)
  setCharacterAttributes(aset, false);
  replaceSelection(s); // there is no selection, so inserts at caret
 }

 public void appendANSI(String ANSIString)
  {
   int mIndex = 0;
   int endStringIndex = 0;
   String colorStr;
   Color color;
   String strToPrint;

   if(ANSIString.length()>0)
    {
     // Not an escape string
     if(!ANSIString.startsWith("\033[38;5;"))
      {
       append(cBlack,ANSIString);
       return;
      }

     // It is an escape string
     else
      {

       // Get Color
       // Get "m" index
       mIndex = ANSIString.indexOf("m");
       colorStr = ANSIString.substring(7,mIndex);
       color = getANSIColor(colorStr);

       // Get End
       endStringIndex = ANSIString.indexOf("[0");
       if(endStringIndex==-1)
        endStringIndex = ANSIString.length();

       // Get string to print
       strToPrint = ANSIString.substring(mIndex+1,endStringIndex-1);

       append(color,strToPrint);
      }
    }
  }

 public Color getANSIColor(String ANSIColor)
  {
  switch(ANSIColor)
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
}