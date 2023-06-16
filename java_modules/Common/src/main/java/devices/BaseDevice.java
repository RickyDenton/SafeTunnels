package devices;


public abstract class BaseDevice implements Comparable<BaseDevice>
 {
  // Device type enumeration
  public enum DevType
   {
    sensor,
    actuator
   }

  // The device's (unique) MAC
  public final String MAC;

  // The device's unique ID in the SafeTunnels database
  public final short ID;

  // The device's current connection state (false -> offline, true -> online)
  protected boolean connState;



  // Sets the device as offline
  public abstract void setConnStateOffline();

  // Set the device online (subclasses only)
  public abstract void setConnStateOnline();

  // Get the connection state
  public boolean getConnState()
   { return connState; }

  /**
   * @return The device's type ('sensor' || 'actuator)
   */
  public abstract DevType getDevType();

  protected BaseDevice(String MAC, short ID)
   {
    this.MAC = MAC;
    this.ID = ID;
    connState = false;
   }


  @Override
  public int compareTo(BaseDevice baseDevice)
   { return Short.compare(ID,baseDevice.ID); }
 }

