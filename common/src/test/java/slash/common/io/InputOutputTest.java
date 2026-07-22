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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Tests for {@link InputOutput}.
 *
 * @author Christian Pesch
 */

public class InputOutputTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testDefaultBufferSize() {
        assertEquals(4096, InputOutput.DEFAULT_BUFFER_SIZE);
    }

    @Test
    public void testCopyAndCloseStreams() throws IOException {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputOutput.copyAndClose(in, out);
        assertArrayEquals(data, out.toByteArray());
    }

    @Test
    public void testCopyAndCloseEmptyStream() throws IOException {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputOutput.copyAndClose(in, out);
        assertEquals(0, out.toByteArray().length);
    }

    @Test
    public void testCopyAndCloseReaderWriter() throws IOException {
        String text = "hello reader";
        Reader reader = new StringReader(text);
        StringWriter writer = new StringWriter();
        InputOutput.copyAndClose(reader, writer);
        assertEquals(text, writer.toString());
    }

    @Test
    public void testReadBytes() throws IOException {
        byte[] data = {10, 20, 30, 40};
        InputStream in = new ByteArrayInputStream(data);
        byte[] result = InputOutput.readBytes(in);
        assertArrayEquals(data, result);
    }

    @Test
    public void testReadBytesEmpty() throws IOException {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        byte[] result = InputOutput.readBytes(in);
        assertEquals(0, result.length);
    }

    @Test
    public void testReadFileToString() throws IOException {
        File file = temporaryFolder.newFile("test.txt");
        String content = "file content line";
        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
            fw.write(content);
        }
        String result = InputOutput.readFileToString(file);
        assertEquals(content, result);
    }

    @Test
    public void testReadFileToStringUtf8() throws IOException {
        File file = temporaryFolder.newFile("utf8.txt");
        String content = "Zürich ? München";
        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
            fw.write(content);
        }
        String result = InputOutput.readFileToString(file);
        assertEquals(content, result);
    }

    private static String toString(Reader reader) throws IOException {
        StringWriter writer = new StringWriter();
        InputOutput.copyAndClose(reader, writer);
        return writer.toString();
    }

    @Test
    public void testReadAsReaderPreferringUtf8DecodesUtf8() throws IOException {
        String text = "Grün Ö Ä ß Straße";
        Reader reader = InputOutput.readAsReaderPreferringUtf8(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        assertEquals(text, toString(reader));
    }

    @Test
    public void testReadAsReaderPreferringUtf8FallsBackToWindows1252() throws IOException {
        String text = "Grün Ö Ä ß Straße";
        byte[] ansi = text.getBytes(Charset.forName("windows-1252")); // not valid UTF-8
        Reader reader = InputOutput.readAsReaderPreferringUtf8(new ByteArrayInputStream(ansi));
        assertEquals(text, toString(reader));
    }

    @Test
    public void testReadAsReaderPreferringUtf8PlainAscii() throws IOException {
        String text = "plain ascii 123";
        Reader reader = InputOutput.readAsReaderPreferringUtf8(new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII)));
        assertEquals(text, toString(reader));
    }
}

