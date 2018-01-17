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
package slash.navigation.gpx;

import org.junit.Test;
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpx.binding11.*;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.io.Transfer.UTF8_ENCODING;
import static slash.navigation.common.NavigationConversion.formatPosition;
import static slash.navigation.gpx.GpxExtensionType.*;
import static slash.navigation.gpx.GpxUtil.toXml;

public class Gpx11ExtensionsTest {
    private slash.navigation.gpx.binding11.ObjectFactory gpx11Factory = new slash.navigation.gpx.binding11.ObjectFactory();
    private slash.navigation.gpx.garmin3.ObjectFactory garmin3Factory = new slash.navigation.gpx.garmin3.ObjectFactory();
    private slash.navigation.gpx.trackpoint1.ObjectFactory trackpoint1Factory = new slash.navigation.gpx.trackpoint1.ObjectFactory();
    private slash.navigation.gpx.trackpoint2.ObjectFactory trackpoint2Factory = new slash.navigation.gpx.trackpoint2.ObjectFactory();

    private WptType createWptType() {
        WptType trkptType = gpx11Factory.createWptType();
        trkptType.setLat(formatPosition(1.0));
        trkptType.setLon(formatPosition(2.0));
        return trkptType;
    }

    private GpxType createGpxType(WptType trkptType) {
        TrksegType trksegType = gpx11Factory.createTrksegType();
        trksegType.getTrkpt().add(trkptType);

        TrkType trkType = gpx11Factory.createTrkType();
        trkType.getTrkseg().add(trksegType);

        GpxType gpx = gpx11Factory.createGpxType();
        gpx.getTrk().add(trkType);
        return gpx;
    }

    private GpxPosition getFirstPositionOfFirstRoute(List<GpxRoute> gpxRoutes) {
        GpxRoute gpxRoute = gpxRoutes.get(0);
        return gpxRoute.getPosition(0);
    }

    private List<GpxRoute> readGpx(String source) throws Exception {
        ParserContext<GpxRoute> context = new ParserContextImpl<>(null, null);
        new Gpx11Format().read(new ByteArrayInputStream(source.getBytes(UTF8_ENCODING)), context);
        return context.getRoutes();
    }

