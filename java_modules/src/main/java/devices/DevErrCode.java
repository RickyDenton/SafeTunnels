/* SafeTunnels Device Error Code Interface Definition */

package devices;

/* ================================== IMPORTS ================================== */

/* --------------------------- SafeTunnels Packages --------------------------- */
import errors.ErrCodeInfo;
import errors.ErrCode;
import devices.Device.DevType;


/* =========================== INTERFACE DEFINITION =========================== */
public interface DevErrCode extends ErrCode
 {
  /**
   * @return The errCodeInfo object associated with the device error code
   */
  ErrCodeInfo getErrCodeInfo();

  /**
   * @return The device type associated with the device error code
   */
  DevType getDevType();
 }