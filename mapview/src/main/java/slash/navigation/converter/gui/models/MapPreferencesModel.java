/*
 *
 *     This file is part of RouteConverter.
 *
 *     RouteConverter is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     RouteConverter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with RouteConverter; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *     Copyright (C) 2007 Christian Pesch. All Rights Reserved.
 * /
 */

package slash.navigation.converter.gui.models;

import slash.navigation.gui.models.BooleanModel;
import slash.navigation.mapview.MapView;
import slash.navigation.routing.RoutingPreferencesModel;

/**
 * A model for the preferences that affect a {@link MapView}.
 *
 * @author Christian Pesch
 */
public class MapPreferencesModel {
    private static final String SHOW_COORDINATES_PREFERENCE = "showCoordinates";
    private static final String SHOW_SHADED_HILLS_PREFERENCE = "showShadedHills";
    private static final String SHOW_WAYPOINT_DESCRIPTION_PREFERENCE = "showWaypointDescription";

    private final RoutingPreferencesModel routingPreferencesModel;
    private final CharacteristicsModel characteristicsModel;
    private final UnitSystemModel unitSystemModel;
    private final BooleanModel showCoordinatesModel = new BooleanModel(SHOW_COORDINATES_PREFERENCE, false);
    private final BooleanModel showShadedHills = new BooleanModel(SHOW_SHADED_HILLS_PREFERENCE, false);
    private final BooleanModel showWaypointDescriptionModel = new BooleanModel(SHOW_WAYPOINT_DESCRIPTION_PREFERENCE, false); // only BrowserMapView
    private final ColorModel routeColorModel = new ColorModel("route", "C86CB1F3"); // "6CB1F3" w 0.8 alpha
    private final ColorModel trackColorModel = new ColorModel("track", "FF0033FF"); // "0033FF" w 1.0 alpha
    private final ColorModel waypointColorModel = new ColorModel("waypoint", "FF000000"); // "000000" w 1.0 alpha
    private final FixMapModeModel fixMapModeModel = new FixMapModeModel();

    public MapPreferencesModel(RoutingPreferencesModel routingPreferencesModel,
                               CharacteristicsModel characteristicsModel,
                               UnitSystemModel unitSystemModel) {
        this.routingPreferencesModel = routingPreferencesModel;
        this.characteristicsModel = characteristicsModel;
        this.unitSystemModel = unitSystemModel;
    }

    public RoutingPreferencesModel getRoutingPreferencesModel() {
        return routingPreferencesModel;
    }

    public CharacteristicsModel getCharacteristicsModel() {
        return characteristicsModel;
    }

    public UnitSystemModel getUnitSystemModel() {
        return unitSystemModel;
    }

    public BooleanModel getShowCoordinatesModel() {
        return showCoordinatesModel;
    }

    public BooleanModel getShowShadedHills() {
        return showShadedHills;
    }

    public BooleanModel getShowWaypointDescriptionModel() {
        return showWaypointDescriptionModel;
    }

    public ColorModel getRouteColorModel() {
        return routeColorModel;
    }

    public ColorModel getTrackColorModel() {
        return trackColorModel;
    }

    public ColorModel getWaypointColorModel() {
        return waypointColorModel;
    }

    public FixMapModeModel getFixMapModeModel() {
        return fixMapModeModel;
    }
}
