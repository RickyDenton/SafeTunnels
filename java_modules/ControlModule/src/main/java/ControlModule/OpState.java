/**
 * The possible operating states of the SafeTunnels's
 *  - System (= max among all its sensors)
 *  - Sensor (= max among its C02 density and Temperature state)
 *    - C02 Density
 *    - Temperature
 */

package ControlModule;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/* --------------------------- SafeTunnels Resources --------------------------- */
import devices.actuator.BaseActuator.LightState;
import static devices.actuator.BaseActuator.LightState.*;


/* ============================== ENUM DEFINITION ============================== */
public enum OpState
 {
  /* ====================== Enumeration Values Definition ====================== */

  // The component's parameters are within their nominal thresholds
  NOMINAL,

  // One or more of the component's parameters are beyond
  // their nominal but within their alert thresholds
  WARNING,

  // One or more of the component's parameters are beyond
  // their alert but within their emergency thresholds
  ALERT,

  // One or more of the component's parameters are beyond their emergency thresholds
  EMERGENCY;

  /* =================== Operating State <--> Colors Mappings =================== */

  private static final EnumMap<OpState,Color> opStatesColorMap = new EnumMap<>(Map.ofEntries
   (
    Map.entry(NOMINAL,new Color(47,132,11)),
    Map.entry(WARNING,new Color(214,171,0)),
    Map.entry(ALERT,new Color(242,108,3)),
    Map.entry(EMERGENCY,new Color(203,46,0))
   ));


  /* ============== Fan Relative Speed Operating States Thresholds ============== */

  private static final int FANRELSPEED_THRESHOLD_WARNING = 30;
  private static final int FANRELSPEED_THRESHOLD_ALERT = 55;
  private static final int FANRELSPEED_THRESHOLD_EMERGENCY = 80;


  /* ===== Operating State <--> Automatic Mode Fan Relative Speeds Mappings ===== */

  private static final EnumMap<OpState,Integer> opStatesAutoFanRelSpeedMap = new EnumMap<>(Map.ofEntries
    (
      Map.entry(NOMINAL,0),
      Map.entry(WARNING,30),
      Map.entry(ALERT,60),
      Map.entry(EMERGENCY,100)
    ));

  /* ========= Operating State <--> Automatic Mode LightState Mappings ========= */

  private static final EnumMap<OpState, LightState> opStatesAutoLightStateMap = new EnumMap<>(Map.ofEntries
    (
      Map.entry(NOMINAL,LIGHT_OFF),
      Map.entry(WARNING,LIGHT_ON),
      Map.entry(ALERT,LIGHT_BLINK_ALERT),
      Map.entry(EMERGENCY,LIGHT_BLINK_EMERGENCY)
    ));


  /* ========================== Enumeration Methods  ========================== */

  /**
   * @param fanRelSpeed A fan relative speed value
   * @return The operating state color associated with such fan relative speed
   */
  public static Color fanRelSpeedToOpStateColor(int fanRelSpeed)
   {
    if(fanRelSpeed < FANRELSPEED_THRESHOLD_WARNING)
     return opStatesColorMap.get(NOMINAL);
    if(fanRelSpeed < FANRELSPEED_THRESHOLD_ALERT)
     return opStatesColorMap.get(WARNING);
    if(fanRelSpeed < FANRELSPEED_THRESHOLD_EMERGENCY)
     return opStatesColorMap.get(ALERT);
    else
     return opStatesColorMap.get(EMERGENCY);
   }

  /**
   * @return The Color object associated with the operating state
   */
  public Color getColor()
   { return opStatesColorMap.get(this); }

  /**
   * @return The automatic mode fan relative speed
   *         associated with the operating state
   */
  public Integer getAutoFanRelSpeed()
   { return opStatesAutoFanRelSpeedMap.get(this); }

  /**
   * @return The automatic mode light state
   *         associated with the operating state
   */
  public LightState getAutoLightState()
   { return opStatesAutoLightStateMap.get(this); }
 }