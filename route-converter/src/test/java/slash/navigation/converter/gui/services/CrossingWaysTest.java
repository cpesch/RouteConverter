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

package slash.navigation.converter.gui.services;

import junit.framework.TestCase;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class CrossingWaysTest extends TestCase {
    public void testExtractResult() throws ParserConfigurationException, IOException, SAXException {
        String result = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<string xmlns=\"http://www.crossingways.com/\">Track saved! Have a nice Day!</string>";
        String extracted = new CrossingWays().extractResult(result);
        assertEquals("Track saved! Have a nice Day!", extracted);
    }
}

