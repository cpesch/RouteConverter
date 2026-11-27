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

package slash.navigation.base;

import slash.navigation.common.NavigationPosition;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * The base of native replacements for the GPSBabel-based formats (spec 00083): reuses
 * {@code slash.navigation.babel.BabelFormat}'s GPX-exchange semantics without requiring the
 * GPSBabel executable.
 *
 * @author Christian Pesch
 */

public abstract class GpxBackedNavigationFormat extends BaseNavigationFormat<GpxRoute> implements GpxBackedFormat {
    private static final int TAB = 0x9;
    private static final int SPACE = 0x20;
    private static final int LF = 0xA;
    private static final int CR = 0xD;

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> GpxRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new GpxRoute(new Gpx10Format(), characteristics, name, null, (List<GpxPosition>) positions);
    }

    protected List<RouteCharacteristics> getWriteCharacteristics() {
        return asList(Route, Track, Waypoints);
    }

    protected boolean isValidRoute(GpxRoute route) {
        return true;
    }

    protected GpxRoute sanitizeRouteAfterReading(GpxRoute route) {
        return route;
    }

    protected abstract List<GpxRoute> readRoutes(InputStream source) throws IOException;

    private List<GpxRoute> filterValidRoutes(List<GpxRoute> routes) {
        if (routes == null)
            return null;

        List<GpxRoute> result = new ArrayList<>();
        for (GpxRoute aRoute : routes) {
            GpxRoute route = sanitizeRouteAfterReading(aRoute);
            if (isValidRoute(route))
                result.add(route);
        }
        return !result.isEmpty() ? result : null;
    }

    public final void read(InputStream source, ParserContext<GpxRoute> context) throws IOException {
        List<GpxRoute> result = filterValidRoutes(readRoutes(source));
        if (result != null && !result.isEmpty())
            context.appendRoutes(result);
    }

    /**
     * Whether a character may appear in an XML 1.0 document: #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD].
     * Surrogates are not legal on their own, a valid pair is handled by {@link #stripIllegalXmlCharacters(String)}.
     */
    public static boolean isLegalXmlCharacter(char character) {
        return character == TAB || character == LF || character == CR ||
                (character >= SPACE && character <= 0xD7FF) ||
                (character >= 0xE000 && character <= 0xFFFD);
    }

    /**
     * Replaces every character that is illegal in XML 1.0 - NUL and the other C0 controls except TAB, LF and CR,
     * U+FFFE, U+FFFF and unpaired surrogates - by a space.
     */
    public static String stripIllegalXmlCharacters(String string) {
        if (string == null)
            return null;

        StringBuilder result = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); i++) {
            char character = string.charAt(i);
            if (Character.isHighSurrogate(character) && i + 1 < string.length() &&
                    Character.isLowSurrogate(string.charAt(i + 1))) {
                result.append(character).append(string.charAt(++i));
                continue;
            }
            result.append(isLegalXmlCharacter(character) ? character : SPACE);
        }
        return result.toString();
    }
}
