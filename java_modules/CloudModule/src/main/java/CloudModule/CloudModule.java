package CloudModule;


import

// Paho imports
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class CloudModule implements MqttCallback
 {

  // MQTT Broker endpoint
  private final static String MQTT_BROKER_ENDPOINT = "tcp://127.0.0.1:1883";

  // CloudModule MQTT ClientID
  private final static String MQTT_CLI_ID = "CloudModule";

  // Sensors MQTT topics
  private final static String TOPIC_SENSORS_ERRORS = "SafeTunnels/sensorsErrors";
  private final static String TOPIC_SENSORS_C02 = "SafeTunnels/C02";
  private final static String TOPIC_SENSORS_TEMP = "SafeTunnels/temp";


  // Constructor
  public CloudModule() throws MqttException
   {




    MqttClient mqttClient = new MqttClient(MQTT_BROKER_ENDPOINT, MQTT_CLI_ID);
    System.out.println("Connecting to broker: "+MQTT_BROKER_ENDPOINT);

    mqttClient.setCallback( this );

    mqttClient.connect();

    mqttClient.subscribe(TOPIC_SENSORS_ERRORS);

    mqttClient.subscribe(TOPIC_SENSORS_C02);
    mqttClient.subscribe(TOPIC_SENSORS_TEMP);
   }

 public void connectionLost(Throwable cause) {
  // TODO Auto-generated method stub
 }

 public void messageArrived(String topic, MqttMessage message) throws Exception {
  System.out.println(String.format("[%s]: %s", topic, new String(message.getPayload())));
 }

 public void deliveryComplete(IMqttDeliveryToken token) {
  // TODO Auto-generated method stub
 }


 public static void main(String[] args) {


  try
   {
    Log.dbg("Hello World")



   // No code
   public static void dbg(String logStr)
   { System.out.println(COLOR_DBG + "[DBG]: " + logStr + COLOR_RST); }

   public static void info(String logStr)
   { System.out.println(COLOR_INFO + "[INFO]: " + logStr + COLOR_RST); }

   public static void warn(String logStr)
   { System.out.println(COLOR_WARNING + "[WARN]: " + logStr + COLOR_RST); }

   public static void err(String logStr)
   { System.out.println(COLOR_ERROR + "[ERR]: " + logStr + COLOR_RST); }

   public static void fatal(String logStr)
   { System.out.println(COLOR_FATAL + "[FATAL]: " + logStr + COLOR_RST); }

   // Device error codes
   public static void code(DeviceErrCode devErrCode, short devID)
   { code(devErrCode,devID,""); }

   public static void code(DeviceErrCode devErrCode, short devID, String addDscr)
   {
    ErrCodeInfo errCodeInfo = devErrCode.getErrCodeInfo();
    System.out.println(ErrCodeSevColorMap.get(errCodeInfo.sevLev) + printHeadDevSev(devErrCode.getDevType(),devID,errCodeInfo.sevLev) + " " + addDscr + COLOR_RST);
   }



   // CloudModule mc = new CloudModule();


  } catch(MqttException me) {

   me.printStackTrace();
  }
 }

}