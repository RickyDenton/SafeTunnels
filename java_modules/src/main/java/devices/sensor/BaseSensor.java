package devices.sensor;

import devices.Device;


public class BaseSensor implements Device
 {
  public final short deviceID;

  public BaseSensor(short deviceID)
   {this.deviceID = deviceID;}
 }