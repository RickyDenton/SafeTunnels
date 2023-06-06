package CloudModule;

// Paho imports
import devices.DevErrCode;
import errors.DevErrCodeExcp;
import errors.ErrCodeExcp;
import errors.ErrCodeInfo;
import errors.ErrCodeSeverity;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import devices.sensor.BaseSensor;
import logging.Log;

import java.util.Map;

import static devices.actuator.ActuatorErrCode.ERR_LIGHT_PUT_NO_LIGHTSTATE;
import static devices.sensor.SensorErrCode.*;
import static errors.ErrCodeSeverity.ERROR;
import static errors.ErrCodeSeverity.WARNING;


public class CloudModule implements MqttCallback
 {

  // MQTT Broker endpoint
  private final static String MQTT_BROKER_ENDPOINT = "tcp://127.0.0.1:1883";

  // CloudModule MQTT ClientID
  private final static String MQTT_CLI_ID = "CloudModule";



  // Constructor
  public CloudModule() throws MqttException
   {
    MqttClient mqttClient = new MqttClient(MQTT_BROKER_ENDPOINT, MQTT_CLI_ID);
    System.out.println("Connecting to broker: "+MQTT_BROKER_ENDPOINT);

    mqttClient.setCallback( this );

    mqttClient.connect();

    mqttClient.subscribe(BaseSensor.TOPIC_SENSORS_ERRORS);

    mqttClient.subscribe(BaseSensor.TOPIC_SENSORS_C02);
    mqttClient.subscribe(BaseSensor.TOPIC_SENSORS_TEMP);
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


  public static void main(String[] args)
   {
    try {

    CloudModule mc = new CloudModule();


   } catch(MqttException me) {

    me.printStackTrace();
   }
  }

 }