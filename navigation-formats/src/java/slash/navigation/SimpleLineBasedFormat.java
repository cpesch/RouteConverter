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
package slash.navigation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Calendar;

/**
 * Represents simple line based text route formats.
 *
 * @author Christian Pesch
 */

public abstract class SimpleLineBasedFormat<R extends SimpleRoute> extends SimpleFormat<R> {

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public List<R> read(BufferedReader reader, Calendar startDate, String encoding) throws IOException {
        List<Wgs84Position> positions = new ArrayList<Wgs84Position>();

        int lineCount = 0;
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (line.length() == 0)
                continue;

            if (isValidLine(line)) {
                if (isPosition(line)) {
                    Wgs84Position position = parsePosition(line, startDate);
                    positions.add(position);
                }
            } else {
                if (lineCount++ > getGarbleCount())
                    return null;
            }
        }

        if (shouldCreateRoute(positions))
            return Arrays.asList(createRoute(getRouteCharacteristics(), positions));
        else
            return null;
    }

    protected int getGarbleCount() {
        return 0;
    }

    protected boolean shouldCreateRoute(List<Wgs84Position> positions) {
        return positions.size() > 0;
    }

    protected R createRoute(RouteCharacteristics characteristics, List<Wgs84Position> positions) {
        return (R)new Wgs84Route(this, characteristics, positions);
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return RouteCharacteristics.Waypoints;
    }

    protected boolean isValidLine(String line) {
        return isPosition(line);
    }
    protected abstract boolean isPosition(String line);
    protected abstract Wgs84Position parsePosition(String line, Calendar startDate);


    public void write(R route, PrintWriter writer, int startIndex, int endIndex, boolean numberPositionNames) {
        List<Wgs84Position> positions = route.getPositions();
        writeHeader(writer);
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            writePosition(position, writer, i, i == startIndex);
        }
    }

    protected void writeHeader(PrintWriter writer) {
    }

    protected abstract void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition);

}
