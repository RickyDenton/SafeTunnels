package logging;

public abstract class Log
 {
  public static void logDbg(String logStr)
   { System.out.println("\u001B[34m" + "[DBG]: " + logStr + "\u001B[0m"); }

  public static void logInfo(String logStr)
   { System.out.println("\u001B[32m" + "[INFO]: " + logStr + "\u001B[0m"); }

  public static void logWarn(String logStr)
   { System.out.println("\u001B[33m" + "[WARN]: " + logStr + "\u001B[0m"); }

  public static void logErr(String logStr)
   { System.out.println("\u001B[31m" + "[ERR]: " + logStr + "\u001B[0m"); }

  public static void logFatal(String logStr)
   { System.out.println("\u001B[91m" + "[ERR]: " + logStr + "\u001B[0m"); }
 }