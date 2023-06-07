package devices;

public interface Device
 {
  // The device types
  public enum DevType
   {
    sensor,
    actuator
   };

  // The device's unique ID in the database
  public final String deviceID = null;
 }
