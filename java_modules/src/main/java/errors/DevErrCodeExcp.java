package errors;

public class DevErrCodeExcp extends ErrCodeExcp
 {
  public final int devID;

  public DevErrCodeExcp(ErrCode errCode, int devID)
   {
    super(errCode);
    this.devID = devID;
   }

  public DevErrCodeExcp(ErrCode errCode, int devID, String addDscr)
   {
    super(errCode,addDscr);
    this.devID = devID;
   }
 }
