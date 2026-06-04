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
import slash.navigation.kml.binding20.Kml;

import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertNotNull;
import static slash.common.TestCase.assertEquals;
import static slash.navigation.kml.KmlUtil.KML_20_NAMESPACE_URI;
import static slash.navigation.kml.KmlUtil.KML_21_NAMESPACE_URI;
import static slash.navigation.kml.KmlUtil.KML_22_BETA_NAMESPACE_URI;
import static slash.navigation.kml.KmlUtil.KML_22_NAMESPACE_URI;
import static slash.navigation.kml.KmlUtil.newUnmarshaller20;
import static slash.navigation.kml.KmlUtil.unmarshal20;
import static slash.navigation.kml.KmlUtil.unmarshal21;
import static slash.navigation.kml.KmlUtil.unmarshal22;
import static slash.navigation.kml.KmlUtil.unmarshal22Beta;

public class KmlUtilTest {
    private static final String KML20_DOCUMENT = """
            <?xml version=\"1.0\" encoding=\"UTF-8\"?>
            <kml xmlns=\"%s\">
              <Folder>
                <Placemark/>
                <Placemark/>
                <Placemark/>
              </Folder>
            </kml>
            """.formatted(KML_20_NAMESPACE_URI);
    private static final String KML21_DOCUMENT = """
            <?xml version=\"1.0\" encoding=\"UTF-8\"?>
            <kml xmlns=\"%s\">
              <Document/>
            </kml>
            """.formatted(KML_21_NAMESPACE_URI);
    private static final String KML22_BETA_DOCUMENT = """
            <?xml version=\"1.0\" encoding=\"UTF-8\"?>
            <kml xmlns=\"%s\">
              <Document/>
            </kml>
            """.formatted(KML_22_BETA_NAMESPACE_URI);
    private static final String KML22_DOCUMENT = """
            <?xml version=\"1.0\" encoding=\"UTF-8\"?>
            <kml xmlns=\"%s\">
              <Document/>
            </kml>
            """.formatted(KML_22_NAMESPACE_URI);

    private Reader reader(String xml) {
        return new StringReader(xml);
    }

    private InputStream inputStream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(UTF_8));
    }

    @Test
    public void testReader() throws IOException, JAXBException {
        Reader reader = reader(KML20_DOCUMENT);
        Kml kml = (Kml) newUnmarshaller20().unmarshal(reader);
        assertNotNull(kml);
        assertNotNull(kml.getFolder());
        assertEquals(3, kml.getFolder().getDocumentOrFolderOrGroundOverlay().size());
    }

    @Test
    public void testInputStream() throws IOException, JAXBException {
        InputStream in = inputStream(KML20_DOCUMENT);
        Kml kml = (Kml) newUnmarshaller20().unmarshal(in);
        assertNotNull(kml);
        assertNotNull(kml.getFolder());
        assertEquals(3, kml.getFolder().getDocumentOrFolderOrGroundOverlay().size());
    }

    @Test
    public void testUnmarshal20() throws IOException {
        Reader reader = reader(KML20_DOCUMENT);
        Kml kml = unmarshal20(reader);
        assertNotNull(kml);
        assertNotNull(kml.getFolder());
        assertEquals(3, kml.getFolder().getDocumentOrFolderOrGroundOverlay().size());
    }

    @Test(expected = IOException.class)
    public void testUnmarshal20TypeError() throws Exception {
        Reader reader = reader(KML20_DOCUMENT);
        unmarshal21(reader);
    }

    @Test
    public void testUnmarshal21() throws IOException {
        Reader reader = reader(KML21_DOCUMENT);
        slash.navigation.kml.binding21.KmlType kml = unmarshal21(reader);
        assertNotNull(kml);
        assertNotNull(kml.getFeature());
    }

    @Test(expected = IOException.class)
    public void testUnmarshal21TypeError() throws Exception {
        Reader reader = reader(KML21_DOCUMENT);
        unmarshal20(reader);
    }

    @Test
    public void testUnmarshal22Beta() throws IOException {
        Reader reader = reader(KML22_BETA_DOCUMENT);
        slash.navigation.kml.binding22beta.KmlType kml = unmarshal22Beta(reader);
        assertNotNull(kml);
        assertNotNull(kml.getAbstractFeatureGroup());
    }

    @Test
    public void testUnmarshal22() throws IOException {
        Reader reader = reader(KML22_DOCUMENT);
        slash.navigation.kml.binding22.KmlType kml = unmarshal22(reader);
        assertNotNull(kml);
        assertNotNull(kml.getAbstractFeatureGroup());
    }
}

