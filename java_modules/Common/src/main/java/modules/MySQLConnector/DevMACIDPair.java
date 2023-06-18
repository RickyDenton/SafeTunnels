/* A device's MAC-ID pairing retrieved from the SafeTunnels Database */

package modules.MySQLConnector;

/* ============================== CLASS DEFINITION ============================== */
public class DevMACIDPair
 {
  /* ============================ PUBLIC ATTRIBUTES ============================ */

  // The device's (unique) MAC
  public String MAC;

  // The device's unique ID in the SafeTunnels database
  public short ID;

  /* ============================= PUBLIC METHODS ============================= */

  /**
   * DevMACIDPair constructor, initializing its attributes
   * @param MAC The device's (unique) MAC
   * @param ID  The device's unique ID in the SafeTunnels database
   */
  public DevMACIDPair(String MAC, short ID)
   {
    this.MAC = MAC;
    this.ID = ID;
   }
 }