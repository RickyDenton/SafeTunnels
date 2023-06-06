package devices.actuator;

public class Actuator
 {
  protected final String actuatorID;
  protected final String MAC;
  protected LightState lightState;
  protected Short relFanSpeed;

  Actuator(String actuatorID,String MAC)
   {
    this.actuatorID = actuatorID;
    this.MAC = MAC;
    this.lightState = null;
    this.relFanSpeed = null;
   }

  @Override
  public boolean equals(Object o)
   {
    if(this==o)
     return true;
    if(!(o instanceof Actuator))
     return false;

    Actuator actuator = (Actuator)o;

    if(!actuatorID.equals(actuator.actuatorID))
     return false;
    if(!MAC.equals(actuator.MAC))
     return false;
    if(lightState!=actuator.lightState)
     return false;
    return relFanSpeed!=null?relFanSpeed.equals(actuator.relFanSpeed):actuator.relFanSpeed==null;
   }

  @Override
  public int hashCode()
   {
    int result = actuatorID.hashCode();
    result = 31 * result + MAC.hashCode();
    result = 31 * result + (lightState!=null?lightState.hashCode():0);
    result = 31 * result + (relFanSpeed!=null?relFanSpeed.hashCode():0);
    return result;
   }
 }