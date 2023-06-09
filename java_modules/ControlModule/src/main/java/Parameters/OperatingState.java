/* Components possible operating states */

package Parameters;

/* ================================== IMPORTS ================================== */

/* -------------------------- Java Standard Libraries -------------------------- */
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;


/* ============================== ENUM DEFINITION ============================== */
public enum OperatingState
 {
  /* ====================== Enumeration Values Definition ====================== */

  // Nominal state, the component's attributes are within their nominal margins
  NOMINAL,

  // One or more of the component's attributes are beyond their nominal margins
  WARNING,

  // One or more of the component's attributes are beyond their warning margins
  ALERT,

  // One or more of the component's attributes are beyond their alert margins
  EMERGENCY;

  /**
   * EnumMap mapping each operating state with its associated Color
   */
  private static final EnumMap<OperatingState,Color> operatingStatesColorMap = new EnumMap<>(Map.ofEntries
   (
    Map.entry(NOMINAL,new Color(44,125,10)),
    Map.entry(WARNING,new Color(242,108,3)),
    Map.entry(ALERT,new Color(203,46,0)),
    Map.entry(EMERGENCY,new Color(241,57,0))
   ));


  /* ========================== Enumeration Methods  ========================== */

  /**
   * @return The Color object associated with an operating state
   */
  public Color getColor()
   { return operatingStatesColorMap.get(this); }
 }