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

        String after = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(19.8, position2.getTemperature());
        assertEquals(new HashSet<>(singletonList(Garmin3)), position2.getPositionExtension().getExtensionTypes());

        position2.setTemperature(null);

        String after2 = writeGpx(routes2);

        List<GpxRoute> routes3 = readGpx(after2);
        GpxPosition position3 = getFirstPositionOfFirstRoute(routes3);
        assertNull(position3.getTemperature());
        assertEquals(new HashSet<>(singletonList(Garmin3)), position3.getPositionExtension().getExtensionTypes());
        // setting temperature to null removes <temperature> element but leaves <gpxx:TrackPointExtension/>
        assertTrue(after2.contains("<extensions>"));
        assertTrue(after2.contains("<gpxx:TrackPointExtension/>"));
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

        String after = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(19.8, position2.getTemperature());
        assertEquals(new HashSet<>(singletonList(TrackPoint1)), position2.getPositionExtension().getExtensionTypes());
        // <gpxtpx1:wtemp> remains unchanged
        assertTrue(after.contains("<gpxtpx1:wtemp>22.0</gpxtpx1:wtemp>"));
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

        String after = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(19.8, position2.getTemperature());
        assertEquals(new HashSet<>(singletonList(TrackPoint2)), position2.getPositionExtension().getExtensionTypes());
        // <gpxtpx:wtemp> remains unchanged
        assertTrue(after.contains("<gpxtpx:wtemp>22.0</gpxtpx:wtemp>"));
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
        position1.setTemperature(19.8);

        String after = writeGpx(routes1);

        List<GpxRoute> routes2 = readGpx(after);
        GpxPosition position2 = getFirstPositionOfFirstRoute(routes2);
        assertDoubleEquals(19.8, position2.getTemperature());
        assertEquals(new HashSet<>(singletonList(Text)), position2.getPositionExtension().getExtensionTypes());
    }
}
