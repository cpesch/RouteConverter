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
import slash.common.io.NotClosingUnderlyingInputStream;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.String.format;
import static slash.common.io.Transfer.*;

/**
 * The base of all CSV formats.
 *
 * @author Christian Pesch
 */

public abstract class CsvFormat extends BaseNavigationFormat<CsvRoute> {
    private static final Logger log = Logger.getLogger(CsvFormat.class.getName());

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

    public void read(InputStream source, ParserContext<CsvRoute> context) throws Exception {
        NotClosingUnderlyingInputStream buffer = new NotClosingUnderlyingInputStream(source);
        buffer.mark(source.available());
        if(!read(buffer, UTF8_ENCODING, context)) {
            buffer.reset();
            read(buffer, ISO_LATIN1_ENCODING, context);
        }
    }

    protected boolean read(InputStream source, String encoding, ParserContext<CsvRoute> context) throws IOException {
        log.info(format("Reading CSV with column separator '%c' and encoding '%s'", getColumnSeparator(), encoding));

        try (Reader reader = new InputStreamReader(source, encoding)) {
            return read(new BufferedReader(reader), context);
        }
    }

    private boolean containsGarbage(Map<String, String> map) {
        for (String key : map.keySet()) {
            if (isIsoLatin1ButReadWithUtf8(key))
                return true;
            String value = map.get(key);
            if (isIsoLatin1ButReadWithUtf8(value))
                return true;
        }
        return false;
    }

    protected boolean read(Reader reader, ParserContext<CsvRoute> context) throws IOException {
        List<CsvPosition> positions = new ArrayList<>();

        CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnSeparator(getColumnSeparator());
        ObjectReader objectReader = new CsvMapper().readerFor(LinkedHashMap.class).with(schema);
        try {
            MappingIterator<LinkedHashMap<String, String>> iterator = objectReader.readValues(reader);
            while (iterator.hasNext()) {
                LinkedHashMap<String, String> rowAsMap = iterator.next();
                if (containsGarbage(rowAsMap)) {
                    log.warning(format("Found garbage in '%s'", rowAsMap));
                    return false;
                }
                positions.add(new CsvPosition(rowAsMap));
            }
        }
        finally {
            reader.close();
        }

        if (positions.size() > 0)
            context.appendRoute(new CsvRoute(this, null, positions));
        return true;
    }

    public void write(CsvRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        List<CsvPosition> positions = route.getPositions();

        CsvSchema.Builder builder = new CsvSchema.Builder();
        if (positions.size() > 0)
            for (String key : positions.get(0).getRowAsMap().keySet())
                builder = builder.addColumn(key);

        CsvSchema schema = builder.build().withHeader().withColumnSeparator(getColumnSeparator());
        try(SequenceWriter writer = new CsvMapper().writer(schema).writeValues(target)) {
            for (int i = startIndex; i < endIndex; i++) {
                CsvPosition position = positions.get(i);
                writer.write(position.getRowAsMap());
            }
        }
    }
}
