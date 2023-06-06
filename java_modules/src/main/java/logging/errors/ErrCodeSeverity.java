package logging.errors;

public enum ErrCodeSeverity
 {
  FATAL  { @Override public String toString() { return "FATAL"; } },
  ERROR  { @Override public String toString() { return "ERROR"; } },
  WARNING  { @Override public String toString() { return "WARNING"; } },
  INFO  { @Override public String toString() { return "INFO"; } },
  DEBUG  { @Override public String toString() { return "DEBUG"; } }
 }