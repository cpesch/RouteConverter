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

package slash.common.type;

import org.apache.commons.codec.DecoderException;

import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.apache.commons.codec.binary.Hex.encodeHex;

/**
 * Helps to encode bytes to hexadecimal encoded numbers and back.
 *
 * @author Christian Pesch
 */

public class HexadecimalNumber {
    public static String encodeByte(byte aByte) {
        return encodeBytes(new byte[]{aByte});
    }

    public static String encodeBytes(byte[] bytes) {
        return new String(encodeHex(bytes)).toUpperCase();
    }

    public static byte[] decodeBytes(String string) {
        try {
            return decodeHex(string.toCharArray());
        } catch (DecoderException e) {
            throw new IllegalArgumentException("Not an even number of hex digits: " + string);
        }
    }

    public static String encodeInt(int integer) {
        // with Java 8 use Integer#toUnsignedString
        return Integer.toHexString(integer);
    }

    public static int decodeInt(String string) {
        // with Java 8 use Integer#parseUnsignedInt
        return (int) Long.parseLong(string, 16);
    }
}
