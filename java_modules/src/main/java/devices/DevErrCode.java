package devices;

import errors.ErrCodeInfo;
import errors.ErrCode;


public interface DevErrCode extends ErrCode
 {
  public ErrCodeInfo getErrCodeInfo();
  public String getDevType();
 }
