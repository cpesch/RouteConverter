/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.converter.gui;

import slash.common.helpers.APIKeyRegistry;
import slash.navigation.converter.gui.helpers.AutomaticElevationService;
import slash.navigation.converter.gui.helpers.AutomaticGeocodingService;
import slash.navigation.converter.gui.helpers.GoogleDirections;
import slash.navigation.googlemaps.GoogleService;
import slash.navigation.mapview.MapViewCallback;
import slash.navigation.routing.RoutingService;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;

import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.showInputDialog;
import static slash.common.io.Transfer.trim;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowserForGoogleAPIKey;

/**
 * A small graphical user interface for the route conversion based on Google Maps APIs.
 *
 * @author Christian Pesch
 */

public class RouteConverterGoogle extends RouteConverter {
    public static void main(String[] args) {
        launch(RouteConverterGoogle.class, new String[]{RouteConverter.class.getPackage().getName() + ".Untranslated", RouteConverter.class.getName()}, args);
    }

    public String getEdition() {
        return "RouteConverter Google Edition";
    }

    public String getEditionId() {
        return "online";
    }

    protected void checkForGoogleMapsAPIKey() {
        String apiKey = APIKeyRegistry.getInstance().getAPIKey("google", "map");
        if (apiKey != null)
            return;

        JLabel labelGoogleAPIKeyMissing = new JLabel(MessageFormat.format(getBundle().getString("google-apikey-missing"), getEdition()));
        labelGoogleAPIKeyMissing.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                startBrowserForGoogleAPIKey(getFrame());
            }
        });
        String input = showInputDialog(getFrame(), labelGoogleAPIKeyMissing, getTitle(), QUESTION_MESSAGE);
        if (trim(input) != null)
            APIKeyRegistry.getInstance().setAPIKeyPreference("google", input);
    }

    protected MapViewCallback getMapViewCallback() {
        return new MapViewCallbackGoogleImpl();
    }

    protected void initializeElevationServices() {
        AutomaticElevationService service = new AutomaticElevationService(getElevationServiceFacade());
        getElevationServiceFacade().addElevationService(service);
        getElevationServiceFacade().setPreferredElevationService(service);

        getElevationServiceFacade().addElevationService(new GoogleService());
    }

    protected void updateElevationServices() {
    }

    protected void initializeGeocodingServices() {
        AutomaticGeocodingService service = new AutomaticGeocodingService(getGeocodingServiceFacade());
        getGeocodingServiceFacade().addGeocodingService(service);
        getGeocodingServiceFacade().setPreferredGeocodingService(service);

        getGeocodingServiceFacade().addGeocodingService(new GoogleService());
    }

    protected void initializeRoutingServices() {
        RoutingService service = new GoogleDirections();
        getRoutingServiceFacade().addRoutingService(service);
        getRoutingServiceFacade().setPreferredRoutingService(service);
    }

    protected void updateRoutingServices() {
    }

    protected void scanLocalMapsAndThemes() {
    }

    protected void scanRemoteMapsAndThemes() {
    }
}
