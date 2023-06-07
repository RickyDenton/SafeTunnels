package CloudModule;

import modules.MySQLConnector.MySQLConnector;


public class CloudMySQLConnector extends MySQLConnector
 {
  public CloudMySQLConnector()
   { super(); }

  public void pushSensorConnStatus(int devID, boolean connStatus) throws java.sql.SQLException
   {
    // Convert the boolean connStatus to a (short) bit)
    short connStatusBit = connStatus?(short)1:(short)0;

    // Attempt to push the sensor connection status update into the database
    pushDevState(ST_DB_SENSORS_TABLE_CONNSTATE,"sensorID","connState",devID,String.valueOf(connStatusBit));
   }

  public void pushSensorQuantity(String quantityTable, String quantityColumn, int devID, int quantityValue) throws java.sql.SQLException
   {
    // Attempt to push the sensor quantity update into the database
    pushDevState(quantityTable,"sensorID",quantityColumn,devID,String.valueOf(quantityValue));
   }
 }
