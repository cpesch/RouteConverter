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

package slash.navigation.hex;

import slash.navigation.TestCase;

public class HexDecoderTest extends TestCase {

    public void testParseStringAsBytes() {
        assertByteArrayEquals(new byte[]{0x7f, -1, 0x00, 0x55}, HexDecoder.decodeBytes("7FFF0055"));
        assertByteArrayEquals(new byte[]{-1, -1, 0x00, -1},     HexDecoder.decodeBytes("FFFF00FF"));
    }
}
