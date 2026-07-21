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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

import static java.nio.charset.CodingErrorAction.REPORT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.copyLarge;

/**
 * As a pipe reads from input and writes to output.
 *
 * @author Christian Pesch
 */

public class InputOutput {
    public static final int DEFAULT_BUFFER_SIZE = 4 * 1024;

    public static void copyAndClose(InputStream inputStream, OutputStream outputStream) throws IOException {
        try (InputStream i = inputStream; OutputStream o = outputStream){
            copyLarge(i, o, new byte[DEFAULT_BUFFER_SIZE]);
        }
    }

    public static void copyAndClose(Reader reader, Writer writer) throws IOException {
        try (Reader r = reader; Writer w = writer){
            copyLarge(r, w, new char[DEFAULT_BUFFER_SIZE]);
        }
    }

    public static byte[] readBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copyAndClose(input, output);
        return output.toByteArray();
    }

    public static String readFileToString(File file) throws IOException {
        return new String(readBytes(new FileInputStream(file)), UTF_8);
    }

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    /**
     * Returns a {@link Reader} for legacy/garbled XML that carries no encoding declaration: decodes as
     * UTF-8 when the bytes are valid UTF-8, otherwise falls back to Windows-1252 (the common legacy code
     * page). Deterministic and independent of the platform default charset, which changed to UTF-8 in
     * JDK 18 (JEP 400) and previously made this depend on the host locale.
     */
    public static Reader readAsReaderPreferringUtf8(InputStream input) throws IOException {
        byte[] bytes = readBytes(input);
        return new StringReader(new String(bytes, isValidUtf8(bytes) ? UTF_8 : WINDOWS_1252));
    }

    private static boolean isValidUtf8(byte[] bytes) {
        try {
            UTF_8.newDecoder().onMalformedInput(REPORT).onUnmappableCharacter(REPORT).decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }
}
