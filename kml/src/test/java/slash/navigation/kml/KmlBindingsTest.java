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
import slash.navigation.kml.binding22.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;
import static slash.navigation.kml.KmlUtil.*;

/**
 * Tests JAXB round-trips for KML 2.2 binding objects (Document, Folder, Placemark, Point, LineString).
 *
 * @author Christian Pesch
 */
public class KmlBindingsTest {

    private static final ObjectFactory FACTORY = new ObjectFactory();

    // ---- helpers ----

    private static KmlType roundTrip22(KmlType kml) throws IOException, JAXBException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshal22(kml, out);
        String xml = out.toString("UTF-8");
        assertFalse("XML must not be empty", xml.isEmpty());
        return unmarshal22(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }

    private static PlacemarkType placemark(String name, JAXBElement<? extends AbstractGeometryType> geometry) {
        PlacemarkType p = FACTORY.createPlacemarkType();
        p.setName(name);
        p.setAbstractGeometryGroup(geometry);
        return p;
    }

    private static PointType point(String coords) {
        PointType pt = FACTORY.createPointType();
        pt.getCoordinates().add(coords);
        return pt;
    }

    private static LineStringType lineString(String... coordPairs) {
        LineStringType ls = FACTORY.createLineStringType();
        for (String c : coordPairs)
            ls.getCoordinates().add(c);
        return ls;
    }

    // ---- KmlType basic accessors ----

    @Test
    public void kmlTypeDefaultValues() {
        KmlType kml = FACTORY.createKmlType();
        assertNull("hint should be null", kml.getHint());
        assertNull("networkLinkControl should be null", kml.getNetworkLinkControl());
        assertNull("abstractFeatureGroup should be null", kml.getAbstractFeatureGroup());
    }

    @Test
    public void kmlTypeHint() {
        KmlType kml = FACTORY.createKmlType();
        kml.setHint("target");
        assertEquals("target", kml.getHint());
    }

    // ---- PointType ----

    @Test
    public void pointTypeCoordinates() {
        PointType pt = point("13.4,52.5,0");
        assertEquals(1, pt.getCoordinates().size());
        assertEquals("13.4,52.5,0", pt.getCoordinates().get(0));
    }

    @Test
    public void pointTypeAltitudeModeGroupNullByDefault() {
        PointType pt = FACTORY.createPointType();
        assertNull("default altitudeModeGroup should be null", pt.getAltitudeModeGroup());
    }

    @Test
    public void pointTypeExtrude() {
        PointType pt = FACTORY.createPointType();
        pt.setExtrude(Boolean.TRUE);
        assertEquals(Boolean.TRUE, pt.isExtrude());
    }

    // ---- LineStringType ----

    @Test
    public void lineStringTypeCoordinates() {
        LineStringType ls = lineString("13.4,52.5", "13.5,52.6");
        assertEquals(2, ls.getCoordinates().size());
    }

    @Test
    public void lineStringTypeTessellate() {
        LineStringType ls = FACTORY.createLineStringType();
        ls.setTessellate(Boolean.TRUE);
        assertEquals(Boolean.TRUE, ls.isTessellate());
    }

    @Test
    public void lineStringTypeExtrude() {
        LineStringType ls = FACTORY.createLineStringType();
        ls.setExtrude(Boolean.FALSE);
        assertEquals(Boolean.FALSE, ls.isExtrude());
    }

    // ---- PlacemarkType ----

    @Test
    public void placemarkTypeNameAndDescription() {
        PlacemarkType p = FACTORY.createPlacemarkType();
        p.setName("Test");
        p.setDescription("A test placemark");
        assertEquals("Test", p.getName());
        assertEquals("A test placemark", p.getDescription());
    }

    @Test
    public void placemarkTypeWithPoint() {
        PlacemarkType p = placemark("Berlin", FACTORY.createPoint(point("13.4,52.5,0")));
        assertNotNull(p.getAbstractGeometryGroup());
        PointType recovered = (PointType) p.getAbstractGeometryGroup().getValue();
        assertEquals("13.4,52.5,0", recovered.getCoordinates().get(0));
    }

    @Test
    public void placemarkTypeVisibility() {
        PlacemarkType p = FACTORY.createPlacemarkType();
        p.setVisibility(Boolean.FALSE);
        assertEquals(Boolean.FALSE, p.isVisibility());
    }

    @Test
    public void placemarkTypeStyleUrl() {
        PlacemarkType p = FACTORY.createPlacemarkType();
        p.setStyleUrl("#myStyle");
        assertEquals("#myStyle", p.getStyleUrl());
    }

    // ---- FolderType ----

    @Test
    public void folderTypeNameAndFeatures() {
        FolderType folder = FACTORY.createFolderType();
        folder.setName("My Folder");

        PlacemarkType p = placemark("Point1", FACTORY.createPoint(point("1.0,2.0")));
        folder.getAbstractFeatureGroup().add(FACTORY.createPlacemark(p));

        assertEquals("My Folder", folder.getName());
        assertEquals(1, folder.getAbstractFeatureGroup().size());
    }

    @Test
    public void folderTypeVisibility() {
        FolderType folder = FACTORY.createFolderType();
        folder.setVisibility(Boolean.TRUE);
        assertEquals(Boolean.TRUE, folder.isVisibility());
    }

    // ---- DocumentType ----

