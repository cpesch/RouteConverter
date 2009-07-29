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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.googlemaps;

import junit.framework.TestCase;

import java.io.IOException;

import slash.navigation.kml.KmlUtil;
import slash.navigation.kml.binding20.Kml;

import javax.xml.bind.JAXBException;

public class GoogleMapsServiceTest extends TestCase {
    private final GoogleMapsService service = new GoogleMapsService();

    private final String result = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<kml xmlns=\"http://earth.google.com/kml/2.0\"><Response>\n" +
            "  <name>47.3,9.0</name>\n" +
            "  <Status>\n" +
            "    <code>200</code>\n" +
            "    <request>geocode</request>\n" +
            "  </Status>\n" +
            "  <Placemark id=\"p1\">\n" +
            "    <address>8638 Goldingen, Switzerland</address>\n" +
            "    <AddressDetails Accuracy=\"5\" xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\"><Country><CountryNameCode>CH</CountryNameCode><CountryName>Switzerland</CountryName><AdministrativeArea><AdministrativeAreaName>St Gallen</AdministrativeAreaName><SubAdministrativeArea><SubAdministrativeAreaName>Lac</SubAdministrativeAreaName><Locality><LocalityName>Goldingen</LocalityName><PostalCode><PostalCodeNumber>8638</PostalCodeNumber></PostalCode></Locality></SubAdministrativeArea></AdministrativeArea></Country></AddressDetails>\n" +
            "    <ExtendedData>\n" +
            "      <LatLonBox north=\"47.3193810\" south=\"47.2484240\" east=\"9.0203580\" west=\"8.9362864\" />\n" +
            "    </ExtendedData>\n" +
            "    <Point><coordinates>8.9709262,47.2794582,0</coordinates></Point>\n" +
            "  </Placemark>\n" +
            "  <Placemark id=\"p2\">\n" +
            "    <address>Goldingen, Switzerland</address>\n" +
            "    <AddressDetails Accuracy=\"4\" xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\"><Country><CountryNameCode>CH</CountryNameCode><CountryName>Switzerland</CountryName><AdministrativeArea><AdministrativeAreaName>St Gallen</AdministrativeAreaName><SubAdministrativeArea><SubAdministrativeAreaName>Lac</SubAdministrativeAreaName><Locality><LocalityName>Goldingen</LocalityName></Locality></SubAdministrativeArea></AdministrativeArea></Country></AddressDetails>\n" +
            "    <ExtendedData>\n" +
            "      <LatLonBox north=\"47.3193810\" south=\"47.2484240\" east=\"9.0203580\" west=\"8.9336580\" />\n" +
            "    </ExtendedData>\n" +
            "    <Point><coordinates>8.9654168,47.2629857,0</coordinates></Point>\n" +
            "  </Placemark>\n" +
            "  <Placemark id=\"p3\">\n" +
            "    <address>Lac, Switzerland</address>\n" +
            "    <AddressDetails Accuracy=\"3\" xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\"><Country><CountryNameCode>CH</CountryNameCode><CountryName>Switzerland</CountryName><AdministrativeArea><AdministrativeAreaName>St Gallen</AdministrativeAreaName><SubAdministrativeArea><SubAdministrativeAreaName>Lac</SubAdministrativeAreaName></SubAdministrativeArea></AdministrativeArea></Country></AddressDetails>\n" +
            "    <ExtendedData>\n" +
            "      <LatLonBox north=\"47.3193810\" south=\"47.1236950\" east=\"9.2379090\" west=\"8.7956860\" />\n" +
            "    </ExtendedData>\n" +
            "    <Point><coordinates>9.0108986,47.2320001,0</coordinates></Point>\n" +
            "  </Placemark>\n" +
            "  <Placemark id=\"p4\">\n" +
            "    <address>St Gallen, Switzerland</address>\n" +
            "    <AddressDetails Accuracy=\"2\" xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\"><Country><CountryNameCode>CH</CountryNameCode><CountryName>Switzerland</CountryName><AdministrativeArea><AdministrativeAreaName>St Gallen</AdministrativeAreaName></AdministrativeArea></Country></AddressDetails>\n" +
            "    <ExtendedData>\n" +
            "      <LatLonBox north=\"47.5839688\" south=\"46.8728090\" east=\"9.6742830\" west=\"8.7956860\" />\n" +
            "    </ExtendedData>\n" +
            "    <Point><coordinates>9.3504332,47.1456254,0</coordinates></Point>\n" +
            "  </Placemark>\n" +
            "  <Placemark id=\"p5\">\n" +
            "    <address>Switzerland</address>\n" +
            "    <AddressDetails Accuracy=\"1\" xmlns=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\"><Country><CountryNameCode>CH</CountryNameCode><CountryName>Switzerland</CountryName></Country></AddressDetails>\n" +
            "    <ExtendedData>\n" +
            "      <LatLonBox north=\"47.8083810\" south=\"45.8177919\" east=\"10.4923800\" west=\"5.9558940\" />\n" +
            "    </ExtendedData>\n" +
            "    <Point><coordinates>8.2275120,46.8181880,0</coordinates></Point>\n" +
            "  </Placemark>\n" +
            "</Response></kml>\n";

    public void testResponse() throws JAXBException {
        Kml kml = KmlUtil.unmarshal20(result);
        assertNotNull(kml);
        assertNotNull(kml.getResponse());
        assertNotNull(kml.getResponse().getNameOrStatusOrPlacemark());
        assertEquals(200, service.extractStatusCode(kml));
        assertEquals(5, service.extractPlacemarks(kml).size());
        assertEquals("8638 Goldingen, Switzerland", service.extractHighestAccuracyLocation(kml));
    }

    public void testLocationLookup() throws IOException {
        assertEquals("8638 Goldingen, Switzerland", service.getLocationFor(9.0, 47.3));
        assertEquals("Bühlstraße, 97506 Grafenrheinfeld, Germany", service.getLocationFor(10.2, 50.001));
        assertEquals("82467 Garmisch-Partenkirchen, Germany", service.getLocationFor(11.06561, 47.42428));
        assertNull(service.getLocationFor(0.0, 0.0));
        assertNull(service.getLocationFor(0.0, -90.0));
        assertNull(service.getLocationFor(0.0, 90.0));
        assertNull(service.getLocationFor(90.0, 90.0));
    }
}
