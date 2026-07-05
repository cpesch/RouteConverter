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

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Test;
import slash.navigation.columbus.ColumbusGpsType1Format;
import slash.navigation.gpx.Gpx11Format;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link NavigationFormatParser} read/write paths that do not
 * need external sample files (those live in {@link NavigationFormatParserIT}):
 * reading from a string, an input stream, a file (small, large, and by
 * extension), remote URLs (large bodies, chunked responses, error responses),
 * writing and reading back, and the static file-count helper.
 *
 * @author Christian Pesch
 */
public class NavigationFormatParserTest {
    private final NavigationFormatParser parser = new NavigationFormatParser(new NavigationFormatRegistry());
    private final List<File> temporaryFiles = new ArrayList<>();

    private static final String GPX_11 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<gpx version=\"1.1\" creator=\"test\" xmlns=\"http://www.topografix.com/GPX/1/1\">" +
            "<wpt lat=\"1.0\" lon=\"2.0\"><name>a</name></wpt></gpx>";

    private static final String COLUMBUS_HEADER = "INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX\r\n";

    @After
    public void tearDown() {
        for (File file : temporaryFiles)
            //noinspection ResultOfMethodCallIgnored
            file.delete();
    }

    // a Columbus GPS Type 1 body with the given number of track rows
    private static byte[] columbusBody(int rows) {
        StringBuilder builder = new StringBuilder(COLUMBUS_HEADER);
        for (int i = 1; i <= rows; i++)
            builder.append(i).append(",T,181129,120000,20.956431N,085.108008E,20,0,0,3D,SPS ,1.8,1.5,0.9,\r\n");
        return builder.toString().getBytes(US_ASCII);
    }

    private File temporaryFile(String suffix, byte[] content) throws IOException {
        File file = File.createTempFile("parser-test", suffix);
        temporaryFiles.add(file);
        Files.write(file.toPath(), content);
        return file;
    }

    @Test
    public void testReadFromString() throws IOException {
        ParserResult result = parser.read(GPX_11);
        assertTrue(result.isSuccessful());
        assertEquals(Gpx11Format.class, result.getFormat().getClass());
        assertEquals(1, result.getTheRoute().getPositionCount());
    }

    @Test
    public void testReadFromInputStream() throws IOException {
        ParserResult result = parser.read(new ByteArrayInputStream(GPX_11.getBytes(UTF_8)));
        assertTrue(result.isSuccessful());
        assertEquals(Gpx11Format.class, result.getFormat().getClass());
    }

    @Test
    public void testReadFromFile() throws IOException {
        ParserResult result = parser.read(temporaryFile(".gpx", GPX_11.getBytes(UTF_8)));
        assertTrue(result.isSuccessful());
        assertEquals(Gpx11Format.class, result.getFormat().getClass());
        assertEquals(1, result.getTheRoute().getPositionCount());
    }

    @Test
    public void testReadColumbusFromFileByExtension() throws IOException {
        ParserResult result = parser.read(temporaryFile(".csv", columbusBody(5)));
        assertTrue(result.isSuccessful());
        assertEquals(ColumbusGpsType1Format.class, result.getFormat().getClass());
        assertEquals(5, result.getTheRoute().getPositionCount());
    }

    @Test
    public void testReadLargeFileFromDisk() throws IOException {
        byte[] body = columbusBody(20000);
        assertTrue(body.length > NavigationFormatParser.TOTAL_BUFFER_SIZE);
        ParserResult result = parser.read(temporaryFile(".csv", body));
        assertTrue(result.isSuccessful());
        assertEquals(ColumbusGpsType1Format.class, result.getFormat().getClass());
        assertEquals(20000, result.getTheRoute().getPositionCount());
    }

    @Test
    public void testReadRemoteFileLargerThanDefaultBuffer() throws Exception {
        byte[] body = columbusBody(20000);
        // the body must exceed the default buffer to exercise the mark()/reset() path
        assertTrue(body.length > NavigationFormatParser.TOTAL_BUFFER_SIZE);
        withServer(body, /* withContentLength */ true, url -> {
            ParserResult result = parser.read(url);
            assertTrue(result.isSuccessful());
            assertEquals(ColumbusGpsType1Format.class, result.getFormat().getClass());
        });
    }

    @Test
    public void testReadRemoteFileChunkedWithoutContentLength() throws Exception {
        byte[] body = columbusBody(20000);
        // a chunked response carries no Content-Length; detection must still work
        withServer(body, /* withContentLength */ false, url -> {
            ParserResult result = parser.read(url);
            assertTrue(result.isSuccessful());
            assertEquals(ColumbusGpsType1Format.class, result.getFormat().getClass());
        });
    }

    @Test
    public void testReadRemoteNotFoundThrows() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            URL url = new URL("http://127.0.0.1:" + server.getAddress().getPort() + "/missing");
            parser.read(url);
            fail("expected IOException for HTTP 404");
        } catch (IOException expected) {
            // openStream() rejects the error response
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testWriteAndReadBack() throws IOException {
        ParserResult read = parser.read(GPX_11);
        File target = File.createTempFile("parser-test", ".gpx");
        temporaryFiles.add(target);

        parser.write(read.getTheRoute(), new Gpx11Format(), target);

        ParserResult reread = parser.read(target);
        assertTrue(reread.isSuccessful());
        assertEquals(Gpx11Format.class, reread.getFormat().getClass());
        assertEquals(1, reread.getTheRoute().getPositionCount());
    }

    @Test
    public void testGetNumberOfFilesToWriteFor() throws IOException {
        Gpx11Format format = new Gpx11Format();
        ParserResult result = parser.read(GPX_11);
        // one position and a format with a large per-file limit => a single file
        assertEquals(1, NavigationFormatParser.getNumberOfFilesToWriteFor(result.getTheRoute(), format, false));
    }

    private interface UrlConsumer {
        void accept(URL url) throws Exception;
    }

    private void withServer(byte[] body, boolean withContentLength, UrlConsumer consumer) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/file", exchange -> {
            // responseLength 0 => chunked (no Content-Length); body.length => fixed Content-Length
            exchange.sendResponseHeaders(200, withContentLength ? body.length : 0);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            consumer.accept(new URL("http://127.0.0.1:" + server.getAddress().getPort() + "/file"));
        } finally {
            server.stop(0);
        }
    }
}
