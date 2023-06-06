package devices;

import errors.ErrCodeInfo;
import errors.SafeTunnelsErrCode;


public interface DeviceErrCode extends SafeTunnelsErrCode
 {
  public ErrCodeInfo getErrCodeInfo();
  public String getDevType();
 }
