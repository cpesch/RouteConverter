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

import org.junit.Test;

import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.*;
import static slash.navigation.kml.KmlUtil.*;

/**
 * Tests for {@link KmlUtil} marshal/unmarshal round-trips for all four KML dialect versions.
 *
 * @author Christian Pesch
 */
public class KmlUtilTest {

    // ---- namespace URI constants ----

    @Test
    public void testNamespaceUriConstants() {
        assertEquals("http://earth.google.com/kml/2.0", KML_20_NAMESPACE_URI);
        assertEquals("http://earth.google.com/kml/2.1", KML_21_NAMESPACE_URI);
        assertEquals("http://earth.google.com/kml/2.2", KML_22_BETA_NAMESPACE_URI);
        assertEquals("http://www.opengis.net/kml/2.2", KML_22_NAMESPACE_URI);
        assertEquals("http://www.w3.org/2005/Atom", ATOM_2005_NAMESPACE_URI);
        assertEquals("urn:oasis:names:tc:ciq:xsdschema:xAL:2.0", XAL_20_NAMESPACE_URI);
        assertEquals("http://www.google.com/kml/ext/2.2", KML_22_EXT_NAMESPACE_URI);
    }

    // ---- KML 2.0 ----

    private static final String KML_20_DOC =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<kml xmlns=\"http://earth.google.com/kml/2.0\"/>";

    @Test
    public void testUnmarshal20FromReader() throws IOException {
        slash.navigation.kml.binding20.Kml kml = unmarshal20(new StringReader(KML_20_DOC));
        assertNotNull(kml);
    }

    @Test
    public void testUnmarshal20FromStream() throws IOException {
        Object result = unmarshal20(new java.io.ByteArrayInputStream(KML_20_DOC.getBytes("UTF-8")));
        assertNotNull(result);
    }

    @Test
    public void testMarshal20RoundTrip() throws IOException, JAXBException {
        slash.navigation.kml.binding20.Kml kml = unmarshal20(new StringReader(KML_20_DOC));
        assertNotNull(kml);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshal20(kml, out);
        String xml = out.toString("UTF-8");
        assertTrue("marshalled XML should contain kml element", xml.contains("kml"));
    }

    @Test(expected = IOException.class)
    public void testUnmarshal20BadXml() throws IOException {
        unmarshal20(new StringReader("<notvalid/>"));
    }

    // ---- KML 2.1 ----

    private static final String KML_21_DOC =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<kml xmlns=\"http://earth.google.com/kml/2.1\"/>";

    @Test
    public void testUnmarshal21FromReader() throws IOException {
        slash.navigation.kml.binding21.KmlType kml = unmarshal21(new StringReader(KML_21_DOC));
        assertNotNull(kml);
    }

    @Test
    public void testUnmarshal21FromStream() throws IOException {
        slash.navigation.kml.binding21.KmlType kml = unmarshal21(
                new java.io.ByteArrayInputStream(KML_21_DOC.getBytes("UTF-8")));
        assertNotNull(kml);
    }

    @Test
    public void testMarshal21RoundTrip() throws IOException, JAXBException {
        slash.navigation.kml.binding21.KmlType kml = unmarshal21(new StringReader(KML_21_DOC));
        assertNotNull(kml);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshal21(kml, out);
        String xml = out.toString("UTF-8");
        assertTrue("marshalled XML should contain kml element", xml.contains("kml"));
    }

    @Test(expected = IOException.class)
    public void testUnmarshal21BadXml() throws IOException {
        unmarshal21(new StringReader("<notvalid/>"));
    }

    // ---- KML 2.2 Beta ----

    private static final String KML_22BETA_DOC =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<kml xmlns=\"http://earth.google.com/kml/2.2\"/>";

    @Test
    public void testUnmarshal22BetaFromReader() throws IOException {
        slash.navigation.kml.binding22beta.KmlType kml = unmarshal22Beta(new StringReader(KML_22BETA_DOC));
        assertNotNull(kml);
    }

    @Test
    public void testUnmarshal22BetaFromStream() throws IOException {
        slash.navigation.kml.binding22beta.KmlType kml = unmarshal22Beta(
                new java.io.ByteArrayInputStream(KML_22BETA_DOC.getBytes("UTF-8")));
        assertNotNull(kml);
    }

    @Test
    public void testMarshal22BetaRoundTrip() throws IOException, JAXBException {
        slash.navigation.kml.binding22beta.KmlType kml = unmarshal22Beta(new StringReader(KML_22BETA_DOC));
        assertNotNull(kml);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshal22Beta(kml, out);
        String xml = out.toString("UTF-8");
        assertTrue("marshalled XML should contain kml element", xml.contains("kml"));
    }

    @Test(expected = IOException.class)
    public void testUnmarshal22BetaBadXml() throws IOException {
        unmarshal22Beta(new StringReader("<notvalid/>"));
    }

    // ---- KML 2.2 ----

    private static final String KML_22_DOC =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\"/>";

    @Test
    public void testUnmarshal22FromReader() throws IOException {
        slash.navigation.kml.binding22.KmlType kml = unmarshal22(new StringReader(KML_22_DOC));
        assertNotNull(kml);
    }

    @Test
    public void testUnmarshal22FromStream() throws IOException {
        slash.navigation.kml.binding22.KmlType kml = unmarshal22(
                new java.io.ByteArrayInputStream(KML_22_DOC.getBytes("UTF-8")));
        assertNotNull(kml);
    }

    @Test
    public void testMarshal22RoundTrip() throws IOException, JAXBException {
        slash.navigation.kml.binding22.KmlType kml = unmarshal22(new StringReader(KML_22_DOC));
        assertNotNull(kml);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshal22(kml, out);
        String xml = out.toString("UTF-8");
        assertTrue("marshalled XML should contain kml element", xml.contains("kml"));
    }

    @Test(expected = IOException.class)
    public void testUnmarshal22BadXml() throws IOException {
        unmarshal22(new StringReader("<notvalid/>"));
    }
}

