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
package slash.navigation.kml;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.junit.Test;
import slash.navigation.kml.binding21.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.*;
import static slash.navigation.kml.KmlUtil.*;

/**
 * Tests JAXB binding objects and round-trips for KML 2.1 bindings.
 *
 * @author Christian Pesch
 */
public class KmlBinding21Test {

    private static final ObjectFactory FACTORY = new ObjectFactory();

    // ---- helpers ----

    private static KmlType roundTrip21(KmlType kml) throws IOException, JAXBException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshal21(kml, out);
        String xml = out.toString("UTF-8");
        assertFalse("marshalled XML must not be empty", xml.isEmpty());
        return unmarshal21(new StringReader(xml));
    }

    private static PointType point21(String coords) {
        PointType pt = FACTORY.createPointType();
        pt.getCoordinates().add(coords);
        return pt;
    }

    // ---- KmlType ----

    @Test
    public void kmlTypeDefaultFeatureIsNull() {
        KmlType kml = FACTORY.createKmlType();
        assertNull(kml.getFeature());
    }

    @Test
    public void kmlTypeNetworkLinkControlNullByDefault() {
        KmlType kml = FACTORY.createKmlType();
        assertNull(kml.getNetworkLinkControl());
    }

    // ---- FeatureType (accessed via PlacemarkType) ----

    @Test
    public void placemarkTypeNameAndDescription() {
        PlacemarkType p = FACTORY.createPlacemarkType();
        p.setName("Berlin");
        p.setDescription("Capital of Germany");
        assertEquals("Berlin", p.getName());
        assertEquals("Capital of Germany", p.getDescription());
    }

    @Test
    public void placemarkTypeVisibility() {
        PlacemarkType p = FACTORY.createPlacemarkType();
        p.setVisibility(Boolean.FALSE);
        assertEquals(Boolean.FALSE, p.getVisibility());
    }

    @Test
    public void placemarkTypeAddressAndPhoneNumber() {
        PlacemarkType p = FACTORY.createPlacemarkType();
        p.setAddress("Unter den Linden 1");
        p.setPhoneNumber("+49 30 12345");
        assertEquals("Unter den Linden 1", p.getAddress());
        assertEquals("+49 30 12345", p.getPhoneNumber());
    }

    @Test
    public void placemarkTypeStyleUrl() {
        PlacemarkType p = FACTORY.createPlacemarkType();
        p.setStyleUrl("#highlight");
        assertEquals("#highlight", p.getStyleUrl());
    }

    @Test
    public void placemarkTypeGeometry() {
        PlacemarkType p = FACTORY.createPlacemarkType();
        PointType pt = point21("13.4,52.5,0");
        p.setGeometry(FACTORY.createPoint(pt));
        assertNotNull(p.getGeometry());
        PointType recovered = (PointType) p.getGeometry().getValue();
        assertEquals("13.4,52.5,0", recovered.getCoordinates().get(0));
    }

    // ---- PointType ----

    @Test
    public void pointTypeCoordinates() {
        PointType pt = point21("11.576,48.137,500");
        assertEquals(1, pt.getCoordinates().size());
        assertEquals("11.576,48.137,500", pt.getCoordinates().get(0));
    }

    @Test
    public void pointTypeExtrude() {
        PointType pt = FACTORY.createPointType();
        pt.setExtrude(Boolean.TRUE);
        assertEquals(Boolean.TRUE, pt.getExtrude());
    }

    @Test
    public void pointTypeTessellate() {
        PointType pt = FACTORY.createPointType();
        pt.setTessellate(Boolean.FALSE);
        assertEquals(Boolean.FALSE, pt.getTessellate());
    }

    @Test
    public void pointTypeAltitudeMode() {
        PointType pt = FACTORY.createPointType();
        pt.setAltitudeMode(AltitudeModeEnum.ABSOLUTE);
        assertEquals(AltitudeModeEnum.ABSOLUTE, pt.getAltitudeMode());
    }

    // ---- LineStringType ----

    @Test
    public void lineStringTypeCoordinates() {
        LineStringType ls = FACTORY.createLineStringType();
        ls.getCoordinates().add("13.4,52.5");
        ls.getCoordinates().add("13.5,52.6");
        assertEquals(2, ls.getCoordinates().size());
    }

    @Test
    public void lineStringTypeTessellate() {
        LineStringType ls = FACTORY.createLineStringType();
        ls.setTessellate(Boolean.TRUE);
        assertEquals(Boolean.TRUE, ls.getTessellate());
    }

    // ---- FolderType ----

    @Test
    public void folderTypeNameAndVisibility() {
        FolderType folder = FACTORY.createFolderType();
        folder.setName("My Folder");
        folder.setVisibility(Boolean.TRUE);
        assertEquals("My Folder", folder.getName());
        assertEquals(Boolean.TRUE, folder.getVisibility());
    }

    @Test
    public void folderTypeContainsPlacemark() {
        FolderType folder = FACTORY.createFolderType();
        PlacemarkType p = FACTORY.createPlacemarkType();
        p.setName("P1");
        folder.getFeature().add(FACTORY.createPlacemark(p));
        assertEquals(1, folder.getFeature().size());
    }

    // ---- DocumentType ----

    @Test
    public void documentTypeNameAndFeature() {
        DocumentType doc = FACTORY.createDocumentType();
        doc.setName("My Document");
        PlacemarkType p = FACTORY.createPlacemarkType();
        p.setName("P1");
        doc.getFeature().add(FACTORY.createPlacemark(p));
        assertEquals("My Document", doc.getName());
        assertEquals(1, doc.getFeature().size());
    }

    // ---- LookAtType ----

    @Test
    public void lookAtTypeCoordinates() {
        LookAtType la = FACTORY.createLookAtType();
        la.setLongitude(13.4);
        la.setLatitude(52.5);
        la.setAltitude(200.0);
        la.setHeading(45.0);
        la.setTilt(30.0);
        la.setRange(5000.0);
        assertEquals(13.4, la.getLongitude(), 0.001);
        assertEquals(52.5, la.getLatitude(), 0.001);
        assertEquals(200.0, la.getAltitude(), 0.001);
        assertEquals(45.0, la.getHeading(), 0.001);
        assertEquals(30.0, la.getTilt(), 0.001);
        assertEquals(5000.0, la.getRange(), 0.001);
    }

    // ---- round-trip: Placemark with Point ----

    @Test
    public void roundTripPlacemarkWithPoint() throws IOException, JAXBException {
        PlacemarkType p = FACTORY.createPlacemarkType();
        p.setName("Munich");
        p.setDescription("Bavarian capital");
        p.setGeometry(FACTORY.createPoint(point21("11.576,48.137,0")));

        DocumentType doc = FACTORY.createDocumentType();
        doc.setName("Cities");
        doc.getFeature().add(FACTORY.createPlacemark(p));

        KmlType kml = FACTORY.createKmlType();
        kml.setFeature(FACTORY.createDocument(doc));

        KmlType result = roundTrip21(kml);

        assertNotNull(result.getFeature());
        DocumentType resultDoc = (DocumentType) result.getFeature().getValue();
        assertEquals("Cities", resultDoc.getName());
        assertEquals(1, resultDoc.getFeature().size());

        PlacemarkType resultP = (PlacemarkType) resultDoc.getFeature().get(0).getValue();
        assertEquals("Munich", resultP.getName());
        assertEquals("Bavarian capital", resultP.getDescription());

        PointType resultPt = (PointType) resultP.getGeometry().getValue();
        assertTrue(resultPt.getCoordinates().get(0).startsWith("11.576"));
    }

    @Test
    public void roundTripFolderWithMultiplePlacemarks() throws IOException, JAXBException {
        FolderType folder = FACTORY.createFolderType();
        folder.setName("Waypoints");
        for (int i = 0; i < 3; i++) {
            PlacemarkType p = FACTORY.createPlacemarkType();
            p.setName("WP" + i);
            p.setGeometry(FACTORY.createPoint(point21(i + ".0," + i + ".0")));
            folder.getFeature().add(FACTORY.createPlacemark(p));
        }

        KmlType kml = FACTORY.createKmlType();
        kml.setFeature(FACTORY.createFolder(folder));

        KmlType result = roundTrip21(kml);
        FolderType resultFolder = (FolderType) result.getFeature().getValue();
        assertEquals("Waypoints", resultFolder.getName());
        assertEquals(3, resultFolder.getFeature().size());
    }

    @Test
    public void roundTripFromInputStream() throws IOException, JAXBException {
        PlacemarkType p = FACTORY.createPlacemarkType();
        p.setName("Vienna");
        p.setGeometry(FACTORY.createPoint(point21("16.37,48.21")));

        KmlType kml = FACTORY.createKmlType();
        kml.setFeature(FACTORY.createPlacemark(p));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshal21(kml, out);
        byte[] bytes = out.toByteArray();

        KmlType result = unmarshal21(new ByteArrayInputStream(bytes));
        PlacemarkType resultP = (PlacemarkType) result.getFeature().getValue();
        assertEquals("Vienna", resultP.getName());
    }
}





