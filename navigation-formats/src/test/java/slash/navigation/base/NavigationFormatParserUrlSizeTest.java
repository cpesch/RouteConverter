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
import org.junit.Test;
import slash.navigation.columbus.ColumbusGpsType1Format;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Guards against the regression where {@code read(URL)} could not detect the
 * format of a remote file larger than the fixed default buffer: getSize(URL)
 * returned a constant, the mark() read limit was too small, and reset() failed
 * between format attempts so any format tried after a full-stream reader (e.g.
 * a Columbus CSV, detected late in the registry) never saw the data.
 *
 * @author Christian Pesch
 */
public class NavigationFormatParserUrlSizeTest {

    @Test
    public void testReadRemoteFileLargerThanDefaultBuffer() throws Exception {
        StringBuilder builder = new StringBuilder("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX\r\n");
        for (int i = 1; i <= 20000; i++)
            builder.append(i).append(",T,181129,120000,20.956431N,085.108008E,20,0,0,3D,SPS ,1.8,1.5,0.9,\r\n");
        byte[] body = builder.toString().getBytes(US_ASCII);
        // the body must exceed the default buffer to exercise the mark()/reset() path
        assertTrue(body.length > NavigationFormatParser.TOTAL_BUFFER_SIZE);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/file", exchange -> {
            exchange.sendResponseHeaders(200, body.length); // fixed length => Content-Length header
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        try {
            URL url = new URL("http://127.0.0.1:" + server.getAddress().getPort() + "/file");
            NavigationFormatParser parser = new NavigationFormatParser(new NavigationFormatRegistry());
            ParserResult result = parser.read(url);

            assertTrue(result.isSuccessful());
            assertEquals(ColumbusGpsType1Format.class, result.getFormat().getClass());
        } finally {
            server.stop(0);
        }
    }
}
