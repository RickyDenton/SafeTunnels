package devices.sensor;

import devices.Device;


public class BaseSensor implements Device
 {
  // Sensors MQTT topics
  public final static String TOPIC_SENSORS_C02 = "SafeTunnels/C02";
  public final static String TOPIC_SENSORS_TEMP = "SafeTunnels/temp";
  public final static String TOPIC_SENSORS_ERRORS = "SafeTunnels/sensorsErrors";

  public final short deviceID;

  public BaseSensor(short deviceID)
   {this.deviceID = deviceID;}
 }