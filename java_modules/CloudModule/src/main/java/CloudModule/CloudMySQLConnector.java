/* Cloud Module SafeTunnels MySQL Database Connector */

package CloudModule;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import java.sql.SQLException;

/* --------------------------- SafeTunnels Resources --------------------------- */
import logging.Log;
import modules.MySQLConnector.MySQLConnector;
import static CloudModule.CloudMySQLConnectorErrCode.*;
import devices.sensor.BaseSensor.SensorQuantity;
import static devices.sensor.BaseSensor.SensorQuantity.C02;


/* ============================== CLASS DEFINITION ============================== */
final class CloudMySQLConnector extends MySQLConnector
 {

  /* ============================= PRIVATE METHODS ============================= */

  /**
   * @param sensorQuantity The sensor quantity to retrieve the
   *                       database table name (C02 || TEMP)
   * @return The SafeTunnels database table name
   *         associated with a sensor quantity
   */
  private String getSensorQuantityTableName(SensorQuantity sensorQuantity)
   {
    if(sensorQuantity == C02)
     return ST_DB_SENSORS_TABLE_C02;
    else
     return ST_DB_SENSORS_TABLE_TEMP;
   }


  /**
   * @param sensorQuantity The sensor quantity to retrieve the
   *                       database column name (C02 || TEMP)
   * @return The SafeTunnels database column name
   *         associated with a sensor quantity
   */
  private String getSensorQuantityColumnName(SensorQuantity sensorQuantity)
   {
    if(sensorQuantity == C02)
     return ST_DB_SENSORS_COLUMN_C02;
    else
     return ST_DB_SENSORS_COLUMN_TEMP;
   }


  /* ============================== PACKAGE METHODS ============================== */

  /**
   *  Cloud MySQL Connector constructor, attempting to establish
   *  a connection with the SafeTunnels MySQL database
   */
  CloudMySQLConnector()
   { super(); }


  /**
   * Pushes an updated sensor connection state into the database
   * @param sensorID The ID of the sensor to update the connection status
   * @param connState The sensor updated connection state as a
   *                  boolean (false -> offline, true -> online)
   */
  void pushSensorConnState(int sensorID, boolean connState)
   {
    // Convert the boolean "connState" into a (short) bit (0 -> offline, 1 -> online)
    short connStatusBit = connState?(short)1:(short)0;

    try
     {
      // Attempt to push the updated sensor connection state into the database
      pushDevState(ST_DB_SENSORS_TABLE_CONNSTATE,ST_DB_SENSORS_COLUMN_ID,
                   ST_DB_COMMON_COLUMN_CONNSTATE,sensorID,String.valueOf(connStatusBit));

      // If successful, log the updated sensor connection state
      if(connState)
       Log.info("sensor" + sensorID + " is now online (pushed into the database)");
      else
       Log.warn("sensor" + sensorID + " appears to be offline (pushed into the database)");
     }
    catch(SQLException sqlExcp)
     { Log.code(ERR_CLOUD_PUSH_CONNSTATE_FAILED,"(sensorID = " + sensorID + ", "
                + "connState = " + connState + ", reason = " + sqlExcp.getMessage() + ")"); }
   }


  /**
   * Pushes an updated sensor quantity value into the database
   * @param sensorID The ID of the sensor to update the quantity value
   * @param sensorQuantity The sensor quantity to be updated (C02 || TEMP)
   * @param quantityValue The updated quantity value
   */
  void pushSensorQuantityValue(int sensorID, SensorQuantity sensorQuantity, int quantityValue)
   {
    // Retrieve the names of the database table and
    // column associated with the quantity to be updated
    String sensorQuantityTable = getSensorQuantityTableName(sensorQuantity);
    String sensorQuantityColumn = getSensorQuantityColumnName(sensorQuantity);

    try
     {
      // Attempt to push the updated sensor quantity value into the database
      pushDevState(sensorQuantityTable,ST_DB_SENSORS_COLUMN_ID,sensorQuantityColumn,sensorID,String.valueOf(quantityValue));

      // If successful, log the updated sensor quantity
      Log.info("Pushed sensor" + sensorID + " updated " + sensorQuantity + " (" + quantityValue + ") into the database");
     }
    catch(SQLException sqlExcp)
     { Log.code(ERR_CLOUD_PUSH_QUANTITY_FAILED,"(sensorID = " + sensorID + ", quantity = "
       + sensorQuantity + ", value = " + quantityValue + ", reason = " + sqlExcp.getMessage() + ")"); }
   }
 }