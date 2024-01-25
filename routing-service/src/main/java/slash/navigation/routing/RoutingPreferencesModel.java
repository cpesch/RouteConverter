package slash.navigation.routing;

import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;

/**
 * A model for the preferences that affect a {@link RoutingService}.
 *
 * @author Christian Pesch
 */
public class RoutingPreferencesModel {
    private static final Preferences preferences = Preferences.userNodeForPackage(RoutingPreferencesModel.class);
    private static final Logger log = Logger.getLogger(RoutingPreferencesModel.class.getName());

    private static final String ROUTING_SERVICE_PREFERENCE = "routingService";
    private static final String TRAVEL_MODE_PREFERENCE = "travelMode";
    private static final String AVOID_BRIDGES_PREFERENCE = "avoidBridges";
    private static final String AVOID_FERRIES_PREFERENCE = "avoidFerries";
    private static final String AVOID_MOTORWAYS_PREFERENCE = "avoidMotorways";
    private static final String AVOID_TOLLS_PREFERENCE = "avoidTolls";
    private static final String AVOID_TUNNELS_PREFERENCE = "avoidTunnels";

    private final EventListenerList listenerList = new EventListenerList();

    private final List<RoutingService> routingServices = new ArrayList<>();
    private RoutingService preferredRoutingService;
    private boolean loggedFailedRoutingServiceWarning, loggedFailedTravelModeWarning;

    public List<RoutingService> getRoutingServices() {
        return routingServices;
    }

    public void addRoutingService(RoutingService routingService) {
        routingServices.add(routingService);
    }

    public void setPreferredRoutingService(RoutingService preferredRoutingService) {
        this.preferredRoutingService = preferredRoutingService;
    }

    public RoutingService getRoutingService() {
        String lookupServiceName = preferences.get(ROUTING_SERVICE_PREFERENCE, preferredRoutingService.getName());

        for (RoutingService service : getRoutingServices()) {
            if (lookupServiceName.endsWith(service.getName()))
                return service;
        }

        if (!loggedFailedRoutingServiceWarning) {
            log.warning(format("Failed to find routing service %s; using preferred %s", lookupServiceName, preferredRoutingService.getName()));
            loggedFailedRoutingServiceWarning = true;
        }
        return preferredRoutingService;
    }

    public void setRoutingService(RoutingService service) {
        preferences.put(ROUTING_SERVICE_PREFERENCE, service.getName());
        fireChanged();
    }

    public TravelMode getTravelMode() {
        RoutingService service = getRoutingService();
        TravelMode preferredTravelMode = service.getPreferredTravelMode();
        String lookupName = preferences.get(TRAVEL_MODE_PREFERENCE + service.getName(), preferredTravelMode.getName());

        for (TravelMode travelMode : service.getAvailableTravelModes()) {
            if (lookupName.equals(travelMode.getName()))
                return travelMode;
        }

        if (!loggedFailedTravelModeWarning) {
            log.warning(format("Failed to find travel mode %s; using preferred travel mode %s", lookupName, preferredTravelMode.getName()));
            loggedFailedTravelModeWarning = true;
        }
        return preferredTravelMode;
    }

    public void setTravelMode(TravelMode travelMode) {
        preferences.put(TRAVEL_MODE_PREFERENCE + getRoutingService().getName(), travelMode.getName());
        fireChanged();
    }

    public TravelRestrictions getTravelRestrictions() {
        return new TravelRestrictions(getAvoidPreference(AVOID_BRIDGES_PREFERENCE),
                getAvoidPreference(AVOID_FERRIES_PREFERENCE), getAvoidPreference(AVOID_MOTORWAYS_PREFERENCE),
                getAvoidPreference(AVOID_TOLLS_PREFERENCE), getAvoidPreference(AVOID_TUNNELS_PREFERENCE));
    }

    private boolean getAvoidPreference(String key) {
        return preferences.getBoolean(key + getRoutingService().getName(), false);
    }

    private void setAvoidPreference(String key, boolean avoidPreference) {
        preferences.putBoolean(key + getRoutingService().getName(), avoidPreference);
        fireChanged();
    }

    public void setAvoidBridges(boolean avoidBridges) {
        setAvoidPreference(AVOID_BRIDGES_PREFERENCE, avoidBridges);
    }

    public void setAvoidFerries(boolean avoidFerries) {
        setAvoidPreference(AVOID_FERRIES_PREFERENCE, avoidFerries);
    }

    public void setAvoidMotorways(boolean avoidMotorways) {
        setAvoidPreference(AVOID_MOTORWAYS_PREFERENCE, avoidMotorways);
    }

    public void setAvoidTolls(boolean avoidTolls) {
        setAvoidPreference(AVOID_TOLLS_PREFERENCE, avoidTolls);
    }

    public void setAvoidTunnels(boolean avoidTunnels) {
        setAvoidPreference(AVOID_TUNNELS_PREFERENCE, avoidTunnels);
    }

    protected void fireChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(null);
            }
        }
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }
}
