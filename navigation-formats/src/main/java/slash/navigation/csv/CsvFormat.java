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
package slash.navigation.csv;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;

import java.io.*;
import java.util.*;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static slash.common.io.InputOutput.readAsReaderPreferringUtf8;
import static slash.common.io.Transfer.*;
import static slash.navigation.csv.ColumnType.Coordinates;
import static slash.navigation.csv.ColumnType.Unsupported;

/**
 * The base of all CSV formats.
 *
 * @author Christian Pesch
 */

public abstract class CsvFormat extends BaseNavigationFormat<CsvRoute> {

    public String getExtension() {
        return ".csv";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> CsvRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new CsvRoute(this, name, (List<CsvPosition>) positions);
    }

    protected abstract char getColumnSeparator();

    protected LinkedHashMap<String, String> transformRead(LinkedHashMap<String, String> rowAsMap) {
        return rowAsMap;
    }

    protected Map<String, String> transformWrite(Map<String, String> rowAsMap) {
        return rowAsMap;
    }

    public void read(InputStream source, ParserContext<CsvRoute> context) throws IOException {
        try (Reader reader = readAsReaderPreferringUtf8(source)) {
            if (!read(new BufferedReader(reader), context))
                throw new IllegalArgumentException(format("Format %s cannot find positions; exiting", getName()));
        }
    }

    private static final int HEADER_SEARCH_ROW_LIMIT = 10;
    // generous lookahead for mark()/reset(): only the title/header rows actually get buffered
    private static final int HEADER_SEARCH_MARK_LIMIT = 1 << 20;

    protected boolean read(Reader reader, ParserContext<CsvRoute> context) throws IOException {
        List<CsvPosition> positions = new ArrayList<>();

        BufferedReader bufferedReader = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
        skipToHeaderLine(bufferedReader);

        CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnSeparator(getColumnSeparator());
        ObjectReader objectReader = new CsvMapper().readerFor(LinkedHashMap.class).with(schema);
        MappingIterator<LinkedHashMap<String, String>> iterator = objectReader.readValues(bufferedReader);
        while (iterator.hasNext()) {
            LinkedHashMap<String, String> rowAsMap = iterator.next();
            CsvPosition position = new CsvPosition(transformRead(rowAsMap));

            // skip positions without any reasonable data to make format less greedy
            if (position.getLongitude() == null && position.getLatitude() == null && position.getDescription() == null)
                continue;

            positions.add(position);
        }

        if (!positions.isEmpty()) {
            context.appendRoute(new CsvRoute(this, null, positions));
            return true;
        } else
            return false;
    }

    // finds the header line among the first lines, skipping an optional title row above it, then
    // rewinds so the rest of the stream reaches the CSV parser unmodified (quoting, embedded
    // newlines in quoted fields) instead of being rejoined from naively split lines
    private void skipToHeaderLine(BufferedReader reader) throws IOException {
        reader.mark(HEADER_SEARCH_MARK_LIMIT);

        int headerLineIndex = 0;
        String line;
        for (int i = 0; i < HEADER_SEARCH_ROW_LIMIT && (line = reader.readLine()) != null; i++) {
            if (countKnownColumns(line) >= 2) {
                headerLineIndex = i;
                break;
            }
        }

        reader.reset();
        for (int i = 0; i < headerLineIndex; i++)
            reader.readLine();
    }

    private int countKnownColumns(String line) {
        List<String> cells = splitRespectingQuotes(line, getColumnSeparator());
        int count = 0;
        for (String cell : cells) {
            ColumnType columnType = ColumnType.classify(trim(cell));
            if (columnType == Coordinates)
                count += 2;
            else if (columnType != Unsupported)
                count++;
        }
        return count;
    }

    // a minimal quote-aware split, just for classifying a single title/header line;
    // the CSV parser itself handles the full dialect (escaping, embedded newlines) once reading resumes
    private List<String> splitRespectingQuotes(String line, char separator) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"')
                inQuotes = !inQuotes;
            else if (c == separator && !inQuotes) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else
                cell.append(c);
        }
        cells.add(cell.toString());
        return cells;
    }

    private Set<String> collectKeys(List<CsvPosition> positions) {
        Set<String> result = new LinkedHashSet<>();
        for (CsvPosition position : positions) {
            Set<String> keys = transformWrite(position.getRowAsMap()).keySet();
            result.addAll(keys);
        }
        return result;
    }

    public void write(CsvRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        List<CsvPosition> positions = route.getPositions();

        CsvSchema.Builder builder = new CsvSchema.Builder();
        for (String key : collectKeys(positions))
            builder = builder.addColumn(key);

        CsvSchema schema = builder.build().withHeader().withColumnSeparator(getColumnSeparator());
        try(SequenceWriter writer = new CsvMapper().writer(schema).writeValues(target)) {
            for (int i = startIndex; i < endIndex; i++) {
                CsvPosition position = positions.get(i);
                writer.write(transformWrite(position.getRowAsMap()));
            }
        }
    }
}
