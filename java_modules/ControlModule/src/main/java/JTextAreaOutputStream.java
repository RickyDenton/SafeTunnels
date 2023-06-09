import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.io.IOException;
import java.io.OutputStream;


public class JTextAreaOutputStream extends OutputStream
 {
  private final ANSIColorPane destination;

  public JTextAreaOutputStream(ANSIColorPane destination)
   {
    if(destination==null)
     throw new IllegalArgumentException("Destination is null");

    this.destination = destination;
   }

  @Override
  public void write(byte[] buffer,int offset,int length) throws IOException
   {
    final String text = new String(buffer,offset,length);
    SwingUtilities.invokeLater(new Runnable()
     {
      @Override
      public void run()
       {
        destination.appendANSI(text);
       }
        /*
        try
         {
          //destination.getStyledDocument().insertString(destination.getStyledDocument().getLength(),text,null);
          destination.appendANSI(text);
         }
        catch(BadLocationException badLoc)
         { System.out.println("shit"); }}

         */
     });
   }

  @Override
  public void write(int b) throws IOException
   { write(new byte[]{(byte)b},0,1); }
 }