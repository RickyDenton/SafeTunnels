/* Control Module SafeTunnels MySQL Database Connector */

package ControlModule.ControlMySQLConnector;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import java.sql.SQLException;

/* --------------------------- SafeTunnels Resources --------------------------- */
import logging.Log;
import modules.MySQLConnector.MySQLConnector;
import static ControlModule.ControlMySQLConnector.ControlMySQLConnectorErrCode.*;
import devices.actuator.BaseActuator.ActuatorQuantity;
import static devices.actuator.BaseActuator.ActuatorQuantity.FANRELSPEED;


/* ============================== CLASS DEFINITION ============================== */
final public class ControlMySQLConnector extends MySQLConnector
 {
  /* ============================= PRIVATE METHODS ============================= */

  /**
   * @param actuatorQuantity The actuator quantity to retrieve the
   *                         database table name (FANRELSPEED || LIGHTSTATE)
   * @return The SafeTunnels database table name
   *         associated with an actuator quantity
   */
  private String getActuatorQuantityTableName(ActuatorQuantity actuatorQuantity)
   {
    if(actuatorQuantity == FANRELSPEED)
     return ST_DB_ACTUATORS_TABLE_FANRELSPEED;
    else
     return ST_DB_ACTUATORS_TABLE_LIGHTSTATE;
   }


  /**
   * @param actuatorQuantity The actuator quantity to retrieve the database
   *                          column name (FANRELSPEED || LIGHTSTATE)
   * @return The SafeTunnels database column name
   *         associated with an actuator quantity
   */
  private String getActuatorQuantityColumnName(ActuatorQuantity actuatorQuantity)
   {
    if(actuatorQuantity == FANRELSPEED)
     return ST_DB_ACTUATORS_COLUMN_FANRELSPEED;
    else
     return ST_DB_ACTUATORS_COLUMN_LIGHTSTATE;
   }


  /* ============================== PUBLIC METHODS ============================== */

  /**
   *  Control MySQL Connector constructor, attempting to
   *  establish a connection with the SafeTunnels MySQL database
   */
  public ControlMySQLConnector()
   { super(); }


  /**
   * Pushes an updated actuator connection state into the database
   * @param actuatorID The ID of the actuator to update the connection status
   * @param connState The actuator updated connection state as a
   *                  boolean (false -> offline, true -> online)
   */
  public void pushActuatorConnState(int actuatorID, boolean connState)
   {
    // Convert the boolean "connState" into a (short) bit (0 -> offline, 1 -> online)
    short connStatusBit = connState?(short)1:(short)0;

    // Attempt to push the updated actuator connection
    // state into the database, logging the error otherwise
    try
     {
      pushDevState(ST_DB_ACTUATORS_TABLE_CONNSTATE,ST_DB_ACTUATORS_COLUMN_ID,
        ST_DB_COMMON_COLUMN_CONNSTATE,actuatorID,String.valueOf(connStatusBit));
     }
    catch(SQLException sqlExcp)
     { Log.code(ERR_CONTROL_PUSH_CONNSTATE_FAILED,"(actuatorID = " + actuatorID + ", "
       + "connState = " + connState + ", reason = " + sqlExcp.getMessage() + ")"); }
   }


  /**
   * Pushes an updated actuator quantity value into the database
   * @param actuatorID The ID of the actuator to update the quantity value
   * @param actuatorQuantity The actuator quantity to be updated (FANRELSPEED || LIGHTSTATE)
   * @param quantityValue The updated quantity value
   */
  public void pushActuatorQuantityValue(int actuatorID, ActuatorQuantity actuatorQuantity, int quantityValue)
   {
    // Retrieve the names of the database table and
    // column associated with the quantity to be updated
    String actuatorQuantityTableName = getActuatorQuantityTableName(actuatorQuantity);
    String actuatorQuantityColumnName = getActuatorQuantityColumnName(actuatorQuantity);

    // Attempt to push the updated actuator quantity value
    // into the database, logging the error otherwise
    try
     {
      pushDevState(actuatorQuantityTableName,ST_DB_ACTUATORS_COLUMN_ID,
                   actuatorQuantityColumnName,actuatorID,String.valueOf(quantityValue));
     }
    catch(SQLException sqlExcp)
     {
      /*
       * The SQL error code 1062 is associated with failing to insert a record because another
       * such primary key (in this case, {actuatorID,timestamp}) already exists in the destination
       * table, which may occur for actuators quickly varying their state,and so can be ignored
       */
      if(sqlExcp.getErrorCode() != 1062)
       Log.code(ERR_CONTROL_PUSH_QUANTITY_FAILED,"(actuatorID = " + actuatorID + ", quantity = "
        + actuatorQuantity + ", value = " + quantityValue + ", reason = " + sqlExcp.getMessage() + ")");
     }
   }
 }