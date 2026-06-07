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

package slash.common.io;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

public class TokenReplacingReaderTest {

    private String read(String input, TokenResolver resolver) throws IOException {
        TokenReplacingReader reader = new TokenReplacingReader(new StringReader(input), resolver);
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            sb.append((char) ch);
        }
        reader.close();
        return sb.toString();
    }

    @Test
    public void testNoTokenPassthrough() throws IOException {
        String input = "Hello World";
        assertEquals("Hello World", read(input, name -> null));
    }

    @Test
    public void testEmptyInputPassthrough() throws IOException {
        assertEquals("", read("", name -> null));
    }

    @Test
    public void testSingleTokenReplaced() throws IOException {
        String input = "Hello ${name}!";
        String result = read(input, name -> "name".equals(name) ? "World" : null);
        assertEquals("Hello World!", result);
    }

    @Test
    public void testMultipleTokensReplaced() throws IOException {
        String input = "${greeting} ${subject}";
        String result = read(input, name -> switch (name) {
            case "greeting" -> "Hi";
            case "subject" -> "There";
            default -> null;
        });
        assertEquals("Hi There", result);
    }

    @Test
    public void testUnknownTokenLeftAsIs() throws IOException {
        String input = "value=${unknown}";
        String result = read(input, name -> null);
        assertEquals("value=${unknown}", result);
    }

    @Test
    public void testDollarSignWithoutBraceIsPassedThrough() throws IOException {
        String input = "100$ off";
        String result = read(input, name -> null);
        assertEquals("100$ off", result);
    }

    @Test
    public void testAdjacentTokens() throws IOException {
        String input = "${a}${b}";
        String result = read(input, name -> switch (name) {
            case "a" -> "X";
            case "b" -> "Y";
            default -> null;
        });
        assertEquals("XY", result);
    }

    @Test
    public void testReadArrayVariant() throws IOException {
        String input = "Hello ${name}!";
        TokenReplacingReader reader = new TokenReplacingReader(new StringReader(input), name -> "World");
        char[] buf = new char[20];
        int read = reader.read(buf, 0, buf.length);
        reader.close();
        String result = new String(buf, 0, read);
        assertEquals("Hello World!", result);
    }
}

