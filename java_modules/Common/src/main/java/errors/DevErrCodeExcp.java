/* SafeTunnels Devices Error Code Exception Definition */

package errors;

/* ============================== CLASS DEFINITION ============================== */
public class DevErrCodeExcp extends ErrCodeExcp
 {
  /* ============================ PUBLIC ATTRIBUTES ============================ */

  // The ID of the device the exception is associated with
  public final int devID;


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * ErrCode-only DevErrCode Exception constructor
   * @param errCode The ErrCode that has occurred
   * @param devID The ID of the device the exception is associated with
   */
  public DevErrCodeExcp(ErrCode errCode, int devID)
   {
    super(errCode);
    this.devID = devID;
   }

  /**
   * ErrCode + addDscr DevErrCode Exception constructor
   * @param errCode The ErrCode that has occurred
   * @param devID The ID of the device the exception is associated with
   * @param addDscr An additional human-readable description
   *                associated with the error that has occurred
   */
  public DevErrCodeExcp(ErrCode errCode, int devID, String addDscr)
   {
    super(errCode,addDscr);
    this.devID = devID;
   }
 }