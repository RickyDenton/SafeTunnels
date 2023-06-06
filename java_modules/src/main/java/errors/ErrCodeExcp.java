package errors;

public class ErrCodeExcp extends Exception
 {
  public final ErrCode errCode;
  public final String addDscr;

  public ErrCodeExcp(ErrCode errCode)
   {
    this.errCode = errCode;
    this.addDscr = "";
   }

  public ErrCodeExcp(ErrCode errCode,String addDscr)
   {
    this.errCode = errCode;
    this.addDscr = addDscr;
   }
 }
