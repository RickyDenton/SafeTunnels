package errors;

public interface ModuleErrCode extends ErrCode
 {
  public ErrCodeInfo getErrCodeInfo();

  public String getModuleName();
 }
