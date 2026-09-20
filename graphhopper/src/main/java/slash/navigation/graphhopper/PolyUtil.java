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
package slash.navigation.graphhopper;

import slash.navigation.common.NavigationPosition;
import slash.navigation.common.Polygon;
import slash.navigation.common.SimpleNavigationPosition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Parses Osmosis polygon filter files, as published by Geofabrik next to each extract.
 *
 * @author Christian Pesch
 */

class PolyUtil {
    private static final String END = "END";

    private PolyUtil() {
    }

    /**
     * Grammar: a name line that is ignored, then rings of an id line (a leading "!" marks a hole),
     * "longitude latitude" lines and END, and a final END closing the file.
     *
     * @return null for an empty, truncated or malformed file
     */
    static Polygon parse(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8))) {
            if (reader.readLine() == null)
                return null;

            List<List<NavigationPosition>> rings = new ArrayList<>();
            List<Boolean> holes = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                if (line.equals(END))
                    return rings.isEmpty() ? null : Polygon.of(rings, holes);

                List<NavigationPosition> ring = parseRing(reader);
                if (ring == null)
                    return null;
                rings.add(ring);
                holes.add(line.startsWith("!"));
            }
            // no terminating END
            return null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static List<NavigationPosition> parseRing(BufferedReader reader) throws IOException {
        List<NavigationPosition> result = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty())
                continue;
            if (line.equals(END))
                return result.size() >= 3 ? result : null;

            String[] columns = line.split("\\s+");
            if (columns.length != 2)
                return null;
            // longitude first, then latitude
            double longitude = Double.parseDouble(columns[0]);
            double latitude = Double.parseDouble(columns[1]);
            if (!Double.isFinite(longitude) || !Double.isFinite(latitude))
                return null;
            result.add(new SimpleNavigationPosition(longitude, latitude));
        }
        // ring without END
        return null;
    }
}
