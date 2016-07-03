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

package slash.navigation.mm;

import slash.navigation.base.GkPosition;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleFormat;
import slash.navigation.common.NavigationPosition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.*;
import static slash.navigation.base.RouteCharacteristics.Track;

/**
 * Reads and writes MagicMaps Tour (.pth) files.
 *
 * @author Christian Pesch
 */

public class MagicMapsPthFormat extends SimpleFormat<MagicMapsPthRoute> { // TODO make this a SimpleLineBasedFormat
    private static final Pattern NAME_VALUE_PATTERN = Pattern.compile("(.+?):(.+|)");
    private static final Pattern POSITION_PATTERN = Pattern.compile("\\s*([-|\\d|\\.]+)\\s+([-|\\d|\\.]+)\\s*(.*)");

    public String getExtension() {
        return ".pth";
    }

    public String getName() {
        return "MagicMaps Tour (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> MagicMapsPthRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new MagicMapsPthRoute(characteristics, (List<GkPosition>) positions);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<MagicMapsPthRoute> context) throws IOException {
        List<GkPosition> positions = new ArrayList<>();

        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (trim(line) == null)
                continue;

            //noinspection StatementWithEmptyBody
            if (line.startsWith("#")) {
            } else //noinspection StatementWithEmptyBody
                if (isNameValue(line)) {
            } else if (isPosition(line)) {
                GkPosition position = parsePosition(line);
                positions.add(position);
            } else {
                return;
            }
        }

        if (positions.size() > 0)
            context.appendRoute(createRoute(Track, null, positions));
    }

    boolean isNameValue(String line) {
        Matcher matcher = NAME_VALUE_PATTERN.matcher(line);
        return matcher.matches();
    }

    boolean isPosition(String line) {
        Matcher matcher = POSITION_PATTERN.matcher(line);
        return matcher.matches();
    }

    GkPosition parsePosition(String line) {
        Matcher lineMatcher = POSITION_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        Double right = parseDouble(lineMatcher.group(1));
        Double height = parseDouble(lineMatcher.group(2));
        String description = trim(lineMatcher.group(3));
        return new GkPosition(right, height, description);
    }

    public void write(MagicMapsPthRoute route, PrintWriter writer, int startIndex, int endIndex) {
        List<GkPosition> positions = route.getPositions();
        writer.println("# Path2D file format V1.0 - MagicMaps");
        writer.println("Pathsize: " + positions.size());
        writer.println("selectedPoint: 1");

        for (int i = startIndex; i < endIndex; i++) {
            GkPosition position = positions.get(i);
            String right = formatDoubleAsString(position.getRight(), 2);
            String height = formatDoubleAsString(position.getHeight(), 2);
            writer.println(right + " \t " + height);
        }
    }
}
