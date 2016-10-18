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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Represents simple line based text route formats.
 *
 * @author Christian Pesch
 */

public abstract class SimpleLineBasedFormat<R extends SimpleRoute> extends SimpleFormat<R> {

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    @SuppressWarnings("unchecked")
    protected R createRoute(RouteCharacteristics characteristics, List<Wgs84Position> positions) {
        return (R)new Wgs84Route(this, characteristics, positions);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<R> context) throws IOException {
        List<Wgs84Position> positions = new ArrayList<>();

        int lineCount = 0;
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (line.length() == 0)
                continue;

            if (isValidLine(line)) {
                if (isPosition(line)) {
                    Wgs84Position position = parsePosition(line, context);
                    positions.add(position);
                }
            } else {
                if (lineCount++ > getGarbleCount())
                    throw new IOException(format("Too much garble for %s: %d > %d lines", getName(), lineCount, getGarbleCount()));
            }
        }

        if (positions.size() > 0)
            context.appendRoute(createRoute(getRouteCharacteristics(), positions));
    }

    protected int getGarbleCount() {
        return 0;
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return Waypoints;
    }

    protected boolean isValidLine(String line) {
        return isPosition(line);
    }
    protected abstract boolean isPosition(String line);
    protected abstract Wgs84Position parsePosition(String line, ParserContext context);


    @SuppressWarnings("unchecked")
    public void write(R route, PrintWriter writer, int startIndex, int endIndex) {
        List<Wgs84Position> positions = route.getPositions();
        writeHeader(writer, route);
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            writePosition(position, writer, i, i == startIndex);
        }
        writeFooter(writer, endIndex - startIndex);
    }

    protected void writeHeader(PrintWriter writer, R route) {
    }

    protected abstract void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition);

    protected void writeFooter(PrintWriter writer, int positionCount) {
    }
}
