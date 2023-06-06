package errors;

public enum ErrCodeSeverity
 {
  DEBUG  { @Override public String toString() { return "DEBUG"; } },
  INFO  { @Override public String toString() { return "INFO"; } },
  WARNING  { @Override public String toString() { return "WARNING"; } },
  ERROR  { @Override public String toString() { return "ERROR"; } },
  FATAL  { @Override public String toString() { return "FATAL"; } }
 }