/* SafeTunnels Log Module used both for the CLI and the GUI */

package logging;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/* --------------------------- SafeTunnels Resources --------------------------- */
import devices.DevErrCode;
import devices.BaseDevice.DevType;
import errors.*;

// Unit testing purposes
import static devices.actuator.BaseActuatorErrCode.ERR_LIGHT_PUT_NO_LIGHTSTATE;
import static devices.sensor.BaseSensorErrCode.ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC;


/* ============================== CLASS DEFINITION ============================== */
public abstract class Log
 {
  /* ================================ ATTRIBUTES ================================ */

  /* --------------------- Logging Configuration Parameters --------------------- */

  // The LOG_LEVEL, or minimum ErrCodeSeverity to be logged
  public static ErrCodeSeverity LOG_LEVEL = ErrCodeSeverity.DEBUG;

  // Whether FATAL errors should terminate the application
  public static final boolean EXIT_IF_FATAL = true;

  /* ------------------------ Logging Colors Definitions ------------------------ */

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

  /* ============================== PRIVATE METHODS ============================== */

  /**
   * Returns a ModuleErrCode's header (e.g. [INFO]:)
   * @param sev The ModuleErrCode's severity level
   * @return The ModuleErrCode's header
   */
  private static String printLogHeaderModule(ErrCodeSeverity sev)
   { return "[" + sev + "] "; }

  /**
   * Returns a DevErrCode's header (e.g. [sensor1 - ERR]: ... )
   * @param sev The DevErrCode's severity level
   * @return The DevErrCode's header
   */
  private static String printLogHeaderDev(DevType devType, int devID, ErrCodeSeverity sev)
   { return "[" + devType + devID + " - " + sev + "]: "; }


  /**
   * Terminates the program if a fatal error
   * has occurred and EXIT_IF_FATAL == true
   * @param sevLev The severity level of an error that has occurred
   */
  private static void checkFatalExit(ErrCodeSeverity sevLev)
   {
    // If a fatal error has occurred and the application should terminate
    if(sevLev == ErrCodeSeverity.FATAL && EXIT_IF_FATAL)
     {
      // Print that the application will now terminate
      System.out.println(COLOR_FATAL + "The application will now exit" + COLOR_RST);

      // Wait a delay before terminating the application (this is to allow
      // the error message to be displayed in the ControlModule's GUI)
      try
       { TimeUnit.SECONDS.sleep(5); }
      catch(InterruptedException excp)
       { System.exit(1); }

      // Terminate the application
      System.exit(1);
     }
   }


  /* ============================== PUBLIC METHODS ============================== */

  /* ----------------- Plain Severity-based Logging (noErrCode) ----------------- */

  /**
   * DEBUG plain logging
   * @param logStr The String to be logged
   */
  public static void dbg(String logStr)
   {
    // Log only if LOG_LEVEL == DEBUG
    if(ErrCodeSeverity.DEBUG.ordinal() == LOG_LEVEL.ordinal())
     System.out.println(COLOR_DBG + "[DBG]: " + logStr + COLOR_RST);
   }

  /**
   * INFO plain logging
   * @param logStr The String to be logged
   */
  public static void info(String logStr)
   {
    // Log only if LOG_LEVEL >= INFO
    if(ErrCodeSeverity.INFO.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(COLOR_INFO + "[INFO]: " + logStr + COLOR_RST);
   }

  /**
   * WARNING plain logging
   * @param logStr The String to be logged
   */
  public static void warn(String logStr)
   {
    // Log only if LOG_LEVEL >= WARNING
    if(ErrCodeSeverity.WARNING.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(COLOR_WARNING + "[WARN]: " + logStr + COLOR_RST);
   }

  /**
   * ERROR plain logging
   * @param logStr The String to be logged
   */
  public static void err(String logStr)
   {
    // Log only if LOG_LEVEL >= ERROR
    if(ErrCodeSeverity.ERROR.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(COLOR_ERROR + "[ERR]: " + logStr + COLOR_RST);
   }

  /**
   * FATAL plain logging
   * @param logStr The String to be logged
   */
  public static void fatal(String logStr)
   {
    // Log the fatal error
    System.out.println(COLOR_FATAL + "[FATAL]: " + logStr + COLOR_RST);

    // Terminate the program if EXIT_IF_FATAL == true
    checkFatalExit(ErrCodeSeverity.FATAL);
   }


  /* -------------------------- ModuleErrCode Logging -------------------------- */

  /**
   * No additional description ModuleErrCode logging, calling the
   * additional description ModuleErrCode passing an empty string
   * @param modErrCode The ModuleErrCode that has occurred
   */
  public static void code(ModuleErrCode modErrCode)
   { code(modErrCode,""); }

  /**
   * ModuleErrCode logging
   * @param modErrCode The ModuleErrCode that has occurred
   * @param addDscr    An additional description
   *                   associated with the ModuleErrCode
   */
  public static void code(ModuleErrCode modErrCode, String addDscr)
   {
    // Retrieve the errCodeInfo associated with the ModuleErrCode
    ErrCodeInfo errCodeInfo = modErrCode.getErrCodeInfo();

    // If LOG_LEVEL >= the errCodeInfo severity, log the ModuleErrCode
    if(errCodeInfo.sevLev.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(ErrCodeSevColorMap.get(errCodeInfo.sevLev)
                        + printLogHeaderModule(errCodeInfo.sevLev)
                        + errCodeInfo.humanDscr + " " + addDscr + COLOR_RST);

    // In case of a fatal error, terminate the program if EXIT_IF_FATAL == true
    checkFatalExit(errCodeInfo.sevLev);
   }


  /* ---------------------------- DevErrCode Logging ---------------------------- */

  /**
   * No additional description DevErrCode logging, calling the
   * additional description DevErrCode passing an empty string
   * @param devErrCode The DevErrCode that has occurred
   * @param devID      The ID of the device associated with the error
   */
  public static void code(DevErrCode devErrCode, int devID)
   { code(devErrCode,devID,""); }

  /**
   * DevErrCode logging
   * @param devErrCode The DevErrCode that has occurred
   * @param devID      The ID of the device associated with the error
   * @param addDscr    An additional description
   *                   associated with the DevErrCode
   */
  public static void code(DevErrCode devErrCode, int devID, String addDscr)
   {
    // Retrieve the errCodeInfo associated with the DevErrCode
    ErrCodeInfo errCodeInfo = devErrCode.getErrCodeInfo();

    // If LOG_LEVEL >= the errCodeInfo severity, log the DevErrCode
    if(errCodeInfo.sevLev.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(ErrCodeSevColorMap.get(errCodeInfo.sevLev)
                        + printLogHeaderDev(devErrCode.getDevType(),devID,errCodeInfo.sevLev)
                        + errCodeInfo.humanDscr + " " + addDscr + COLOR_RST);

    // In case of a fatal error, terminate the program if EXIT_IF_FATAL == true
    checkFatalExit(errCodeInfo.sevLev);
   }


  /* ---------------------- ErrCodeExcp Exception Logging ---------------------- */

  /**
   * Calls the excpModule() or excpDev()
   * method depending on the errCodeExcp type
   * @param errCodeExcp The errCodeExcp that was caught
   */
  public static void excp(ErrCodeExcp errCodeExcp)
   {
    if(errCodeExcp instanceof DevErrCodeExcp)
     excpDev((DevErrCodeExcp)errCodeExcp);
    else
     excpModule(errCodeExcp);
   }

  /**
   * Module errCodeExcp Logging
   * @param errCodeExcp The errCodeExcp that was caught
   */
  public static void excpModule(ErrCodeExcp errCodeExcp)
   {
    // Retrieve the errCodeInfo associated with the exception's ModuleErrCode
    ErrCodeInfo errCodeInfo = errCodeExcp.errCode.getErrCodeInfo();

    // If LOG_LEVEL >= the errCodeInfo severity, log the ModuleErrCode
    if(errCodeInfo.sevLev.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(ErrCodeSevColorMap.get(errCodeInfo.sevLev)
                        + printLogHeaderModule(errCodeInfo.sevLev) + errCodeInfo.humanDscr
                        + " " + errCodeExcp.addDscr + COLOR_RST);

    // In case of a fatal error, terminate the program if EXIT_IF_FATAL == true
    checkFatalExit(errCodeInfo.sevLev);
   }

  /**
   * Device errCodeExcp Logging
   * @param devErrCodeExcp The DevErrCodeExcp that was caught
   */
  public static void excpDev(DevErrCodeExcp devErrCodeExcp)
   {
    // Retrieve the errCodeInfo associated with the exception's DevErrCode
    ErrCodeInfo errCodeInfo = devErrCodeExcp.errCode.getErrCodeInfo();

    // If LOG_LEVEL >= the errCodeInfo severity, log the DevErrCode
    if(errCodeInfo.sevLev.ordinal() >= LOG_LEVEL.ordinal())
     System.out.println(ErrCodeSevColorMap.get(errCodeInfo.sevLev)
                        + printLogHeaderDev(((DevErrCode)devErrCodeExcp.errCode).getDevType(),devErrCodeExcp.devID,errCodeInfo.sevLev)
                        + errCodeInfo.humanDscr + " " + devErrCodeExcp.addDscr + COLOR_RST);

    // In case of a fatal error, terminate the program if EXIT_IF_FATAL == true
    checkFatalExit(errCodeInfo.sevLev);
   }


  /* =========================== LOGGING UNIT TESTING =========================== */

  /**
   * Logging unit testing entry point
   */
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

    code(ERR_LIGHT_PUT_NO_LIGHTSTATE,1);
    Log.code(ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,2,
      "(<additional description>)");

    try
     { throw new ErrCodeExcp(ERR_LIGHT_PUT_NO_LIGHTSTATE); }
    catch(ErrCodeExcp excp)
     { Log.excp(excp); }

    try
     { throw new ErrCodeExcp(ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,
                      "(<additional description>)"); }
    catch(ErrCodeExcp excp)
     { Log.excp(excp); }

    try
     { throw new DevErrCodeExcp(ERR_LIGHT_PUT_NO_LIGHTSTATE,1); }
    catch(ErrCodeExcp excp)
     { Log.excp(excp); }

    try
     { throw new DevErrCodeExcp(ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,
                          2,"(<additional description>)"); }
    catch(ErrCodeExcp excp)
     { Log.excp(excp); }
   }
 }