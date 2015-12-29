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
package slash.navigation.converter.tools;

import java.io.*;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * {@link OrderedResourceBundle} but ordered for {@link FilterResourceBundles}.
 *
 * @author Christian Pesch
 */

public class OrderedResourceBundle extends ResourceBundle {
    private OrderedProperties properties;

    public OrderedResourceBundle(InputStream stream) throws IOException {
        properties = new OrderedProperties();
        properties.load(stream);
    }

    protected Object handleGetObject(String key) {
        return properties.get(key);
    }

    public Enumeration<String> getKeys() {
        throw new UnsupportedOperationException();
    }

    public Set<String> getOrderedKeys() {
        return properties.getKeys();
    }

    public Object handleRemoteObject(String key) {
        return properties.remove(key);
    }

    public void store(OutputStream stream) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(stream, "8859_1"));
        for (String key : getOrderedKeys()) {
            String value = saveConvert(handleGetObject(key).toString(), false, false);
            writer.println(key + "=" + value);
        }
        writer.close();
    }

    private String saveConvert(String theString,
                               boolean escapeSpace,
                               boolean escapeUnicode) {
        int len = theString.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuffer outBuffer = new StringBuffer(bufLen);

        for(int x=0; x<len; x++) {
            char aChar = theString.charAt(x);
            // Handle common case first, selecting largest block that
            // avoids the specials below
            if ((aChar > 61) && (aChar < 127)) {
                if (aChar == '\\') {
                    outBuffer.append('\\'); outBuffer.append('\\');
                    continue;
                }
                outBuffer.append(aChar);
                continue;
            }
            switch(aChar) {
                case ' ':
                    if (x == 0 || escapeSpace)
                        outBuffer.append('\\');
                    outBuffer.append(' ');
                    break;
                case '\t':outBuffer.append('\\'); outBuffer.append('t');
                    break;
                case '\n':outBuffer.append('\\'); outBuffer.append('n');
                    break;
                case '\r':outBuffer.append('\\'); outBuffer.append('r');
                    break;
                case '\f':outBuffer.append('\\'); outBuffer.append('f');
                    break;
                default:
                    if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode ) {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(toHex((aChar >> 12) & 0xF));
                        outBuffer.append(toHex((aChar >>  8) & 0xF));
                        outBuffer.append(toHex((aChar >>  4) & 0xF));
                        outBuffer.append(toHex( aChar        & 0xF));
                    } else {
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }

    private static char toHex(int nibble) {
        return hexDigit[(nibble & 0xF)];
    }

    /** A table of hex digits */
    private static final char[] hexDigit = {
            '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };
}
