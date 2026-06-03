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

package slash.navigation.lmx;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;

public class NokiaLandmarkExchangeUtilTest {
    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <lmx xmlns="http://www.nokia.com/schemas/location/landmarks/1/0">
              <landmarkCollection>
                <name>Collection</name>
                <description>Collection Description</description>
                <landmark>
                  <name>Waypoint1 Name</name>
                  <description>Description</description>
                  <coordinates>
                    <latitude>47.1</latitude>
                    <longitude>11.3</longitude>
                    <altitude>820.0</altitude>
                  </coordinates>
                  <coverageRadius>2.0</coverageRadius>
                  <mediaLink>
                    <name>URLName</name>
                    <mime>URLMime</mime>
                    <url>https://example.com/media</url>
                  </mediaLink>
                </landmark>
              </landmarkCollection>
            </lmx>
            """;

    @Test
    public void testMarshalAndUnmarshalRoundTrip() throws Exception {
        Object lmx = NokiaLandmarkExchangeUtil.unmarshal(new ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(lmx);
        Object landmarkCollection = invoke(lmx, "getLandmarkCollection");
        assertNotNull(landmarkCollection);
        assertEquals("Collection", invoke(landmarkCollection, "getName"));
        assertEquals("Collection Description", invoke(landmarkCollection, "getDescription"));
        List<?> landmarks = invokeList(landmarkCollection, "getLandmark");
        assertEquals(1, landmarks.size());

        Object landmark = landmarks.get(0);
        assertEquals("Waypoint1 Name", invoke(landmark, "getName"));
        assertEquals("Description", invoke(landmark, "getDescription"));
        Object coordinates = invoke(landmark, "getCoordinates");
        assertDoubleEquals(47.1, (Double) invoke(coordinates, "getLatitude"));
        assertDoubleEquals(11.3, (Double) invoke(coordinates, "getLongitude"));
        assertDoubleEquals(2.0f, (Float) invoke(landmark, "getCoverageRadius"));
        List<?> mediaLinks = invokeList(landmark, "getMediaLink");
        assertEquals(1, mediaLinks.size());
        Object mediaLink = mediaLinks.get(0);
        assertEquals("URLName", invoke(mediaLink, "getName"));
        assertEquals("URLMime", invoke(mediaLink, "getMime"));
        assertEquals("https://example.com/media", invoke(mediaLink, "getUrl").toString());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Method marshal = NokiaLandmarkExchangeUtil.class.getMethod("marshal", Class.forName("slash.navigation.lmx.binding.Lmx"), java.io.OutputStream.class);
        marshal.invoke(null, lmx, outputStream);
        String marshalled = outputStream.toString(StandardCharsets.UTF_8);

        assertTrue(marshalled.contains("<lmx"));
        assertTrue(marshalled.contains(NokiaLandmarkExchangeUtil.LMX_NAMESPACE_URI));

        Object roundTripped = NokiaLandmarkExchangeUtil.unmarshal(new ByteArrayInputStream(marshalled.getBytes(StandardCharsets.UTF_8)));
        Object roundTrippedCollection = invoke(roundTripped, "getLandmarkCollection");
        Object roundTrippedLandmark = invokeList(roundTrippedCollection, "getLandmark").get(0);
        assertEquals("Waypoint1 Name", invoke(roundTrippedLandmark, "getName"));
        assertEquals("https://example.com/media", invoke(invokeList(roundTrippedLandmark, "getMediaLink").get(0), "getUrl").toString());
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    @SuppressWarnings("unchecked")
    private static List<?> invokeList(Object target, String methodName) throws Exception {
        return (List<?>) invoke(target, methodName);
    }
}