    @Test
    public void documentTypeNameAndFeatures() {
        DocumentType doc = FACTORY.createDocumentType();
        doc.setName("My Document");

        PlacemarkType p = placemark("P1", FACTORY.createPoint(point("10.0,50.0")));
        doc.getAbstractFeatureGroup().add(FACTORY.createPlacemark(p));

        assertEquals("My Document", doc.getName());
        assertEquals(1, doc.getAbstractFeatureGroup().size());
    }

    // ---- round-trip tests ----

    @Test
    public void roundTripPlacemarkWithPoint() throws IOException, JAXBException {
        PlacemarkType p = placemark("Munich", FACTORY.createPoint(point("11.576,48.137,0")));
        p.setDescription("Bavarian capital");

        DocumentType doc = FACTORY.createDocumentType();
        doc.setName("Cities");
        doc.getAbstractFeatureGroup().add(FACTORY.createPlacemark(p));

        KmlType kml = FACTORY.createKmlType();
        kml.setAbstractFeatureGroup(FACTORY.createDocument(doc));

        KmlType result = roundTrip22(kml);

        assertNotNull(result.getAbstractFeatureGroup());
        DocumentType resultDoc = (DocumentType) result.getAbstractFeatureGroup().getValue();
        assertEquals("Cities", resultDoc.getName());
        assertEquals(1, resultDoc.getAbstractFeatureGroup().size());

        PlacemarkType resultP = (PlacemarkType) resultDoc.getAbstractFeatureGroup().get(0).getValue();
        assertEquals("Munich", resultP.getName());
        assertEquals("Bavarian capital", resultP.getDescription());

        PointType resultPt = (PointType) resultP.getAbstractGeometryGroup().getValue();
        assertTrue(resultPt.getCoordinates().get(0).startsWith("11.576"));
    }

    @Test
    public void roundTripPlacemarkWithLineString() throws IOException, JAXBException {
        LineStringType ls = lineString("13.4,52.5", "13.5,52.6", "13.6,52.7");
        ls.setTessellate(Boolean.TRUE);

        PlacemarkType p = placemark("Route", FACTORY.createLineString(ls));
        FolderType folder = FACTORY.createFolderType();
        folder.setName("Routes");
        folder.getAbstractFeatureGroup().add(FACTORY.createPlacemark(p));

        KmlType kml = FACTORY.createKmlType();
        kml.setAbstractFeatureGroup(FACTORY.createFolder(folder));

        KmlType result = roundTrip22(kml);

        FolderType resultFolder = (FolderType) result.getAbstractFeatureGroup().getValue();
        assertEquals("Routes", resultFolder.getName());

        PlacemarkType resultP = (PlacemarkType) resultFolder.getAbstractFeatureGroup().get(0).getValue();
        assertEquals("Route", resultP.getName());

        LineStringType resultLs = (LineStringType) resultP.getAbstractGeometryGroup().getValue();
        assertEquals(3, resultLs.getCoordinates().size());
        assertEquals(Boolean.TRUE, resultLs.isTessellate());
    }

    @Test
    public void roundTripMultiplePlacemarks() throws IOException, JAXBException {
        DocumentType doc = FACTORY.createDocumentType();
        doc.setName("Multi");

        for (int i = 0; i < 5; i++) {
            PlacemarkType p = placemark("P" + i, FACTORY.createPoint(point(i + ".0," + i + ".0")));
            doc.getAbstractFeatureGroup().add(FACTORY.createPlacemark(p));
        }

        KmlType kml = FACTORY.createKmlType();
        kml.setAbstractFeatureGroup(FACTORY.createDocument(doc));

        KmlType result = roundTrip22(kml);
        DocumentType resultDoc = (DocumentType) result.getAbstractFeatureGroup().getValue();
        assertEquals(5, resultDoc.getAbstractFeatureGroup().size());
    }

    // ---- ObjectFactory create methods for other types ----

    @Test
    public void lookAtTypeGettersAndSetters() {
        LookAtType la = FACTORY.createLookAtType();
        la.setLongitude(13.4);
        la.setLatitude(52.5);
        la.setAltitude(100.0);
        la.setHeading(90.0);
        la.setTilt(45.0);
        la.setRange(1000.0);
        assertEquals(13.4, la.getLongitude(), 0.001);
        assertEquals(52.5, la.getLatitude(), 0.001);
        assertEquals(100.0, la.getAltitude(), 0.001);
        assertEquals(90.0, la.getHeading(), 0.001);
        assertEquals(45.0, la.getTilt(), 0.001);
        assertEquals(1000.0, la.getRange(), 0.001);
    }

    @Test
    public void styleTypeId() {
        StyleType style = FACTORY.createStyleType();
        style.setId("myStyle");
        assertEquals("myStyle", style.getId());
    }

    @Test
    public void iconStyleTypeScaleElementNullByDefault() {
        IconStyleType icon = FACTORY.createIconStyleType();
        assertNull("default scaleElement should be null", icon.getScaleElement());
    }

    @Test
    public void lineStyleTypeWidth() {
        LineStyleType lineStyle = FACTORY.createLineStyleType();
        lineStyle.setWidth(3.0);
        assertEquals(3.0, lineStyle.getWidth(), 0.001);
    }

    @Test
    public void polygonStyleTypeFill() {
        PolyStyleType polyStyle = FACTORY.createPolyStyleType();
        polyStyle.setFill(Boolean.FALSE);
        assertEquals(Boolean.FALSE, polyStyle.isFill());
    }
}




