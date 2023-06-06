package modules.MySQLConnector;

import devices.actuator.BaseActuator;
import devices.sensor.BaseSensor;
import logging.Log;

import java.sql.*;
import java.util.HashMap;


public class MySQLConnector
 {
  // SafeTunnels MySQL Database connection parameters
  public final static String MYSQL_DB_HOST = "jdbc:mysql://localhost:3306/";
  public final static String MYSQL_DB_NAME = "SafeTunnelsCoojaDB";
  public final static String MYSQL_DB_ENDPOINT = MYSQL_DB_HOST + MYSQL_DB_NAME;
  public final static String MYSQL_DB_USER = "root";
  public final static String MYSQL_DB_PWD = "iot2023";

  Connection mySQLConn;
  Statement stmt;

  public MySQLConnector() throws java.sql.SQLException
   {
    try
     {
      mySQLConn = DriverManager.getConnection(MYSQL_DB_ENDPOINT,MYSQL_DB_USER,MYSQL_DB_PWD);
      stmt = mySQLConn.createStatement();
     }
    catch(SQLException sqlExcp)
     {
      mySQLConn.close();
      stmt.close();
      throw sqlExcp;
     }
   }

  HashMap<String,BaseSensor> getSensorsMap()
   {
    HashMap<String,BaseSensor> sensorsMap = new HashMap<>();

    String getAllSensorsQuery = "SELECT * FROM sensors";

    try(ResultSet sensorsSet = stmt.executeQuery(getAllSensorsQuery))
     {
      while(sensorsSet.next())
       { sensorsMap.put(sensorsSet.getString("mac"),new BaseSensor(sensorsSet.getShort("sensorID"))); }

      if(sensorsMap.isEmpty())
       Log.err("No sensors were found in the database");
     }
    catch(SQLException sqlExcp)
     { Log.err("Failed to retrieve the sensors list from the database (reason = " + sqlExcp + ")"); }

    return sensorsMap;
   }


  HashMap<String,BaseActuator> getActuatorsMap()
   {
    HashMap<String,BaseActuator> actuatorsMap = new HashMap<>();

    String getAllActuatorsQuery = "SELECT * FROM actuators";

    try(ResultSet actuatorsSet = stmt.executeQuery(getAllActuatorsQuery))
     {
      while(actuatorsSet.next())
       { actuatorsMap.put(actuatorsSet.getString("mac"),new BaseActuator(actuatorsSet.getShort("actuatorID"))); }
      if(actuatorsMap.isEmpty())
       Log.err("No sensors were found in the database");
     }
    catch(SQLException sqlExcp)
     { Log.err("Failed to retrieve the sensors list from the database (reason = " + sqlExcp + ")"); }

    return actuatorsMap;
   }

  // TODO: setSensorOnline(int sensorID)
  // TODO: setSensorOffline(int sensorID)
  // TODO: pushC02Value(int sensorID,int C02Value)
  // TODO: pushTempValue(int sensorID,int tempValue)
 }
