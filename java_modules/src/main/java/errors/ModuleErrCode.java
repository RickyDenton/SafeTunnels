/* SafeTunnels Module Error Code Interface Definition */

package errors;

/* =========================== INTERFACE DEFINITION =========================== */
public interface ModuleErrCode extends ErrCode
 {
  /**
   * @return The errCodeInfo object associated with a module error code
   */
  ErrCodeInfo getErrCodeInfo();

  /**
   * @return The module name associated with a module error code
   */
  String getModuleName();
 }