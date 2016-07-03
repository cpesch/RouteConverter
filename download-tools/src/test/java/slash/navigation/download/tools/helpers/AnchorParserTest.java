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
package slash.navigation.download.tools.helpers;

import org.junit.Test;

import java.io.IOException;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class AnchorParserTest {
    private AnchorParser parser = new AnchorParser();

    @Test
    public void testParseSingleAnchor() throws IOException {
        assertEquals(asList("", "link"), parser.parseAnchors("bla<a href=''><a/>" +
                "bla<a id='a' href=\"link\" download=\"link\">text</a>bla"));
    }

    @Test
    public void testParseMultipleAnchors() throws IOException {
        assertEquals(asList("http://url1", "http://url2"), parser.parseAnchors("bla<a href=\"http://url1\">text</a>blabla<a href=\"http://url2\">text</a>bla"));
    }
}
