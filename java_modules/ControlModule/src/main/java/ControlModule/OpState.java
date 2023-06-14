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
import devices.actuator.BaseActuator.LightState;

import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

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

  /* =================== Operating State <--> Colors Mapping =================== */

  private static final EnumMap<OpState,Color> opStatesColorMap = new EnumMap<>(Map.ofEntries
   (
    Map.entry(NOMINAL,new Color(47,132,11)),
    Map.entry(WARNING,new Color(214,171,0)),
    Map.entry(ALERT,new Color(242,108,3)),
    Map.entry(EMERGENCY,new Color(203,46,0))
   ));

  /* ===== Operating State <--> Automatic Mode Fan Relative Speeds Mapping ===== */

  private static final EnumMap<OpState,Integer> opStatesAutoFanRelSpeedMap = new EnumMap<>(Map.ofEntries
    (
      Map.entry(NOMINAL,0),
      Map.entry(WARNING,30),
      Map.entry(ALERT,60),
      Map.entry(EMERGENCY,100)
    ));

  /* ========= Operating State <--> Automatic Mode LightState Mapping ========= */

  private static final EnumMap<OpState,LightState> opStatesAutoLightStateMap = new EnumMap<>(Map.ofEntries
    (
      Map.entry(NOMINAL,LIGHT_OFF),
      Map.entry(WARNING,LIGHT_ON),
      Map.entry(ALERT,LIGHT_BLINK_ALERT),
      Map.entry(EMERGENCY,LIGHT_BLINK_EMERGENCY)
    ));

  /* ========================== Enumeration Methods  ========================== */

  public static Color fanRelSpeedToOpStateColor(int fanRelSpeed)
   {
    if(fanRelSpeed < 30)
     return opStatesColorMap.get(NOMINAL);
    if(fanRelSpeed < 55)
     return opStatesColorMap.get(WARNING);
    if(fanRelSpeed < 80)
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
   * @return The automatic mode fan relative speed associated with the operating state
   */
  public Integer getAutoFanRelSpeed()
   { return opStatesAutoFanRelSpeedMap.get(this); }

  /**
   * @return The automatic mode light state associated with the operating state
   */
  public LightState getAutoLightState()
   { return opStatesAutoLightStateMap.get(this); }
 }