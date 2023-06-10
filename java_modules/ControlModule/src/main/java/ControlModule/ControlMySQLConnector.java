/* Cloud Module SafeTunnels MySQL Database Connector */

package ControlModule;

/* ================================== IMPORTS ================================== */

/* -------------------------- Java Standard Libraries -------------------------- */
import devices.actuator.BaseActuator;
import logging.Log;
import modules.MySQLConnector.MySQLConnector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import static modules.MySQLConnector.MySQLConnectorErrCode.ERR_DB_GET_ACTUATORS;


/* ============================== CLASS DEFINITION ============================== */
final class ControlMySQLConnector extends MySQLConnector
 {
  /* ============================== PACKAGE METHODS ============================== */

  /**
   *  CloudMySQLConnector constructor, attempting to establish
   *  a connection with the SafeTunnels MySQL database
   */
  ControlMySQLConnector()
   { super(); }

  /**
   * Pushes an updated actuator connection status into the database
   * @param actuatorID The ID of the actuator to update the connection status
   * @param connStatus The updated connection status as a boolean
   *                   (false -> offline, true -> online)
   * @throws java.sql.SQLException Failed to update the actuator
   *                               connection state in the database
   */
  void pushActuatorConnStatus(int actuatorID, boolean connStatus) throws java.sql.SQLException
   {
    // Convert the boolean "connStatus" to a (short) bit (0 -> offline, 1 -> online)
    short connStatusBit = connStatus?(short)1:(short)0;

    // Attempt to push the updated actuator connection status into the database
    pushDevState(ST_DB_ACTUATORS_TABLE_CONNSTATE,"actuatorID",
      "connState",actuatorID,String.valueOf(connStatusBit));
   }

  /**
   * Pushes an updated actuator quantity (fan relative speed or light state) into the database
   * @param quantityTable  The quantity's table
   *                       (ST_DB_ACTUATORS_TABLE_FANRELSPEED || ST_DB_ACTUATORS_TABLE_LIGHTSTATE)
   * @param quantityColumn The quantity's column name
   *                       (ST_DB_ACTUATORS_COLUMN_FANRELSPEED || ST_DB_ACTUATORS_COLUMN_LIGHTSTATE)
   * @param actuatorID       The ID of the actuator whose quantity has updated
   * @param quantityValue  The updated quantity value
   * @throws java.sql.SQLException Failed to push the updated
   *                               quantity value in the database
   */
  void pushActuatorQuantity(String quantityTable, String quantityColumn, int actuatorID, int quantityValue) throws java.sql.SQLException
   { pushDevState(quantityTable,"actuatorID",quantityColumn,actuatorID,String.valueOf(quantityValue)); }


  // TODO!

  /*
  // Returns the Arraylist of ActuatorManagers in the database
  public HashMap<String,BaseActuator> getDBActuatorsMap()
   {
    // <MAC,BaseActuator> map of sensors in the database to be returned
    HashMap<String,BaseActuator> actuatorsMap = new HashMap<>();

    // Ensure the database connection to be active
    checkDBConn();

    // Query to retrieve all actuators in the database
    String getAllActuatorsQuery = "SELECT * FROM " + ST_DB_ACTUATORS_TABLE;

    // Attempt to initialize the statement
    try(Statement mySQLStmt = STDBConn.createStatement())
     {
      // Attempt to retrieve the set of actuators in the database
      try(ResultSet actuatorsSet = mySQLStmt.executeQuery(getAllActuatorsQuery))
       {
        // For every actuator tuple returned, initialize and append
        // its associated <MAC,BaseActuator> element in the actuatorsMap
        while(actuatorsSet.next())
         { actuatorsMap.put(actuatorsSet.getString("mac"),new BaseActuator(actuatorsSet.getShort("actuatorID"))); }

        // Retrieving no actuators from the database is a FATAL error
        if(actuatorsMap.isEmpty())
         Log.code(ERR_DB_GET_ACTUATORS);
       }
     }

    // Failing to retrieve the set of actuators in the database is a FATAL error
    catch(SQLException sqlExcp)
     { Log.code(ERR_DB_GET_ACTUATORS, "(reason = " + sqlExcp.getMessage() + ")"); }

    // Return the <MAC,BaseActuator> map of actuators in the database
    return actuatorsMap;
   }

   */
 }