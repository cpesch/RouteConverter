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

package slash.navigation.googlemaps;

import java.util.Locale;
import java.util.prefs.Preferences;

import static java.util.Locale.CHINA;
import static slash.common.helpers.LocaleHelper.UZBEKISTAN;

/**
 * Enumeration of supported Google Maps servers.
 *
 * @author Christian Pesch
 */

public enum GoogleMapsServer {
    China("http://maps.google.cn", CHINA),
    Ditu("http://ditu.google.cn", null),
    International("https://maps.googleapis.com", null),
    Uzbekistan("http://maps.google.ru", UZBEKISTAN);

    private final String url;
    private final Locale preset;

    GoogleMapsServer(String url, Locale preset) {
        this.url = url;
        this.preset = preset;
    }

    public String getUrl() {
        return url;
    }

    public Locale getPreset() {
        return preset;
    }

    // manage defaults

    private static final Preferences preferences = Preferences.userNodeForPackage(GoogleMapsServer.class);
    private static final String GOOGLE_MAPS_SERVER_PREFERENCE = "googleMapsServer";

    private static GoogleMapsServer getDefaultGoogleMapsServer() {
        for (GoogleMapsServer googleMapsServer : GoogleMapsServer.values()) {
            if (Locale.getDefault().equals(googleMapsServer.getPreset()))
                return googleMapsServer;
        }
        return International;
    }

    public static GoogleMapsServer getGoogleMapsServer() {
        try {
            return GoogleMapsServer.valueOf(preferences.get(GOOGLE_MAPS_SERVER_PREFERENCE, getDefaultGoogleMapsServer().name()));
        } catch (IllegalArgumentException e) {
            return International;
        }
    }

    public static void setGoogleMapsServer(GoogleMapsServer googleMapsServer) {
        preferences.put(GOOGLE_MAPS_SERVER_PREFERENCE, googleMapsServer.toString());
    }
}
