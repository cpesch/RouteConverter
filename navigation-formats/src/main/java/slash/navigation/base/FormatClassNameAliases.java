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

    Copyright (C) 2026 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.base;

import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Maps the class names of GPSBabel-backed formats in {@code slash.navigation.babel} to the
 * package of their native port, so a format remembered or counted under its old class name is
 * resolved to the new one once the native port exists. Until a format is ported, its target
 * class does not exist yet and {@link #resolve} and {@link #migrateCounters} leave the old name
 * untouched.
 *
 * @author Christian Pesch
 */

public final class FormatClassNameAliases {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("slash.navigation.babel.GeoCachingFormat", "slash.navigation.loc.GeoCachingFormat"),
            Map.entry("slash.navigation.babel.GarminPoiDbFormat", "slash.navigation.garmin.GarminPoiDbFormat"),
            Map.entry("slash.navigation.babel.TourExchangeFormat", "slash.navigation.tef.TourExchangeFormat"),
            Map.entry("slash.navigation.babel.TomTomPoiFormat", "slash.navigation.tomtom.TomTomPoiFormat"),
            Map.entry("slash.navigation.babel.Igo8TrackFormat", "slash.navigation.igo.Igo8TrackFormat"),
            Map.entry("slash.navigation.babel.GarminPcx5Format", "slash.navigation.garmin.GarminPcx5Format"),
            Map.entry("slash.navigation.babel.CompeGPSDataRouteFormat", "slash.navigation.compegps.CompeGPSDataRouteFormat"),
            Map.entry("slash.navigation.babel.CompeGPSDataTrackFormat", "slash.navigation.compegps.CompeGPSDataTrackFormat"),
            Map.entry("slash.navigation.babel.CompeGPSDataWaypointFormat", "slash.navigation.compegps.CompeGPSDataWaypointFormat"),
            Map.entry("slash.navigation.babel.OziExplorerRouteFormat", "slash.navigation.ozi.OziExplorerRouteFormat"),
            Map.entry("slash.navigation.babel.OziExplorerTrackFormat", "slash.navigation.ozi.OziExplorerTrackFormat"),
            Map.entry("slash.navigation.babel.OziExplorerWaypointFormat", "slash.navigation.ozi.OziExplorerWaypointFormat"),
            Map.entry("slash.navigation.babel.FlightRecorderDataFormat", "slash.navigation.igc.FlightRecorderDataFormat"),
            Map.entry("slash.navigation.babel.MagellanMapSendFormat", "slash.navigation.magellan.MagellanMapSendFormat"),
            Map.entry("slash.navigation.babel.AlanTrackLogFormat", "slash.navigation.alan.AlanTrackLogFormat"),
            Map.entry("slash.navigation.babel.AlanWaypointsAndRoutesFormat", "slash.navigation.alan.AlanWaypointsAndRoutesFormat"),
            Map.entry("slash.navigation.babel.HoluxM241BinaryFormat", "slash.navigation.holux.HoluxM241BinaryFormat"),
            Map.entry("slash.navigation.babel.NationalGeographicTopo3Format", "slash.navigation.topo.NationalGeographicTopo3Format"),
            Map.entry("slash.navigation.babel.GarminMapSource5Format", "slash.navigation.garmin.GarminMapSource5Format"),
            Map.entry("slash.navigation.babel.GarminMapSource6Format", "slash.navigation.garmin.GarminMapSource6Format"),
            Map.entry("slash.navigation.babel.GarminPoiFormat", "slash.navigation.garmin.GarminPoiFormat"));

    private FormatClassNameAliases() {
    }

    /**
     * Resolves a remembered format class name to its new package once the native port exists;
     * otherwise returns {@code className} unchanged. Returns {@code null} and {@code ""} unchanged.
     */
    public static String resolve(String className) {
        return resolve(className, ALIASES);
    }

    static String resolve(String className, Map<String, String> aliases) {
        if (className == null || className.isEmpty())
            return className;
        String target = aliases.get(className);
        if (target == null || !isLoadable(target))
            return className;
        return target;
    }

    /**
     * Migrates the usage counters stored under each of {@code keyPrefixes} from a format's old
     * class name to its new one, once the new class exists. Idempotent: a counter already
     * migrated has no old key left to migrate again.
     */
    public static void migrateCounters(Preferences preferences, String... keyPrefixes) {
        migrateCounters(preferences, ALIASES, keyPrefixes);
    }

    static void migrateCounters(Preferences preferences, Map<String, String> aliases, String... keyPrefixes) {
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            String newClassName = entry.getValue();
            if (!isLoadable(newClassName))
                continue;

            String oldClassName = entry.getKey();
            for (String keyPrefix : keyPrefixes) {
                String oldKey = keyPrefix + oldClassName;
                if (preferences.get(oldKey, null) == null)
                    continue;

                String newKey = keyPrefix + newClassName;
                int migrated = preferences.getInt(oldKey, 0) + preferences.getInt(newKey, 0);
                preferences.putInt(newKey, migrated);
                preferences.remove(oldKey);
            }
        }
    }

    private static boolean isLoadable(String className) {
        try {
            Class.forName(className, false, FormatClassNameAliases.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
