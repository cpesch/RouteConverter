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

    Translated to Java from GPSBabel garmin_tables.cc/.h (master 9c1e336 and gpsbabel_1_6_0),
    gdb.cc/.h (master 9c1e336) and mapsource.cc (gpsbabel_1_6_0) for RouteConverter, 2026; modified.
    Copyright (C) 2002-2007 Robert Lipe, robertlipe+source@gpsbabel.org
    Copyright (C) 2005-2008 Olaf Klein, o.b.klein@gpsbabel.org
*/

package slash.navigation.garmin;

/**
 * The Garmin defaults GPSBabel writes when a waypoint, route or track carries no
 * Garmin specific data, which is always the case for RouteConverter positions:
 * the "Waypoint" symbol, the waypoint classes, the display modes and the colour table.
 * <p>
 * Shared block S4 of the native MapSource (.mps, .gdb) and Garmin POI (.gpi) ports
 * (rc-meta spec 00083). Deliberately not a port of the full symbol tables: RouteConverter
 * does not keep symbols, so GPSBabel always resolves them to its default.
 */
public final class GarminDefaults {
    private GarminDefaults() {
    }

    // --- symbols -----------------------------------------------------------------------

    /** {@code DEFAULT_ICON_DESCR} in garmin_tables.cc, {@code DEFAULT_ICON} in garmin_gpi.cc */
    public static final String DEFAULT_ICON_DESCRIPTION = "Waypoint";
    /** {@code DEFAULT_ICON_VALUE} in garmin_tables.cc, {@code DEFAULTICONVALUE} in mapsource.cc */
    public static final int DEFAULT_ICON_NUMBER = 18;
    /** {@code GdbFormat::kGDBDefIcon} in gdb.h */
    public static final int GDB_DEFAULT_ICON_NUMBER = 18;

    /** the formats {@code gt_find_icon_number_from_desc()} distinguishes ({@code garmin_formats_e}) */
    public enum Format {
        MAPSOURCE, PCX, GARMIN_SERIAL, GDB
    }

    /**
     * The symbol number {@code gt_find_icon_number_from_desc()} returns for a waypoint
     * without a symbol (null or empty description): {@code DEFAULT_ICON_VALUE}, the
     * "Waypoint" symbol, which carries the same number 18 in the MapSource/GDB and the
     * PCX/serial column of both icon tables.
     */
    public static int waypointIconNumber(Format format) {
        if (format == null)
            throw new IllegalArgumentException("unknown garmin format");
        return DEFAULT_ICON_NUMBER;
    }

    /**
     * The "Waypoint" symbol number the MapSource writer stores for the given .mps version
     * (3, 4 or 5), after {@code mps_converted_icon_number()} in mapsource.cc.
     */
    public static int mapSourceWaypointIconNumber(int mapSourceVersion) {
        return mapSourceConvertedIconNumber(waypointIconNumber(Format.MAPSOURCE), mapSourceVersion);
    }

    /**
     * The "Waypoint" symbol number the GDB writer stores for a waypoint without a symbol
     * in the given .gdb version (1, 2 or 3): {@code kGDBDefIcon} in all versions.
     */
    public static int gdbWaypointIconNumber(int gdbVersion) {
        if (gdbVersion < 1 || gdbVersion > 3)
            throw new IllegalArgumentException("gdb version " + gdbVersion + " not in 1..3");
        return GDB_DEFAULT_ICON_NUMBER;
    }

    /**
     * {@code mps_converted_icon_number()} for {@code MAPSOURCE} from mapsource.cc (gpsbabel_1_6_0):
     * MapSource 5 knows all symbols, version 4 lacks the water hydrant, version 3 additionally
     * the contact symbols and draws geocaches as "Circle with X".
     */
    static int mapSourceConvertedIconNumber(int iconNumber, int mapSourceVersion) {
        if (mapSourceVersion == 5)
            return iconNumber;
        if (mapSourceVersion == 4)
            return iconNumber == 139 ? DEFAULT_ICON_NUMBER : iconNumber;
        if (iconNumber >= 119 && iconNumber <= 138)
            return DEFAULT_ICON_NUMBER;
        if (iconNumber >= 117 && iconNumber <= 118)
            return 65;
        if (iconNumber == 139)
            return DEFAULT_ICON_NUMBER;
        return iconNumber;
    }

    // --- waypoint classes (gt_waypt_classes_e) -----------------------------------------

    public static final int WAYPOINT_CLASS_USER_WAYPOINT = 0;
    public static final int WAYPOINT_CLASS_MAP_POINT = 8;
    /** {@code GDB_DEF_CLASS} in gdb.cc: the class of a visible waypoint */
    public static final int GDB_DEFAULT_CLASS = WAYPOINT_CLASS_USER_WAYPOINT;
    /** {@code GDB_DEF_HIDDEN_CLASS} in gdb.cc: the class of a waypoint that only exists in a route */
    public static final int GDB_DEFAULT_HIDDEN_CLASS = WAYPOINT_CLASS_MAP_POINT;

