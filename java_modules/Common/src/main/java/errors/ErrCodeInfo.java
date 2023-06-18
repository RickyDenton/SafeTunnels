/*
 * SafeTunnels Error Info Class, used for specifying an
 * ErrCode's severity level and human-readable description
 */

package errors;

/* ============================== CLASS DEFINITION ============================== */
public class ErrCodeInfo
 {
  /* ============================ PUBLIC ATTRIBUTES ============================ */

  // The associated ErrCode's severity level (from DEBUG to FATAL)
  final public ErrCodeSeverity sevLev;

  // The associated ErrCode's human-readable description
  final public String humanDscr;


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * ErrCodeInfo constructor, initializing its attributes
   * @param sevLev The associated ErrCode's severity level (from DEBUG to FATAL)
   * @param humanDscr The associated ErrCode's human-readable description
   */
  public ErrCodeInfo(ErrCodeSeverity sevLev, String humanDscr)
   {
    this.sevLev = sevLev;
    this.humanDscr = humanDscr;
   }
 }