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

import slash.navigation.BaseNavigationPosition;
import slash.navigation.GkPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.SimpleFormat;
import slash.navigation.util.Conversion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Magic Maps Tour (.pth) files.
 *
 * @author Christian Pesch
 */

public class MagicMapsPthFormat extends SimpleFormat<MagicMapsPthRoute> {
    private static final Pattern NAME_VALUE_PATTERN = Pattern.compile("(.+?):(.+|)");
    private static final Pattern POSITION_PATTERN = Pattern.compile("\\s*([-|\\d|\\.]+)\\s+([-|\\d|\\.]+)\\s*(.*)");

    public String getExtension() {
        return ".pth";
    }

    public String getName() {
        return "Magic Maps Tour (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public <P extends BaseNavigationPosition> MagicMapsPthRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new MagicMapsPthRoute(characteristics, (List<GkPosition>) positions);
    }

    public List<MagicMapsPthRoute> read(BufferedReader reader, Calendar startDate, String encoding) throws IOException {
        List<GkPosition> positions = new ArrayList<GkPosition>();

        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (Conversion.trim(line) == null)
                continue;

            if (line.startsWith("#")) {
            } else if (isNameValue(line)) {
            } else if (isPosition(line)) {
                GkPosition position = parsePosition(line);
                positions.add(position);
            } else {
                return null;
            }
        }

        if (positions.size() > 0)
            return Arrays.asList(new MagicMapsPthRoute(this, RouteCharacteristics.Track, positions));
        else
            return null;
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
        Double right = Conversion.parseDouble(lineMatcher.group(1));
        Double height = Conversion.parseDouble(lineMatcher.group(2));
        String comment = Conversion.trim(lineMatcher.group(3));
        return new GkPosition(right, height, comment);
    }

    public void write(MagicMapsPthRoute route, PrintWriter writer, int startIndex, int endIndex, boolean numberPositionNames) {
        List<GkPosition> positions = route.getPositions();
        writer.println("# Path2D file format V1.0 - MagicMaps");
        writer.println("Pathsize: " + positions.size());
        writer.println("selectedPoint: 1");

        for (int i = startIndex; i < endIndex; i++) {
            GkPosition position = positions.get(i);
            String right = Conversion.formatDoubleAsString(position.getRight(), 2);
            String height = Conversion.formatDoubleAsString(position.getHeight(), 2);
            writer.println(right + " \t " + height);
        }
    }
}
