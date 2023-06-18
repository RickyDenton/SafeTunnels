/* SafeTunnels Error Code Interface */

package errors;

/* =========================== INTERFACE DEFINITION =========================== */
public interface ErrCode
 {
  /**
   * @return The errCodeInfo object associated with an error code
   */
  ErrCodeInfo getErrCodeInfo();
 }