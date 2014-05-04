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

import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BaseUrlParsingFormatTest {
    private BaseUrlParsingFormat urlFormat = new BaseUrlParsingFormatImpl();

    @Test
    public void testParseSingleURLParameters() {
        Map<String, List<String>> parameters = urlFormat.parseURLParameters("f=d", "ISO-8859-1");
        assertNotNull(parameters);
        List<String> values = parameters.get("f");
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals("d", values.get(0));
    }

    @Test
    public void testParseTwoURLParameters() {
        Map<String, List<String>> parameters = urlFormat.parseURLParameters("f=d&g=e", "UTF-8");
        assertNotNull(parameters);
        List<String> fValues = parameters.get("f");
        assertNotNull(fValues);
        assertEquals(1, fValues.size());
        assertEquals("d", fValues.get(0));
        List<String> gValues = parameters.get("g");
        assertNotNull(gValues);
        assertEquals(1, gValues.size());
        assertEquals("e", gValues.get(0));
    }

    @Test
    public void testParseSetURLParameters() {
        Map<String, List<String>> parameters = urlFormat.parseURLParameters("f=d&f=e", "ISO-8859-1");
        assertNotNull(parameters);
        List<String> fValues = parameters.get("f");
        assertNotNull(fValues);
        assertEquals(2, fValues.size());
        assertEquals("d", fValues.get(0));
        assertEquals("e", fValues.get(1));
    }

    private static class BaseUrlParsingFormatImpl extends BaseUrlParsingFormat {
        protected String findURL(String text) {
            throw new UnsupportedOperationException();
        }

        protected List<Wgs84Position> parsePositions(Map<String, List<String>> parameters) {
            throw new UnsupportedOperationException();
        }

        public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) throws IOException {
            throw new UnsupportedOperationException();
        }

        public String getName() {
            throw new UnsupportedOperationException();
        }

        public String getExtension() {
            throw new UnsupportedOperationException();
        }

        public int getMaximumPositionCount() {
            throw new UnsupportedOperationException();
        }
    }
}
