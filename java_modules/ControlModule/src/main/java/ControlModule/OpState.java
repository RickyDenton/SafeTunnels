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

  /**
   * EnumMap mapping each operating state with its associated Color
   */
  private static final EnumMap<OpState,Color> operatingStatesColorMap = new EnumMap<>(Map.ofEntries
   (
    Map.entry(NOMINAL,new Color(44,125,10)),
    Map.entry(WARNING,new Color(242,108,3)),
    Map.entry(ALERT,new Color(203,46,0)),
    Map.entry(EMERGENCY,new Color(241,57,0))
   ));


  /* ========================== Enumeration Methods  ========================== */

  /**
   * @return The Color object associated with the operating state
   */
  public Color getColor()
   { return operatingStatesColorMap.get(this); }
 }