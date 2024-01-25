package slash.navigation.routing;

/**
 * Travel restrictions for a routing service.
 *
 * @author Christian Pesch
 */
public class TravelRestrictions {
    public static final TravelRestrictions NO_RESTRICTIONS = new TravelRestrictions(false, false, false, false, false);

    private boolean avoidBridges;
    private boolean avoidFerries;
    private boolean avoidMotorways;
    private boolean avoidTolls;
    private boolean avoidTunnels;

    public TravelRestrictions(boolean avoidBridges, boolean avoidFerries, boolean avoidMotorways, boolean avoidTolls, boolean avoidTunnels) {
        this.avoidBridges = avoidBridges;
        this.avoidFerries = avoidFerries;
        this.avoidMotorways = avoidMotorways;
        this.avoidTolls = avoidTolls;
        this.avoidTunnels = avoidTunnels;
    }

    public boolean isAvoidBridges() {
        return avoidBridges;
    }

    public boolean isAvoidFerries() {
        return avoidFerries;
    }

    public boolean isAvoidMotorways() {
        return avoidMotorways;
    }

    public boolean isAvoidTolls() {
        return avoidTolls;
    }

    public boolean isAvoidTunnels() {
        return avoidTunnels;
    }
}