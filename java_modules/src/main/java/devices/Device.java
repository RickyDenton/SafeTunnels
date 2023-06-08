/* SafeTunnels Device Interface Definition */

package devices;

/* =========================== INTERFACE DEFINITION =========================== */
public interface Device
 {
  // Device type
  enum DevType
   {
    sensor,
    actuator
   }

  /**
   * @return The device's unique ID in the SafeTunnels database
   */
  short getID();

  /**
   * @return The device's type ('sensor' || 'actuator)
   */
  DevType getDevType();
 }
