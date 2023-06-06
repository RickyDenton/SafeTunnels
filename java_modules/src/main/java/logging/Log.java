package logging;

import devices.DevErrCode;
import errors.DevErrCodeExcp;
import errors.ErrCodeExcp;
import errors.ErrCodeInfo;
import errors.ErrCodeSeverity;

import java.util.EnumMap;
import java.util.Map;


public abstract class Log
 {
  // Reset color
  private final static String COLOR_RST   = "\u001B[0m";

  /*
  // Error Code Severity Colors (4 bit)
  private final static String COLOR_DBG = "\u001B[34m";
  private final static String COLOR_INFO = "\u001B[32m";
  private final static String COLOR_WARNING = "\u001B[33m";
  private final static String COLOR_ERROR = "\u001B[31m";
  private final static String COLOR_FATAL = "\u001B[91m";
  */

  // Error Code Severity Colors (8 bit)
  private final static String COLOR_DBG = "\033[38;5;246m";
  private final static String COLOR_INFO = "\033[38;5;33m";
  private final static String COLOR_WARNING = "\033[38;5;202m";
  private final static String COLOR_ERROR = "\033[38;5;160m";
  private final static String COLOR_FATAL = "\033[38;5;9m";

  private static final EnumMap<ErrCodeSeverity,String> ErrCodeSevColorMap = new EnumMap<>(Map.ofEntries
   (
    Map.entry(ErrCodeSeverity.DEBUG,COLOR_DBG),
    Map.entry(ErrCodeSeverity.INFO,COLOR_INFO),
    Map.entry(ErrCodeSeverity.WARNING,COLOR_WARNING),
    Map.entry(ErrCodeSeverity.ERROR,COLOR_ERROR),
    Map.entry(ErrCodeSeverity.FATAL,COLOR_FATAL)
   ));


  // Utilities
  private static String printHeadSev(ErrCodeSeverity sev)
   { return "[" + sev + "] "; }

  // Utilities
  private static String printHeadDevSev(String devType, int devID, ErrCodeSeverity sev)
   { return "[" + devType + devID + " - " + sev + "]: "; }

  // No code
  public static void dbg(String logStr)
   { System.out.println(COLOR_DBG + "[DBG]: " + logStr + COLOR_RST); }

  public static void info(String logStr)
   { System.out.println(COLOR_INFO + "[INFO]: " + logStr + COLOR_RST); }

  public static void warn(String logStr)
   { System.out.println(COLOR_WARNING + "[WARN]: " + logStr + COLOR_RST); }

  public static void err(String logStr)
   { System.out.println(COLOR_ERROR + "[ERR]: " + logStr + COLOR_RST); }

  public static void fatal(String logStr)
   {
    System.out.println(COLOR_FATAL + "[FATAL]: " + logStr);
    System.out.println("The application will now exit" + COLOR_RST);
    System.exit(1);
   }

  // Device error codes
  public static void code(DevErrCode devErrCode, int devID)
   { code(devErrCode,devID,""); }

  public static void code(DevErrCode devErrCode, int devID, String addDscr)
   {
    ErrCodeInfo errCodeInfo = devErrCode.getErrCodeInfo();
    System.out.println(ErrCodeSevColorMap.get(errCodeInfo.sevLev) + printHeadDevSev(devErrCode.getDevType(),devID,errCodeInfo.sevLev) + errCodeInfo.humanDscr + " " + addDscr + COLOR_RST);

    if(errCodeInfo.sevLev == ErrCodeSeverity.FATAL)
     System.out.println(COLOR_FATAL + "The application will now exit" + COLOR_RST);
   }

  // Exceptions
  public static void excp(ErrCodeExcp errCodeExcp)
   {
    ErrCodeInfo errCodeInfo = errCodeExcp.errCode.getErrCodeInfo();
    System.out.println(ErrCodeSevColorMap.get(errCodeInfo.sevLev) + printHeadSev(errCodeInfo.sevLev) + errCodeInfo.humanDscr + " " + errCodeExcp.addDscr + COLOR_RST);

    if(errCodeInfo.sevLev == ErrCodeSeverity.FATAL)
     System.out.println(COLOR_FATAL + "The application will now exit" + COLOR_RST);
   }

  public static void excp(DevErrCodeExcp devErrCodeExcp)
   {
    ErrCodeInfo errCodeInfo = devErrCodeExcp.errCode.getErrCodeInfo();
    System.out.println(ErrCodeSevColorMap.get(errCodeInfo.sevLev) + printHeadDevSev(((DevErrCode)devErrCodeExcp.errCode).getDevType(),devErrCodeExcp.devID,errCodeInfo.sevLev) + errCodeInfo.humanDscr + " " + devErrCodeExcp.addDscr + COLOR_RST);

    if(errCodeInfo.sevLev == ErrCodeSeverity.FATAL)
     System.out.println(COLOR_FATAL + "The application will now exit" + COLOR_RST);
   }
 }