    /** the waypoint class the GDB writer uses for a waypoint without Garmin data */
    public static int gdbWaypointClass(boolean routePoint) {
        return routePoint ? GDB_DEFAULT_HIDDEN_CLASS : GDB_DEFAULT_CLASS;
    }

    // --- display modes -----------------------------------------------------------------

    /** {@code gt_gdb_display_modes_e}: the order in .gdb files */
    public static final int GDB_DISPLAY_MODE_SYMBOL = 0;
    public static final int GDB_DISPLAY_MODE_SYMBOL_AND_NAME = 1;
    public static final int GDB_DISPLAY_MODE_SYMBOL_AND_COMMENT = 2;

    /**
     * The display mode the GDB writer stores for a waypoint without Garmin data
     * ({@code get_display() == -1} in {@code GdbFormat::write_waypoint_cb()}):
     * symbol and name below the map point class, otherwise symbol only.
     */
    public static int gdbWaypointDisplayMode(int waypointClass) {
        return waypointClass < WAYPOINT_CLASS_MAP_POINT ? GDB_DISPLAY_MODE_SYMBOL_AND_NAME : GDB_DISPLAY_MODE_SYMBOL;
    }

    /** the waypoint colour the GDB writer stores: always 0 ("color" in {@code GdbFormat::write_waypoint()}) */
    public static final int GDB_WAYPOINT_COLOR = 0;

    /** {@code display} in {@code mps_waypoint_w()}: show the waypoint with its name */
    public static final int MAPSOURCE_WAYPOINT_DISPLAY = 1;
    /** {@code colour} in {@code mps_waypoint_w()}: unknown colour (black is 1, white is 16) */
    public static final int MAPSOURCE_WAYPOINT_COLOUR = 0;
    /** {@code colour} in {@code mps_trackhdr_w()}: unknown colour */
    public static final int MAPSOURCE_TRACK_COLOUR = 0;

    // --- colours (gt_colors) -----------------------------------------------------------

    /** {@code unknown_color} in defs.h: no colour set */
    public static final int UNKNOWN_COLOR = -1;

    private static final String[] COLOR_NAMES = {
            "Unknown", "Black", "DarkRed", "DarkGreen", "DarkYellow", "DarkBlue", "DarkMagenta", "DarkCyan",
            "LightGray", "DarkGray", "Red", "Green", "Yellow", "Blue", "Magenta", "Cyan", "White", "Transparent"
    };
    /** BGR values ({@code bbggrr}) like GPSBabel stores them */
    private static final int[] COLOR_VALUES = {
            UNKNOWN_COLOR, 0x000000, 0x00008B, 0x006400, 0x008B8B, 0x8B0000, 0x8B008B, 0x8B8B00,
            0xD3D3D3, 0xA9A9A9, 0x0000FF, 0x008000, 0x00FFFF, 0xFF0000, 0xFF00FF, 0xFFFF00, 0xFFFFFF, UNKNOWN_COLOR
    };

    public static final int COLOR_INDEX_UNKNOWN = 0;
    public static final int COLOR_INDEX_MAGENTA = 14;

    /** {@code gt_color_value()}: the BGR value of a Garmin colour index, {@link #UNKNOWN_COLOR} if out of range */
    public static int colorValue(long garminIndex) {
        return garminIndex >= 0 && garminIndex < COLOR_VALUES.length ? COLOR_VALUES[(int) garminIndex] : UNKNOWN_COLOR;
    }

    /** {@code gt_color_name()}: the name of a Garmin colour index, "Unknown" if out of range */
    public static String colorName(long garminIndex) {
        return garminIndex >= 0 && garminIndex < COLOR_NAMES.length ? COLOR_NAMES[(int) garminIndex] : COLOR_NAMES[0];
    }

    /** {@code gt_color_index_by_rgb()}: the first Garmin colour index with this BGR value, 0 if none */
    public static int colorIndexByRgb(int bgr) {
        for (int i = 0; i < COLOR_VALUES.length; i++) {
            if (COLOR_VALUES[i] == bgr)
                return i;
        }
        return COLOR_INDEX_UNKNOWN;
    }

    /**
     * The route colour index the GDB writer stores ({@code GdbFormat::write_route()}):
     * Magenta for a route without a colour, which is every RouteConverter route.
     */
    public static int gdbRouteColorIndex(int bgr) {
        return bgr < 0 ? COLOR_INDEX_MAGENTA : colorIndexByRgb(bgr);
    }

    /**
     * The track colour index the GDB writer stores ({@code GdbFormat::write_track()}):
     * "Unknown" (0) for a track without a colour, which is every RouteConverter track.
     */
    public static int gdbTrackColorIndex(int bgr) {
        return colorIndexByRgb(bgr);
    }
}
