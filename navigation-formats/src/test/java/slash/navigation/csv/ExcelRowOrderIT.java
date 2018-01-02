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

import org.junit.Test;
import slash.navigation.base.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.io.File.createTempFile;
import static junit.framework.TestCase.assertEquals;
import static slash.common.io.Files.getExtension;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.NavigationTestCase.calendar;

public class ExcelRowOrderIT {
    private NavigationFormatParser parser = new NavigationFormatParser(new NavigationFormatRegistry());
    private static final File SOURCE = new File(TEST_PATH + "from-order.xls");

    private BaseRoute<BaseNavigationPosition, BaseNavigationFormat> readRoute() throws IOException {
        ParserResult result = parser.read(SOURCE);
        return result.getTheRoute();
    }

    private BaseRoute<BaseNavigationPosition, BaseNavigationFormat> writeAndReadFile(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException {
        File target = createTempFile("target", getExtension(SOURCE));
        parser.write(route, route.getFormat(), target);

        List<NavigationFormat> formats = Arrays.<NavigationFormat>asList(new Excel97Format(), new Excel2008Format());
        ParserResult result = parser.read(target, formats);
        route = result.getTheRoute();
        return route;
    }

    private BaseNavigationPosition createPosition(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) {
        return route.createPosition(-1.0, -1.0, -1.0, -1.0,
                calendar(2001, 1, 1, 1, 1,1), "X");
    }

    @Test
    public void testAddFirstPosition() throws IOException {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = readRoute();

        route.add(0, createPosition(route));

        route = writeAndReadFile(route);

        assertEquals("X", route.getPosition(0).getDescription());
        assertEquals("A", route.getPosition(1).getDescription());
        assertEquals("B", route.getPosition(2).getDescription());
        assertEquals("C", route.getPosition(3).getDescription());
        assertEquals("D", route.getPosition(4).getDescription());
        assertEquals("E", route.getPosition(5).getDescription());
        assertEquals("F", route.getPosition(6).getDescription());
    }

    @Test
    public void testAddMiddlePosition() throws IOException {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = readRoute();

        route.add(3, createPosition(route));

        route = writeAndReadFile(route);

        assertEquals("X", route.getPosition(3).getDescription());
        assertEquals("A", route.getPosition(0).getDescription());
        assertEquals("B", route.getPosition(1).getDescription());
        assertEquals("C", route.getPosition(2).getDescription());
        assertEquals("D", route.getPosition(4).getDescription());
        assertEquals("E", route.getPosition(5).getDescription());
        assertEquals("F", route.getPosition(6).getDescription());
    }

    @Test
    public void testAddLastPosition() throws IOException {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = readRoute();

        route.add(route.getPositionCount(), createPosition(route));

        route = writeAndReadFile(route);

        assertEquals("X", route.getPosition(6).getDescription());
        assertEquals("A", route.getPosition(0).getDescription());
        assertEquals("B", route.getPosition(1).getDescription());
        assertEquals("C", route.getPosition(2).getDescription());
        assertEquals("D", route.getPosition(3).getDescription());
        assertEquals("E", route.getPosition(4).getDescription());
        assertEquals("F", route.getPosition(5).getDescription());
    }

    @Test
    public void testDeleteMiddlePosition() throws IOException {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = readRoute();

        route.remove(2);

        route = writeAndReadFile(route);

        assertEquals("A", route.getPosition(0).getDescription());
        assertEquals("B", route.getPosition(1).getDescription());
        assertEquals("D", route.getPosition(2).getDescription());
        assertEquals("E", route.getPosition(3).getDescription());
        assertEquals("F", route.getPosition(4).getDescription());
    }

    @Test
    public void testMoveMiddlePositionDown() throws IOException {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = readRoute();

        route.move(2, 3);

        route = writeAndReadFile(route);

        assertEquals("A", route.getPosition(0).getDescription());
        assertEquals("B", route.getPosition(1).getDescription());
        assertEquals("D", route.getPosition(2).getDescription());
        assertEquals("C", route.getPosition(3).getDescription());
        assertEquals("E", route.getPosition(4).getDescription());
        assertEquals("F", route.getPosition(5).getDescription());
    }

    @Test
    public void testMoveMiddlePositionTwoDown() throws IOException {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = readRoute();

        route.move(2, 4);

        route = writeAndReadFile(route);

        assertEquals("A", route.getPosition(0).getDescription());
        assertEquals("B", route.getPosition(1).getDescription());
        assertEquals("E", route.getPosition(2).getDescription());
        assertEquals("D", route.getPosition(3).getDescription());
        assertEquals("C", route.getPosition(4).getDescription());
        assertEquals("F", route.getPosition(5).getDescription());
    }

    @Test
    public void testMoveMiddlePositionToBottom() throws IOException {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = readRoute();

        route.bottom(2, 0);

        route = writeAndReadFile(route);

        assertEquals("A", route.getPosition(0).getDescription());
        assertEquals("B", route.getPosition(1).getDescription());
        assertEquals("D", route.getPosition(2).getDescription());
        assertEquals("E", route.getPosition(3).getDescription());
        assertEquals("F", route.getPosition(4).getDescription());
        assertEquals("C", route.getPosition(5).getDescription());
    }

    @Test
    public void testMoveMiddlePositionToBottomWithOffset() throws IOException {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = readRoute();

        route.bottom(2, 1);

        route = writeAndReadFile(route);

        assertEquals("A", route.getPosition(0).getDescription());
        assertEquals("B", route.getPosition(1).getDescription());
        assertEquals("D", route.getPosition(2).getDescription());
        assertEquals("E", route.getPosition(3).getDescription());
        assertEquals("C", route.getPosition(4).getDescription());
        assertEquals("F", route.getPosition(5).getDescription());
    }

    @Test
    public void testMoveMiddlePositionUp() throws IOException {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = readRoute();

        route.move(2, 3);

        route = writeAndReadFile(route);

        assertEquals("A", route.getPosition(0).getDescription());
        assertEquals("B", route.getPosition(1).getDescription());
        assertEquals("D", route.getPosition(2).getDescription());
        assertEquals("C", route.getPosition(3).getDescription());
        assertEquals("E", route.getPosition(4).getDescription());
        assertEquals("F", route.getPosition(5).getDescription());
    }

    @Test
    public void testMoveMiddlePositionToTop() throws IOException {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = readRoute();

        route.top(3, 0);

        route = writeAndReadFile(route);

        assertEquals("D", route.getPosition(0).getDescription());
        assertEquals("A", route.getPosition(1).getDescription());
        assertEquals("B", route.getPosition(2).getDescription());
        assertEquals("C", route.getPosition(3).getDescription());
        assertEquals("E", route.getPosition(4).getDescription());
        assertEquals("F", route.getPosition(5).getDescription());
    }

    @Test
    public void testMoveMiddlePositionToTopWithOffset() throws IOException {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = readRoute();

        route.top(3, 1);

        route = writeAndReadFile(route);

        assertEquals("A", route.getPosition(0).getDescription());
        assertEquals("D", route.getPosition(1).getDescription());
        assertEquals("B", route.getPosition(2).getDescription());
        assertEquals("C", route.getPosition(3).getDescription());
        assertEquals("E", route.getPosition(4).getDescription());
        assertEquals("F", route.getPosition(5).getDescription());
    }
}
