/* SafeTunnels Base Device Class (extended by BaseSensor and BaseActuator) */

package devices;

/* ============================== CLASS DEFINITION ============================== */
public abstract class BaseDevice implements Comparable<BaseDevice>
 {
  /* ============================= DEVICE TYPE ENUM ============================= */
  public enum DevType
   {
    sensor,
    actuator
   }


  /* ================================ ATTRIBUTES ================================ */

  // The device's (unique) MAC
  public final String MAC;

  // The device's unique ID in the SafeTunnels database
  public final short ID;

  // The device's current connection state (false -> offline, true -> online)
  protected boolean connState;


  /* ============================ PROTECTED METHODS ============================ */

  /**
   * BaseDevice constructor, initializing its attributes
   * @param MAC The device's (unique) MAC address
   * @param ID  The device's unique ID in the SafeTunnels database
   */
  protected BaseDevice(String MAC, short ID)
   {
    this.MAC = MAC;
    this.ID = ID;
    connState = false;
   }


  /* ============================== PUBLIC METHODS ============================== */

  /**
   * @return The device's connection state
   *         (0 -> offline, 1 -> online)
   */
  public boolean getConnState()
   { return connState; }


  /**
   * Sets the device as OFFLINE (possibly pushing
   * its updated connState into the database)
   */
  public abstract void setConnStateOffline();


  /**
   * Sets the device as ONLINE (possibly pushing
   * its updated connState into the database)
   */
  public abstract void setConnStateOnline();


  /**
   * @return The device's type ('sensor' || 'actuator)
   */
  public abstract DevType getDevType();


  /**
   * Compare override defining that BaseDevices should be
   * compared for ordering purposes based on their ID
   * @param baseDevice The other BaseDevice this
   *                   one should be compared against
   * @return As for the compareTo semantics (-1, 0, 1)
   */
  @Override
  public int compareTo(BaseDevice baseDevice)
   { return Short.compare(ID,baseDevice.ID); }
 }