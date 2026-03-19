package slash.navigation.routing;

/**
 * Travel restrictions for a routing service.
 *
 * @author Christian Pesch
 */
public record TravelRestrictions(boolean avoidBridges, boolean avoidFerries, boolean avoidMotorways, boolean avoidTolls,
                                 boolean avoidTunnels) {

    public static final TravelRestrictions NO_RESTRICTIONS = new TravelRestrictions(false, false, false, false, false);
}
