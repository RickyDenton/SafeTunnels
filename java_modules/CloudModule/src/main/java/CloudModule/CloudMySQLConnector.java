/* Cloud Module SafeTunnels MySQL Database Connector */

package CloudModule;

/* ================================== IMPORTS ================================== */

/* -------------------------- Java Standard Libraries -------------------------- */
import modules.MySQLConnector.MySQLConnector;

/* ============================== CLASS DEFINITION ============================== */
final class CloudMySQLConnector extends MySQLConnector
 {
  /* ============================== PACKAGE METHODS ============================== */

  /**
   *  CloudMySQLConnector constructor, attempting to establish
   *  a connection with the SafeTunnels MySQL database
   */
  CloudMySQLConnector()
   { super(); }

  /**
   * Pushes an updated sensor connection status into the database
   * @param sensorID The ID of the sensor to update the connection status
   * @param connStatus The updated connection status as a boolean
   *                   (false -> offline, true -> online)
   * @throws java.sql.SQLException Failed to update the sensor
   *                               connection state in the database
   */
  void pushSensorConnStatus(int sensorID, boolean connStatus) throws java.sql.SQLException
   {
    // Convert the boolean "connStatus" to a (short) bit (0 -> offline, 1 -> online)
    short connStatusBit = connStatus?(short)1:(short)0;

    // Attempt to push the updated sensor connection status into the database
    pushDevState(ST_DB_SENSORS_TABLE_CONNSTATE,"sensorID",
      "connState",sensorID,String.valueOf(connStatusBit));
   }

  /**
   * Pushes an updated sensor quantity reading (C02 or temperature) into the database
   * @param quantityTable  The quantity's table
   *                       (ST_DB_SENSORS_TABLE_C02 || ST_DB_SENSORS_TABLE_TEMP)
   * @param quantityColumn The quantity's column name
   *                       (ST_DB_SENSORS_COLUMN_C02 || ST_DB_SENSORS_COLUMN_TEMP)
   * @param sensorID       The ID of the sensor that has reported the reading
   * @param quantityValue  The updated quantity value
   * @throws java.sql.SQLException Failed to push the updated
   *                               quantity value in the database
   */
  void pushSensorQuantity(String quantityTable, String quantityColumn, int sensorID, int quantityValue) throws java.sql.SQLException
   { pushDevState(quantityTable,"sensorID",quantityColumn,sensorID,String.valueOf(quantityValue)); }
 }