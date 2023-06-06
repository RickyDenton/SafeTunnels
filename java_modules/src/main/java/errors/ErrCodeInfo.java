package errors;

public class ErrCodeInfo
 {
  final public ErrCodeSeverity sevLev;    // The error code severity level (FATAL to INFO)
  final public String         humanDscr; // The error code human-readable description

  public ErrCodeInfo(ErrCodeSeverity sevLev, String humanDscr)
   {
    this.sevLev = sevLev;
    this.humanDscr = humanDscr;
   }
 }