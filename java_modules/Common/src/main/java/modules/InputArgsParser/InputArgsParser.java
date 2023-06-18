/* SafeTunnels Application Command-Line Input Arguments Parser */

package modules.InputArgsParser;

/* ================================== IMPORTS ================================== */

/* --------------------------- SafeTunnels Resources --------------------------- */
import logging.Log;
import errors.ErrCodeSeverity;
import modules.MySQLConnector.MySQLConnector;


/* ============================== CLASS DEFINITION ============================== */
public abstract class InputArgsParser
 {

  /* ============================== PUBLIC METHODS ============================== */

  /**
   * Parses a SafeTunnels application command-line input arguments
   * @param appName The SafeTunnels application
   *                name ("CloudModule" or "ControlModule")
   * @param args    The command line input arguments with
   *                which the application was started
   */
  public static void parseCMDInputArgs(String appName, String[] args)
   {
    // A boolean used to check whether the
    // provided input arguments are valid
    boolean validArgs = true;

    // Cycle all provided arguments
    for(int i = 0; i < args.length; i++)
     {

      /* ------------ "-db" option (override target database) ------------ */
      if(args[i].equalsIgnoreCase("-db"))
       {
        // If a value was provided for the option
        if(args.length > i + 1)
         {
          // Override the MySQLConnector database name with the provided value
          MySQLConnector.ST_DB_NAME = args[i + 1];
          MySQLConnector.ST_DB_ENDPOINT = MySQLConnector.ST_DB_HOST + MySQLConnector.ST_DB_NAME;
          Log.info("Target database set to \"" + MySQLConnector.ST_DB_NAME + "\"");

          // Move one (and so two) values ahead the input argument index
          i++;
         }

        // Otherwise, if a value was NOT provided for the option
        else
         {
          // Log that a value must be provided for the option
          Log.err("Missing \"-db\" value (the local database name)");

          // Set that the provided input arguments
          // are not valid and break from the cycle
          validArgs = false;
          break;
         }
       }
      else

       /* -------------- "-log" option (override log level) -------------- */
       if(args[i].equalsIgnoreCase("-log"))
        {
         // If a value was provided for the option
         if(args.length > i + 1)
          {
           // The log level override to be used
           ErrCodeSeverity logLevelOverride;

           // Attempt to interpret the following input argument as a ErrCode Severity
           if(args[i + 1].equalsIgnoreCase("dbg") ||
             args[i + 1].equalsIgnoreCase("DEBUG"))
            logLevelOverride = ErrCodeSeverity.DEBUG;
           else
            if(args[i + 1].equalsIgnoreCase("info"))
             logLevelOverride = ErrCodeSeverity.INFO;
            else
             if(args[i + 1].equalsIgnoreCase("warn") ||
               args[i + 1].equalsIgnoreCase("WARNING"))
              logLevelOverride = ErrCodeSeverity.WARNING;
             else
              if(args[i + 1].equalsIgnoreCase("err") ||
                args[i + 1].equalsIgnoreCase("ERROR"))
               logLevelOverride = ErrCodeSeverity.ERROR;
              else
               if(args[i + 1].equalsIgnoreCase("fatal"))
                logLevelOverride = ErrCodeSeverity.FATAL;

               // If the following input argument could not
               // be interpreted as a ErrCode Severity
               else
                {
                 // Log that the following input argument could
                 // not be interpreted as a ErrCode Severity
                 Log.err("\"" + args[i + 1] + "\" could not "
                          + "be interpreted as a valid log level");

                 // Set that the provided input arguments
                 // are not valid and break from the cycle
                 validArgs = false;
                 break;
                }

           // Override the default with the provided valid log level
           Log.LOG_LEVEL = logLevelOverride;
           Log.info("Log level set to '" + logLevelOverride + "'");

           // Move one (and so two) values ahead the input argument index
           i++;
          }

         // Otherwise, if a value was NOT provided for the option
         else
          {
           // Log that a value must be provided for the option
           Log.err("Missing \"-log\" argument (the log level to be applied)");

           // Set that the provided input arguments
           // are not valid and break from the cycle
           validArgs = false;
           break;
          }
        }

       // Unsupported command line option
       else
        {
         // Log that the provided option is not supported
         Log.err("Unsupported command line option \"" + args[i] +  "\"");

         // Set that the provided input arguments
         // are not valid and break from the cycle
         validArgs = false;
         break;
       }
     }

    /*/ ----------- Outside the input arguments parsing cycle ----------- /*/

    // If invalid command-line inputs arguments were provided
    if(!validArgs)
     {
      // Display a helper message outlining the
      // program's allowed options and values
      System.out.println("Usage: java " + appName + " [-db \"targetDatabase\"] "
                         + "[-log \"logLevelOverride\"]  " +
                         "logLevelOverride: {DEBUG, WARNING, INFO, ERROR, FATAL}");

      // Terminate the program
      System.exit(1);
     }
   }
 }