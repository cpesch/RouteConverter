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

import org.junit.jupiter.api.Test;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.navigation.base.RouteCharacteristics.Route;

public class GpxBackedNavigationFormatTest {
    private static class TestFormat extends GpxBackedNavigationFormat {
        private final List<GpxRoute> toRead;
        private Function<GpxRoute, GpxRoute> sanitizer = route -> route;
        private Predicate<GpxRoute> validator = route -> true;

        private TestFormat(List<GpxRoute> toRead) {
            this.toRead = toRead;
        }

        protected List<GpxRoute> readRoutes(InputStream source) {
            return toRead;
        }

        protected GpxRoute sanitizeRouteAfterReading(GpxRoute route) {
            return sanitizer.apply(route);
        }

        protected boolean isValidRoute(GpxRoute route) {
            return validator.test(route);
        }

        public String getName() {
            return "Test";
        }

        public String getExtension() {
            return ".test";
        }

        public boolean isSupportsMultipleRoutes() {
            return false;
        }

        public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) {
            throw new UnsupportedOperationException();
        }
    }

    private static GpxRoute createRoute(String name) {
        return new GpxRoute(new Gpx10Format(), Route, name, null, emptyList());
    }

    private static ParserContextImpl<GpxRoute> read(TestFormat format) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(new byte[0]), context);
        return context;
    }

    @Test
    public void emptyResultAppendsNothing() throws IOException {
        TestFormat format = new TestFormat(emptyList());
        assertTrue(read(format).getRoutes().isEmpty());
    }

    @Test
    public void nullResultAppendsNothing() throws IOException {
        TestFormat format = new TestFormat(null);
        assertTrue(read(format).getRoutes().isEmpty());
    }

    @Test
    public void invalidRoutesAreDropped() throws IOException {
        TestFormat format = new TestFormat(singletonList(createRoute("original")));
        format.validator = route -> false;
        assertTrue(read(format).getRoutes().isEmpty());
    }

    @Test
    public void sanitizeIsAppliedBeforeFilter() throws IOException {
        TestFormat format = new TestFormat(singletonList(createRoute("original")));
        format.sanitizer = route -> {
            route.setName("sanitized");
            return route;
        };
        format.validator = route -> "sanitized".equals(route.getName());

        List<GpxRoute> routes = read(format).getRoutes();
        assertEquals(1, routes.size());
        assertEquals("sanitized", routes.get(0).getName());
    }

    @Test
    public void stripIllegalXmlCharactersRemovesControlCharactersButKeepsLineBreaks() {
        assertEquals("abc\n\rd", GpxBackedNavigationFormat.stripIllegalXmlCharacters("a\u0001b\u000Bc\n\rd"));
    }

    @Test
    public void stripIllegalXmlCharactersIsNullSafe() {
        assertNull(GpxBackedNavigationFormat.stripIllegalXmlCharacters(null));
    }
}
