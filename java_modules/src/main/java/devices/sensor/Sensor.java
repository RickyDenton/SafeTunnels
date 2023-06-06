package devices.sensor;

public class Sensor
 {
  protected final String sensorID;
  protected final String MAC;
  protected Short temp;
  protected Short C02Density;

  Sensor(String sensorID,String MAC)
   {
    this.sensorID = sensorID;
    this.MAC = MAC;
    temp = null;
    C02Density = null;
   }

  @Override
  public String toString()
   {
    return "Sensor{" + "sensorID='" + sensorID + '\'' + ", MAC='" + MAC + '\'' + ", temp=" + temp + ", C02Density="
      + C02Density + '}';
   }

  @Override
  public boolean equals(Object o)
   {
    if(this==o)
     return true;
    if(!(o instanceof Sensor))
     return false;

    Sensor sensor = (Sensor)o;

    if(!sensorID.equals(sensor.sensorID))
     return false;
    if(!MAC.equals(sensor.MAC))
     return false;
    if(temp!=null?!temp.equals(sensor.temp):sensor.temp!=null)
     return false;
    return C02Density!=null?C02Density.equals(sensor.C02Density):sensor.C02Density==null;
   }

  @Override
  public int hashCode()
   {
    int result = sensorID.hashCode();
    result = 31 * result + MAC.hashCode();
    result = 31 * result + (temp!=null?temp.hashCode():0);
    result = 31 * result + (C02Density!=null?C02Density.hashCode():0);
    return result;
   }
 }