    private String writeGpx(List<GpxRoute> routes) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new Gpx11Format().write(routes, outputStream);
        return new String(outputStream.toByteArray(), UTF8_ENCODING);
    }


    @Test
    public void testWriteHeading() throws Exception {
        WptType trkptType = createWptType();
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);

        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        position1.setHeading(168.4);

        String after = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(168.4, position2.getHeading());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position2.getPositionExtension().getExtensionTypes());
    }

    @Test
    public void testWriteTrackpoint2Heading() throws Exception {
        slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPointExtensionT = trackpoint2Factory.createTrackPointExtensionT();
        trackPointExtensionT.setCourse(new BigDecimal(168.4));
        ExtensionsType extensionsType = gpx11Factory.createExtensionsType();
        extensionsType.getAny().add(trackpoint2Factory.createTrackPointExtension(trackPointExtensionT));

        WptType trkptType = createWptType();
        trkptType.setExtensions(extensionsType);
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);
        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        assertDoubleEquals(168.4, position1.getHeading());

        position1.setHeading(273.9);

        String after1 = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after1);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(273.9, position2.getHeading());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position2.getPositionExtension().getExtensionTypes());

        position2.setHeading(null);

        String after2 = writeGpx(routes2);

        List<GpxRoute> routes3 = readGpx(after2);
        GpxPosition position3 = getFirstPositionOfFirstRoute(routes3);
        assertNull(position3.getHeading());
        assertEquals(new HashSet<>(), position3.getPositionExtension().getExtensionTypes());
        assertFalse(after2.contains("<extensions"));
        assertFalse(after2.contains(":TrackPointExtension"));
    }

    @Test
    public void testWriteTrackpoint2HeadingAndBearing() throws Exception {
        slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPointExtensionT = trackpoint2Factory.createTrackPointExtensionT();
        trackPointExtensionT.setCourse(new BigDecimal(168.4));
        trackPointExtensionT.setBearing(new BigDecimal(2.2));
        ExtensionsType extensionsType = gpx11Factory.createExtensionsType();
        extensionsType.getAny().add(trackpoint2Factory.createTrackPointExtension(trackPointExtensionT));

        WptType trkptType = createWptType();
        trkptType.setExtensions(extensionsType);
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);

        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        position1.setHeading(273.9);

        String after1 = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after1);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(273.9, position2.getHeading());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position2.getPositionExtension().getExtensionTypes());
        // <gpxtpx:bearing> remains unchanged
        assertTrue(after1.contains("<gpxtpx:bearing>2.2"));

        position2.setHeading(null);

        String after2 = writeGpx(routes2);

        List<GpxRoute> routes3 = readGpx(after2);
        GpxPosition position3 = getFirstPositionOfFirstRoute(routes3);
        assertNull(position3.getHeading());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position3.getPositionExtension().getExtensionTypes());
    }

    @Test
    public void testWriteUnknownHeading() throws Exception {
        JAXBElement<String> element = new JAXBElement<>(new QName("http://www.unknown.com/course", "course"), String.class, "168.4");
        ExtensionsType extensionsType = gpx11Factory.createExtensionsType();
        extensionsType.getAny().add(element);

        WptType trkptType = createWptType();
        trkptType.setExtensions(extensionsType);
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);
        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        assertDoubleEquals(168.4, position1.getHeading());

        position1.setHeading(273.9);

        String after1 = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after1);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(273.9, position2.getHeading());
        assertEquals(new HashSet<>(singletonList(Text)), position2.getPositionExtension().getExtensionTypes());

        position2.setHeading(null);

        String after2 = writeGpx(routes2);

        List<GpxRoute> routes3 = readGpx(after2);
        GpxPosition position3 = getFirstPositionOfFirstRoute(routes3);
        assertNull(position3.getHeading());
        assertEquals(new HashSet<>(), position3.getPositionExtension().getExtensionTypes());
        // setting heading to null removes the complete extensions element
        assertFalse(after2.contains("<extensions"));
        assertFalse(after2.contains(":TrackPointExtension"));
    }


    @Test
    public void testWriteSpeed() throws Exception {
        WptType trkptType = createWptType();
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);

        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        position1.setSpeed(32.4);

        String after = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(32.4, position2.getSpeed());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position2.getPositionExtension().getExtensionTypes());
    }

    @Test
    public void testWriteTrackpoint2Speed() throws Exception {
        slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPointExtensionT = trackpoint2Factory.createTrackPointExtensionT();
        trackPointExtensionT.setSpeed(32.4);
        ExtensionsType extensionsType = gpx11Factory.createExtensionsType();
        extensionsType.getAny().add(trackpoint2Factory.createTrackPointExtension(trackPointExtensionT));

        WptType trkptType = createWptType();
        trkptType.setExtensions(extensionsType);
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);
        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        assertDoubleEquals(116.64, position1.getSpeed());

        position1.setSpeed(273.9);

        String after1 = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after1);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(273.96, position2.getSpeed());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position2.getPositionExtension().getExtensionTypes());

        position2.setSpeed(null);

        String after2 = writeGpx(routes2);

        List<GpxRoute> routes3 = readGpx(after2);
        GpxPosition position3 = getFirstPositionOfFirstRoute(routes3);
        assertNull(position3.getSpeed());
        assertEquals(new HashSet<>(), position3.getPositionExtension().getExtensionTypes());
        assertFalse(after2.contains("<extensions"));
        assertFalse(after2.contains(":TrackPointExtension"));
    }

    @Test
    public void testWriteTrackpoint2SpeedAndBearing() throws Exception {
        slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPointExtensionT = trackpoint2Factory.createTrackPointExtensionT();
        trackPointExtensionT.setCourse(new BigDecimal(168.4));
        trackPointExtensionT.setBearing(new BigDecimal(2.2));
        ExtensionsType extensionsType = gpx11Factory.createExtensionsType();
        extensionsType.getAny().add(trackpoint2Factory.createTrackPointExtension(trackPointExtensionT));

        WptType trkptType = createWptType();
        trkptType.setExtensions(extensionsType);
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);

        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        position1.setSpeed(32.4);

        String after1 = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after1);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(32.4, position2.getSpeed());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position2.getPositionExtension().getExtensionTypes());
        // <gpxtpx:bearing> remains unchanged
        assertTrue(after1.contains("<gpxtpx:bearing>2.2"));

        position2.setSpeed(null);

        String after2 = writeGpx(routes2);

        List<GpxRoute> routes3 = readGpx(after2);
        GpxPosition position3 = getFirstPositionOfFirstRoute(routes3);
        assertNull(position3.getSpeed());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position3.getPositionExtension().getExtensionTypes());
    }

    @Test
    public void testWriteUnknownSpeed() throws Exception {
        JAXBElement<String> element = new JAXBElement<>(new QName("http://www.unknown.com/speed", "speed"), String.class, "168.4");
        ExtensionsType extensionsType = gpx11Factory.createExtensionsType();
        extensionsType.getAny().add(element);

        WptType trkptType = createWptType();
        trkptType.setExtensions(extensionsType);
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);
        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        assertDoubleEquals(606.24, position1.getSpeed());

        position1.setSpeed(32.4);

        String after1 = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after1);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(32.4, position2.getSpeed());
        assertEquals(new HashSet<>(singletonList(Text)), position2.getPositionExtension().getExtensionTypes());

        position2.setSpeed(null);

        String after2 = writeGpx(routes2);

        List<GpxRoute> routes3 = readGpx(after2);
        GpxPosition position3 = getFirstPositionOfFirstRoute(routes3);
        assertNull(position3.getSpeed());
        assertEquals(new HashSet<>(), position3.getPositionExtension().getExtensionTypes());
        // setting speed to null removes the complete extensions element
        assertFalse(after2.contains("<extensions"));
        assertFalse(after2.contains(":TrackPointExtension"));
    }


    @Test
    public void testWriteTemperature() throws Exception {
        WptType trkptType = createWptType();
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);

        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        position1.setTemperature(19.8);

        String after = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(19.8, position2.getTemperature());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position2.getPositionExtension().getExtensionTypes());
    }

    @Test
    public void testWriteGarmin3Temperature() throws Exception {
        slash.navigation.gpx.garmin3.TrackPointExtensionT trackPointExtensionT = garmin3Factory.createTrackPointExtensionT();
        trackPointExtensionT.setTemperature(25.0);
        ExtensionsType extensionsType = gpx11Factory.createExtensionsType();
        extensionsType.getAny().add(garmin3Factory.createTrackPointExtension(trackPointExtensionT));

        WptType trkptType = createWptType();
        trkptType.setExtensions(extensionsType);
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);

        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        position1.setTemperature(19.8);

        String after1 = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after1);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(19.8, position2.getTemperature());
        assertEquals(new HashSet<>(singletonList(Garmin3)), position2.getPositionExtension().getExtensionTypes());

        position2.setTemperature(null);

        String after2 = writeGpx(routes2);

        List<GpxRoute> routes3 = readGpx(after2);
        GpxPosition position3 = getFirstPositionOfFirstRoute(routes3);
        assertNull(position3.getTemperature());
        assertEquals(new HashSet<>(), position3.getPositionExtension().getExtensionTypes());
        // setting temperature to null removes the complete extensions element
        assertFalse(after2.contains("<extensions"));
        assertFalse(after2.contains(":TrackPointExtension"));
    }

    @Test
    public void testWriteGarmin3TemperatureThenSpeed() throws Exception {
        slash.navigation.gpx.garmin3.TrackPointExtensionT trackPointExtensionT = garmin3Factory.createTrackPointExtensionT();
        trackPointExtensionT.setTemperature(25.0);
        trackPointExtensionT.setDepth(1.0);
        ExtensionsType extensionsType = gpx11Factory.createExtensionsType();
        extensionsType.getAny().add(garmin3Factory.createTrackPointExtension(trackPointExtensionT));

        WptType trkptType = createWptType();
        trkptType.setExtensions(extensionsType);
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);

        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        position1.setTemperature(19.8);

        String after1 = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after1);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(19.8, position2.getTemperature());
        assertEquals(new HashSet<>(singletonList(Garmin3)), position2.getPositionExtension().getExtensionTypes());

        position2.setSpeed(13.68);

        String after2 = writeGpx(routes2);

        List<GpxRoute> routes3 = readGpx(after2);
        GpxPosition position3 = getFirstPositionOfFirstRoute(routes3);
        assertDoubleEquals(19.8, position3.getTemperature());
        assertDoubleEquals(13.68, position3.getSpeed());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position3.getPositionExtension().getExtensionTypes());
        assertFalse(after2.contains("gpxx:TrackPointExtension"));
        assertTrue(after2.contains("gpxtpx:TrackPointExtension"));
        assertTrue(after2.contains("gpxtpx:depth"));
    }

    @Test
    public void testWriteTrackpoint1TemperatureThenSpeed() throws Exception {
        slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackPointExtensionT = trackpoint1Factory.createTrackPointExtensionT();
        trackPointExtensionT.setAtemp(25.0);
        trackPointExtensionT.setCad(new Short("6"));
        trackPointExtensionT.setDepth(1.0);
        trackPointExtensionT.setHr(new Short("70"));
        trackPointExtensionT.setWtemp(22.0);
        ExtensionsType extensionsType = gpx11Factory.createExtensionsType();
        extensionsType.getAny().add(trackpoint1Factory.createTrackPointExtension(trackPointExtensionT));

        WptType trkptType = createWptType();
        trkptType.setExtensions(extensionsType);
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);

        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        position1.setTemperature(19.8);

        String after1 = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after1);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(19.8, position2.getTemperature());
        assertEquals(new HashSet<>(singletonList(TrackPoint1)), position2.getPositionExtension().getExtensionTypes());

        position2.setSpeed(13.68);

        String after2 = writeGpx(routes2);

        List<GpxRoute> routes3 = readGpx(after2);
        GpxPosition position3 = getFirstPositionOfFirstRoute(routes3);
        assertDoubleEquals(19.8, position3.getTemperature());
        assertDoubleEquals(13.68, position3.getSpeed());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position3.getPositionExtension().getExtensionTypes());
        assertFalse(after2.contains("gpxx:TrackPointExtension"));
        assertTrue(after2.contains("gpxtpx:TrackPointExtension"));
        assertTrue(after2.contains("gpxtpx:cad"));
        assertTrue(after2.contains("gpxtpx:depth"));
        assertTrue(after2.contains("gpxtpx:hr"));
        assertTrue(after2.contains("gpxtpx:wtemp"));
    }

    @Test
    public void testWriteTrackpoint1Temperature() throws Exception {
        slash.navigation.gpx.trackpoint1.TrackPointExtensionT trackPointExtensionT = trackpoint1Factory.createTrackPointExtensionT();
        trackPointExtensionT.setAtemp(25.0);
        trackPointExtensionT.setWtemp(22.0);
        ExtensionsType extensionsType = gpx11Factory.createExtensionsType();
        extensionsType.getAny().add(trackpoint1Factory.createTrackPointExtension(trackPointExtensionT));

        WptType trkptType = createWptType();
        trkptType.setExtensions(extensionsType);
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);

        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        position1.setTemperature(19.8);

        String after1 = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after1);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(19.8, position2.getTemperature());
        assertEquals(new HashSet<>(singletonList(TrackPoint1)), position2.getPositionExtension().getExtensionTypes());
        // <gpxtpx1:wtemp> remains unchanged
        assertTrue(after1.contains("<gpxtpx1:wtemp>22.0</gpxtpx1:wtemp>"));

        position2.setTemperature(null);

        String after2 = writeGpx(routes2);

        List<GpxRoute> routes3 = readGpx(after2);
        GpxPosition position3 = getFirstPositionOfFirstRoute(routes3);
        assertDoubleEquals(22.0, position3.getTemperature());
        assertEquals(new HashSet<>(singletonList(TrackPoint1)), position3.getPositionExtension().getExtensionTypes());
        // setting temperature to null removes the Atemp element

        position3.setTemperature(null);

        String after3 = writeGpx(routes3);

        List<GpxRoute> routes4 = readGpx(after3);
        GpxPosition position4 = getFirstPositionOfFirstRoute(routes4);
        assertNull(position4.getTemperature());

        assertEquals(new HashSet<>(), position4.getPositionExtension().getExtensionTypes());
        // setting temperature to null twice removes the complete extensions element
        assertFalse(after3.contains("<extensions"));
        assertFalse(after3.contains(":TrackPointExtension"));
    }

    @Test
    public void testWriteTrackpoint2Temperature() throws Exception {
        slash.navigation.gpx.trackpoint2.TrackPointExtensionT trackPointExtensionT = trackpoint2Factory.createTrackPointExtensionT();
        trackPointExtensionT.setAtemp(25.0);
        trackPointExtensionT.setWtemp(22.0);
        ExtensionsType extensionsType = gpx11Factory.createExtensionsType();
        extensionsType.getAny().add(trackpoint2Factory.createTrackPointExtension(trackPointExtensionT));

        WptType trkptType = createWptType();
        trkptType.setExtensions(extensionsType);
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);

        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        position1.setTemperature(19.8);

        String after1 = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after1);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(19.8, position2.getTemperature());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position2.getPositionExtension().getExtensionTypes());
        // <gpxtpx:wtemp> remains unchanged
        assertTrue(after1.contains("<gpxtpx:wtemp>22.0</gpxtpx:wtemp>"));

        position2.setTemperature(null);

        String after2 = writeGpx(routes2);

        List<GpxRoute> routes3 = readGpx(after2);
        GpxPosition position3 = getFirstPositionOfFirstRoute(routes3);
        assertDoubleEquals(22.0, position3.getTemperature());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position3.getPositionExtension().getExtensionTypes());
        // setting temperature to null removes the Atemp element

        position3.setTemperature(null);

        String after3 = writeGpx(routes3);

        List<GpxRoute> routes4 = readGpx(after3);
        GpxPosition position4 = getFirstPositionOfFirstRoute(routes4);
        assertNull(position4.getTemperature());

        assertEquals(new HashSet<>(), position4.getPositionExtension().getExtensionTypes());
        // setting temperature to null twice removes the complete extensions element
        assertFalse(after3.contains("<extensions"));
        assertFalse(after3.contains(":TrackPointExtension"));
    }

    @Test
    public void testWriteUnknownTemperature() throws Exception {
        JAXBElement<String> element = new JAXBElement<>(new QName("http://www.unknown.com/temperature", "temperature"), String.class, "25.0");
        ExtensionsType extensionsType = gpx11Factory.createExtensionsType();
        extensionsType.getAny().add(element);

        WptType trkptType = createWptType();
        trkptType.setExtensions(extensionsType);
        GpxType gpx = createGpxType(trkptType);

        String before = toXml(gpx);
        List<GpxRoute> routes1 = readGpx(before);
        GpxPosition position1 = getFirstPositionOfFirstRoute(routes1);
        assertDoubleEquals(25.0, position1.getTemperature());

        position1.setTemperature(19.8);

        String after1 = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after1);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(19.8, position2.getTemperature());
        assertEquals(new HashSet<>(singletonList(Text)), position2.getPositionExtension().getExtensionTypes());

        position2.setTemperature(null);

        String after2 = writeGpx(routes2);

        List<GpxRoute> routes3 = readGpx(after2);
        GpxPosition position3 = getFirstPositionOfFirstRoute(routes3);
        assertNull(position3.getTemperature());
        assertEquals(new HashSet<>(), position3.getPositionExtension().getExtensionTypes());
        // setting temperature to null removes the complete extensions element
        assertFalse(after2.contains("<extensions"));
        assertFalse(after2.contains(":TrackPointExtension"));
    }
}
