package logging;

import devices.DevErrCode;
import errors.*;

import java.util.EnumMap;
import java.util.Map;

// Testing purposes
import static devices.actuator.ActuatorErrCode.ERR_LIGHT_PUT_NO_LIGHTSTATE;
import static devices.sensor.SensorErrCode.ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC;


public abstract class Log
 {
  /* ========================== CONFIGURATION PARAMETERS ========================== */

  // The minimum LOG_LEVEL to be logged
  private static final ErrCodeSeverity LOG_LEVEL = ErrCodeSeverity.DEBUG;

  // Whether FATAL errors should terminate the applications
  private static final boolean EXIT_IF_FATAL = true;

  /* ============================= COLORS MANAGEMENT ============================= */

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

  // Error code severity <-> Logging color mappings
  private static final EnumMap<ErrCodeSeverity,String> ErrCodeSevColorMap = new EnumMap<>(Map.ofEntries
   (
    Map.entry(ErrCodeSeverity.DEBUG,COLOR_DBG),
    Map.entry(ErrCodeSeverity.INFO,COLOR_INFO),
    Map.entry(ErrCodeSeverity.WARNING,COLOR_WARNING),
    Map.entry(ErrCodeSeverity.ERROR,COLOR_ERROR),
    Map.entry(ErrCodeSeverity.FATAL,COLOR_FATAL)
   ));

  /* ============================= LOGGING FUNCTIONS ============================= */

  /* ----------------------------- Utility Functions ----------------------------- */

  // Prints the head of a ModuleErrCode (e.g. [INFO]: ... )
  private static String printLogHeadModule(ErrCodeSeverity sev)
   { return "[" + sev + "] "; }

  // Prints the head of a DevErrCode (e.g. [sensor1 - ERR]: ... )
  private static String printLogHeadDev(String devType, int devID, ErrCodeSeverity sev)
   { return "[" + devType + devID + " - " + sev + "]: "; }

  // Terminates the program is the error code is of fatal severity

  // Terminates the program if EXIT_IF_FATAL ==
  // true and the error code is of fatal severity
  private static void checkFatalExit(ErrCodeSeverity sevLev)
   {
    if(sevLev==ErrCodeSeverity.FATAL && EXIT_IF_FATAL)
     {
      System.out.println(COLOR_FATAL + "The application will now exit" + COLOR_RST);
      System.exit(1);
     }
   }

  /* ------------------------- Plain Logging (noErrCode) ------------------------- */

  public static void dbg(String logStr)
   {
    if(ErrCodeSeverity.DEBUG.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(COLOR_DBG + "[DBG]: " + logStr + COLOR_RST); }

  public static void info(String logStr)
   {
    if(ErrCodeSeverity.INFO.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(COLOR_INFO + "[INFO]: " + logStr + COLOR_RST);
   }

  public static void warn(String logStr)
   {
    if(ErrCodeSeverity.WARNING.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(COLOR_WARNING + "[WARN]: " + logStr + COLOR_RST);
   }

  public static void err(String logStr)
   {
    if(ErrCodeSeverity.ERROR.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(COLOR_ERROR + "[ERR]: " + logStr + COLOR_RST);
   }

  public static void fatal(String logStr)
   {
    System.out.println(COLOR_FATAL + "[FATAL]: " + logStr);

    if(EXIT_IF_FATAL)
     {
      System.out.println(COLOR_FATAL + "The application will now exit" + COLOR_RST);
      System.exit(1);
     }
   }

  /* ------------------------ Modules Error Codes Logging ------------------------ */

  public static void code(ModuleErrCode modErrCode)
   { code(modErrCode,""); }

  public static void code(ModuleErrCode modErrCode, String addDscr)
   {
    ErrCodeInfo errCodeInfo = modErrCode.getErrCodeInfo();

    if(errCodeInfo.sevLev.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(ErrCodeSevColorMap.get(errCodeInfo.sevLev) + printLogHeadModule(errCodeInfo.sevLev) + errCodeInfo.humanDscr + " " + addDscr + COLOR_RST);

    checkFatalExit(errCodeInfo.sevLev);
   }

  /* ------------------------ Devices Error Codes Logging ------------------------ */

  public static void code(DevErrCode devErrCode, int devID)
   { code(devErrCode,devID,""); }

  public static void code(DevErrCode devErrCode, int devID, String addDscr)
   {
    ErrCodeInfo errCodeInfo = devErrCode.getErrCodeInfo();

    if(errCodeInfo.sevLev.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(ErrCodeSevColorMap.get(errCodeInfo.sevLev) + printLogHeadDev(devErrCode.getDevType(),devID,errCodeInfo.sevLev) + errCodeInfo.humanDscr + " " + addDscr + COLOR_RST);

    checkFatalExit(errCodeInfo.sevLev);
   }

  /* ---------------------- Error Codes Exceptions Logging ---------------------- */

  // Calls the appropriate function depending on whether a
  // DevErrCodeExcp or a more general ErrCodeExcp was passed
  public static void excp(ErrCodeExcp errCodeExcp)
   {
    if(errCodeExcp instanceof DevErrCodeExcp)
     excpDev((DevErrCodeExcp)errCodeExcp);
    else
     excpModule(errCodeExcp);
   }

  // Module Error Codes Exceptions logging
  public static void excpModule(ErrCodeExcp errCodeExcp)
   {
    ErrCodeInfo errCodeInfo = errCodeExcp.errCode.getErrCodeInfo();

    if(errCodeInfo.sevLev.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(ErrCodeSevColorMap.get(errCodeInfo.sevLev) + printLogHeadModule(errCodeInfo.sevLev) + errCodeInfo.humanDscr + " " + errCodeExcp.addDscr + COLOR_RST);

    checkFatalExit(errCodeInfo.sevLev);
   }

  // Device Error Codes Exceptions logging
  public static void excpDev(DevErrCodeExcp devErrCodeExcp)
   {
    ErrCodeInfo errCodeInfo = devErrCodeExcp.errCode.getErrCodeInfo();

    if(errCodeInfo.sevLev.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(ErrCodeSevColorMap.get(errCodeInfo.sevLev) + printLogHeadDev(((DevErrCode)devErrCodeExcp.errCode).getDevType(),devErrCodeExcp.devID,errCodeInfo.sevLev) + errCodeInfo.humanDscr + " " + devErrCodeExcp.addDscr + COLOR_RST);

    checkFatalExit(errCodeInfo.sevLev);
   }

  /* ============================== LOGGING TESTING ============================== */

  public static void main(String[] args)
   {
    System.out.println("LOGGING TESTING");
    System.out.println("===============");

    Log.dbg("This is a debug message");
    Log.info("This is a info message");
    Log.warn("This is a warning message");
    Log.err("This is a error message");
    Log.fatal("This is a fatal message");
    Log.dbg("This should not be printed with EXIT_IF_FATAL == true");

    Log.code(ERR_LIGHT_PUT_NO_LIGHTSTATE,1);
    Log.code(ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,2,"(<additional description>)");

    try
     { throw new ErrCodeExcp(ERR_LIGHT_PUT_NO_LIGHTSTATE); }
    catch(ErrCodeExcp excp)
     { Log.excp(excp); }

    try
     { throw new ErrCodeExcp(ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,"(<additional description>)"); }
    catch(ErrCodeExcp excp)
     { Log.excp(excp); }

    try
     { throw new DevErrCodeExcp(ERR_LIGHT_PUT_NO_LIGHTSTATE,1); }
    catch(ErrCodeExcp excp)
     { Log.excp(excp); }

    try
     { throw new DevErrCodeExcp(ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,2,"(<additional description>)"); }
    catch(ErrCodeExcp excp)
     { Log.excp(excp); }
   }
 }