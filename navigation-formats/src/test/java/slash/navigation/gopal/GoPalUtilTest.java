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

package slash.navigation.gopal;

import org.junit.Test;
import slash.navigation.gopal.binding3.ObjectFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link GoPalUtil} binding3 (GoPal v3) unmarshal/marshal round-trips.
 */
public class GoPalUtilTest {

    // Minimal valid GoPal v3 XML with required options attributes and one dest element
    private static final String GOPAL3_XML = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" +
            "<tour>" +
            "<options CyclistSpeed=\"4\" Ferries=\"1\" Mode=\"2\" MotorWays=\"0\" PedestrianSpeed=\"1\"" +
            " TTIMode=\"0\" TollRoad=\"1\" Tunnels=\"1\" Type=\"3\"" +
            " VehicleSpeedInPedestrianArea=\"2\" VehicleSpeedMotorway=\"33\" VehicleSpeedNonMotorway=\"27\"/>" +
            "<dest City=\"Hamburg\" Country=\"49\" Latitude=\"53570000\" Longitude=\"10050000\" Zip=\"20095\" StartPos=\"1\"/>" +
            "</tour>";

    // --- unmarshal3 ---

    @Test
    public void testUnmarshal3ReturnsNonNull() throws IOException {
        byte[] bytes = GOPAL3_XML.getBytes(Charset.forName("ISO-8859-1"));
        slash.navigation.gopal.binding3.Tour tour = GoPalUtil.unmarshal3(new ByteArrayInputStream(bytes));
        assertNotNull(tour);
    }

    @Test
    public void testUnmarshal3OptionsArePresent() throws IOException {
        byte[] bytes = GOPAL3_XML.getBytes(Charset.forName("ISO-8859-1"));
        slash.navigation.gopal.binding3.Tour tour = GoPalUtil.unmarshal3(new ByteArrayInputStream(bytes));
        assertNotNull(tour.getOptions());
        assertEquals(4, tour.getOptions().getCyclistSpeed());
        assertEquals(3, tour.getOptions().getType());
    }

    @Test
    public void testUnmarshal3DestIsPresent() throws IOException {
        byte[] bytes = GOPAL3_XML.getBytes(Charset.forName("ISO-8859-1"));
        slash.navigation.gopal.binding3.Tour tour = GoPalUtil.unmarshal3(new ByteArrayInputStream(bytes));
        assertEquals(1, tour.getDest().size());
        assertEquals("Hamburg", tour.getDest().get(0).getCity());
        assertEquals(53570000L, tour.getDest().get(0).getLatitude());
    }

    // --- marshal3 ---

    @Test
    public void testMarshal3ProducesNonEmptyOutput() throws Exception {
        ObjectFactory factory = new ObjectFactory();
        slash.navigation.gopal.binding3.Tour tour = factory.createTour();

        slash.navigation.gopal.binding3.Tour.Options options = factory.createTourOptions();
        options.setCyclistSpeed((short) 4);
        options.setFerries((short) 1);
        options.setMode((short) 2);
        options.setMotorWays((short) 0);
        options.setPedestrianSpeed((short) 1);
        options.setTTIMode((short) 0);
        options.setTollRoad((short) 1);
        options.setTunnels((short) 1);
        options.setType((short) 3);
        options.setVehicleSpeedInPedestrianArea((short) 2);
        options.setVehicleSpeedMotorway((short) 33);
        options.setVehicleSpeedNonMotorway((short) 27);
        tour.setOptions(options);

        slash.navigation.gopal.binding3.Tour.Dest dest = factory.createTourDest();
        dest.setCity("Hamburg");
        dest.setCountry((short) 49);
        dest.setLatitude(53570000L);
        dest.setLongitude(10050000L);
        dest.setZip("20095");
        tour.getDest().add(dest);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GoPalUtil.marshal3(tour, out);
        String xml = out.toString("ISO-8859-1");
        assertNotNull(xml);
        assert xml.contains("Hamburg") : "Marshalled output should contain city name";
        assert xml.contains("53570000") : "Marshalled output should contain latitude";
    }

    // --- round-trip: marshal3 then unmarshal3 ---

    @Test
    public void testRoundTrip3() throws Exception {
        ObjectFactory factory = new ObjectFactory();
        slash.navigation.gopal.binding3.Tour original = factory.createTour();

        slash.navigation.gopal.binding3.Tour.Options options = factory.createTourOptions();
        options.setCyclistSpeed((short) 14);
        options.setFerries((short) 0);
        options.setMode((short) 0);
        options.setMotorWays((short) 1);
        options.setPedestrianSpeed((short) 5);
        options.setTTIMode((short) 0);
        options.setTollRoad((short) 1);
        options.setTunnels((short) 1);
        options.setTTIMode((short) 0);
        options.setVehicleSpeedInPedestrianArea((short) 7);
        options.setVehicleSpeedMotorway((short) 130);
        options.setVehicleSpeedNonMotorway((short) 100);
        original.setOptions(options);

        slash.navigation.gopal.binding3.Tour.Dest dest = factory.createTourDest();
        dest.setCity("Berlin");
        dest.setCountry((short) 49);
        dest.setLatitude(52520000L);
        dest.setLongitude(13400000L);
        dest.setZip("10117");
        original.getDest().add(dest);

        // Marshal
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GoPalUtil.marshal3(original, out);

        // Unmarshal
        slash.navigation.gopal.binding3.Tour roundtripped = GoPalUtil.unmarshal3(
                new ByteArrayInputStream(out.toByteArray()));

        assertEquals("Berlin", roundtripped.getDest().get(0).getCity());
        assertEquals(52520000L, roundtripped.getDest().get(0).getLatitude());
        assertEquals(14, roundtripped.getOptions().getCyclistSpeed());
    }
}

