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

package slash.navigation.tcx;

import org.junit.Test;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TcxUtilTest {
    private static final String TCX_1_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <TrainingCenterDatabase xmlns="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v1"
                                    xmlns:ae="http://www.garmin.com/xmlschemas/ActivityExtension/v1">
              <Courses>
                <CourseFolder Name="RouteConverter">
                  <Course>
                    <Name>Sample Course</Name>
                    <Track>
                      <Trackpoint>
                        <Time>2026-06-03T09:30:00Z</Time>
                        <Position>
                          <LatitudeDegrees>47.1</LatitudeDegrees>
                          <LongitudeDegrees>11.3</LongitudeDegrees>
                        </Position>
                        <AltitudeMeters>820.0</AltitudeMeters>
                        <Extensions>
                          <ae:ActivityTrackpointExtension SourceSensor="Bike">
                            <ae:Speed>5.2</ae:Speed>
                          </ae:ActivityTrackpointExtension>
                        </Extensions>
                      </Trackpoint>
                    </Track>
                  </Course>
                </CourseFolder>
              </Courses>
            </TrainingCenterDatabase>
            """;

    private static final String TCX_2_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <TrainingCenterDatabase xmlns="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"
                                    xmlns:ae="http://www.garmin.com/xmlschemas/ActivityExtension/v2">
              <Courses>
                <Course>
                  <Name>Sample Course</Name>
                  <Track>
                    <Trackpoint>
                      <Time>2026-06-03T10:00:00Z</Time>
                      <Position>
                        <LatitudeDegrees>47.2</LatitudeDegrees>
                        <LongitudeDegrees>11.4</LongitudeDegrees>
                      </Position>
                      <AltitudeMeters>821.0</AltitudeMeters>
                      <Extensions>
                        <ae:TPX CadenceSensor="Bike">
                          <ae:Speed>5.4</ae:Speed>
                          <ae:Watts>210</ae:Watts>
                        </ae:TPX>
                      </Extensions>
                    </Trackpoint>
                  </Track>
                </Course>
              </Courses>
            </TrainingCenterDatabase>
            """;

    @Test
    public void testMarshalAndUnmarshal1RoundTripWithExtensions() throws Exception {
        Object trainingCenterDatabase = TcxUtil.unmarshal1(new ByteArrayInputStream(TCX_1_XML.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(trainingCenterDatabase);
        Object courses = invoke(trainingCenterDatabase, "getCourses");
        assertNotNull(courses);
        Object courseFolder = invoke(courses, "getCourseFolder");
        assertEquals("RouteConverter", invoke(courseFolder, "getName"));
        Object course = invokeList(courseFolder, "getCourse").get(0);
        assertEquals("Sample Course", invoke(course, "getName"));

        Object trackpoint = extractFirstTrackpoint(course);
        Element extension = getFirstExtensionElement(trackpoint);
        assertEquals("ActivityTrackpointExtension", extension.getLocalName());
        assertEquals("http://www.garmin.com/xmlschemas/ActivityExtension/v1", extension.getNamespaceURI());
        assertEquals("Bike", extension.getAttribute("SourceSensor"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Method marshal = TcxUtil.class.getMethod("marshal1", Class.forName("slash.navigation.tcx.binding1.TrainingCenterDatabaseT"), java.io.OutputStream.class);
        marshal.invoke(null, trainingCenterDatabase, outputStream);
        String marshalled = outputStream.toString(StandardCharsets.UTF_8);

        assertTrue(marshalled.contains("<TrainingCenterDatabase"));
        assertTrue(marshalled.contains(TcxUtil.TCX_1_NAMESPACE_URI));
        assertTrue(marshalled.contains("http://www.garmin.com/xmlschemas/ActivityExtension/v1"));
        assertTrue(marshalled.contains("ActivityTrackpointExtension"));

        Object roundTripped = TcxUtil.unmarshal1(new ByteArrayInputStream(marshalled.getBytes(StandardCharsets.UTF_8)));
        Object roundTrippedCourse = invokeList(invoke(invoke(roundTripped, "getCourses"), "getCourseFolder"), "getCourse").get(0);
        assertEquals("Sample Course", invoke(roundTrippedCourse, "getName"));
        assertEquals("ActivityTrackpointExtension", getFirstExtensionElement(extractFirstTrackpoint(roundTrippedCourse)).getLocalName());
    }

    @Test
    public void testMarshalAndUnmarshal2RoundTripWithExtensions() throws Exception {
        Object trainingCenterDatabase = TcxUtil.unmarshal2(new ByteArrayInputStream(TCX_2_XML.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(trainingCenterDatabase);
        Object courses = invoke(trainingCenterDatabase, "getCourses");
        assertNotNull(courses);
        Object course = invokeList(courses, "getCourse").get(0);
        assertEquals("Sample Course", invoke(course, "getName"));

        Element extension = getFirstExtensionElement(extractFirstTrackpoint(course));
        assertEquals("TPX", extension.getLocalName());
        assertEquals("http://www.garmin.com/xmlschemas/ActivityExtension/v2", extension.getNamespaceURI());
        assertEquals("Bike", extension.getAttribute("CadenceSensor"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Method marshal = TcxUtil.class.getMethod("marshal2", Class.forName("slash.navigation.tcx.binding2.TrainingCenterDatabaseT"), java.io.OutputStream.class);
        marshal.invoke(null, trainingCenterDatabase, outputStream);
        String marshalled = outputStream.toString(StandardCharsets.UTF_8);

        assertTrue(marshalled.contains("<TrainingCenterDatabase"));
        assertTrue(marshalled.contains(TcxUtil.TCX_2_NAMESPACE_URI));
        assertTrue(marshalled.contains("http://www.garmin.com/xmlschemas/ActivityExtension/v2"));
        assertTrue(marshalled.contains("<ae:TPX") || marshalled.contains("<TPX"));

        Object roundTripped = TcxUtil.unmarshal2(new ByteArrayInputStream(marshalled.getBytes(StandardCharsets.UTF_8)));
        Object roundTrippedCourse = invokeList(invoke(roundTripped, "getCourses"), "getCourse").get(0);
        assertEquals("Sample Course", invoke(roundTrippedCourse, "getName"));
        assertEquals("TPX", getFirstExtensionElement(extractFirstTrackpoint(roundTrippedCourse)).getLocalName());
    }

    private static Object extractFirstTrackpoint(Object course) throws Exception {
        Object track = invokeList(course, "getTrack").get(0);
        return invokeList(track, "getTrackpoint").get(0);
    }

    private static Element getFirstExtensionElement(Object trackpoint) throws Exception {
        Object extensions = invoke(trackpoint, "getExtensions");
        assertNotNull(extensions);
        List<?> any = invokeList(extensions, "getAny");
        assertEquals(1, any.size());
        Object extension = any.get(0);
        assertTrue(extension instanceof Element);
        return (Element) extension;
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static List<?> invokeList(Object target, String methodName) throws Exception {
        return (List<?>) invoke(target, methodName);
    }
}

