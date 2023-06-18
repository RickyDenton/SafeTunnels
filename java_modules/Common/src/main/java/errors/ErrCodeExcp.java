/* SafeTunnels Error Code Exception Definition */

package errors;

/* ============================== CLASS DEFINITION ============================== */
public class ErrCodeExcp extends Exception
 {
  /* ============================ PUBLIC ATTRIBUTES ============================ */

  // The ErrCode that has occurred
  public final ErrCode errCode;

  // An additional human-readable description
  // associated with the error that has occurred
  public final String addDscr;


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * ErrCode-only ErrCode Exception constructor
   * @param errCode The ErrCode that has occurred
   */
  public ErrCodeExcp(ErrCode errCode)
   {
    this.errCode = errCode;
    this.addDscr = "";
   }


  /**
   * ErrCode + addDscr ErrCode Exception constructor
   * @param errCode The ErrCode that has occurred
   * @param addDscr An additional human-readable description
   *                associated with the error that has occurred
   */
  public ErrCodeExcp(ErrCode errCode,String addDscr)
   {
    this.errCode = errCode;
    this.addDscr = addDscr;
   }
 }