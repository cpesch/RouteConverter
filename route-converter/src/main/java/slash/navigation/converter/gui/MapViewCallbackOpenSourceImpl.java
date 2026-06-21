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

import slash.navigation.converter.gui.helpers.MapViewCallbackImpl;
import slash.navigation.elevation.ElevationService;
import slash.navigation.gui.Application;
import slash.navigation.gui.models.BooleanModel;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.mapview.mapsforge.MapViewCallbackOpenSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.navigation.gui.helpers.WindowHelper.getFrame;
import static slash.navigation.gui.helpers.WindowHelper.handleOutOfMemoryError;

/**
 * Implements the callbacks from the MapsforgeMapView to the RouteConverterOpenSource services.
 *
 * @author Christian Pesch
 */

public class MapViewCallbackOpenSourceImpl extends MapViewCallbackImpl implements MapViewCallbackOpenSource {
    private static final Logger log = Logger.getLogger(MapViewCallbackOpenSourceImpl.class.getName());

    public MapsforgeMapManager getMapsforgeMapManager() {
        return ((RouteConverterOpenSource) Application.getInstance()).getMapsforgeMapManager();
    }

    public ElevationService getElevationService() {
        return ((RouteConverter) Application.getInstance()).getElevationServiceFacade().getElevationService();
    }

    private ResourceBundle getBundle() {
        return Application.getInstance().getContext().getBundle();
    }

    public void handleRoutingException(Throwable t) {
        if (t instanceof OutOfMemoryError)
            handleOutOfMemoryError((OutOfMemoryError) t);
        else {
            StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            log.severe("Cannot route position list: " + getLocalizedMessage(t) + ", " + writer);
            showMessageDialog(getFrame(), format(getBundle().getString("cannot-route-position-list"), t),
                    getFrame().getTitle(), ERROR_MESSAGE);
        }
    }

    public void showMapException(String mapName, Exception e) {
        log.severe("Cannot display map " + mapName + ": " + getLocalizedMessage(e));
        showMessageDialog(getFrame(), format(getBundle().getString("cannot-display-map"), mapName, e),
                getFrame().getTitle(), ERROR_MESSAGE);
    }
}
