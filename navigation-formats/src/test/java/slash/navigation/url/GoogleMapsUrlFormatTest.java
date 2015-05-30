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

package slash.navigation.url;

import org.junit.Test;
import slash.navigation.base.Wgs84Position;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.url.GoogleMapsUrlFormat.isGoogleMapsLinkUrl;
import static slash.navigation.url.GoogleMapsUrlFormat.isGoogleMapsProfileUrl;

public class GoogleMapsUrlFormatTest {
    private static final String INPUT1_EMAIL = "Betreff: Route nach/zu Riehler Strasse 190 50735 Koeln (Google Maps)\n" +
            "\n" +
            "> Routenplaner\n" +
            "> Link:\n" +
            "<http://maps.google.de/maps?f=d&hl=de&geocode=&saddr=H%C3%B6lderlinstra%C3%9Fe,+51545+Br%C3%B6l,+Oberbergischer+Kreis,+Nordrhein-Westfalen,+Deutschland&daddr=L339%2FWuppertaler+Stra%C3%9Fe+%4050.918890,+7.560880+to%3AL350+%4050.885180,+7.463950+to%3AB%C3%B6vingen%2FK11+%4050.917200,+7.376600+to%3AL312+%4050.916380,+7.327030+to%3AK%C3%B6ln,+Riehler+Str.+190&mrcr=2&mra=mr&sll=50.954318,7.311401&sspn=0.142091,0.32135&ie=UTF8&ll=50.952371,7.261276&spn=0.284193,0.6427&z=11&om=1>\n" +
            ">\n" +
            "> Startadresse: Hoelderlinstrasse 51545 Broel\n" +
            "> Zieladresse: Riehler Strasse 190 50735 Koeln";

    private static final String INPUT2 = "http://maps.google.de/maps?f=d&hl=de&geocode=&saddr=51545+Waldbroel,+Hoelderlinstr.&daddr=50389+Wesseling,+Urfelder+Strasse+221+to%3A50.876178,6.962585&mrcr=1&mrsp=2&sz=10&mra=mi&sll=50.892745,7.312145&sspn=0.569114,1.2854&ie=UTF8&z=10&om=1";

    private static final String INPUT3 = "http://maps.google.com/maps?f=d&hl=de&geocode=&time=&date=&ttype=&saddr=L%C3%BCbeck,+Germany&daddr=Hamburg,+Germany&sll=37.0625,-95.677068&sspn=48.374125,76.464844&ie=UTF8&z=10&om=1";

    private static final String INPUT4 = "http://maps.google.de/maps?f=d&hl=de&geocode=17223560710991701360,51.125340,10.480100%3B12158345081209133212,51.126450,10.720920%3B7678232323906648676,50.944500,10.743250&time=&date=&ttype=&saddr=L1042%2FLangensaltzaer+Strasse+%4051.125340,+10.480100&daddr=51.116994,10.723944+to:Friedhofsweg+%4050.944500,+10.743250&mra=dme&mrcr=0,1&mrsp=1&sz=14&sll=51.128953,10.722742&sspn=0.035766,0.079136&ie=UTF8&ll=51.021962,10.661545&spn=0.286792,0.633087&z=11&om=1";

    private static final String INPUT4_STRIPPED = "http://maps.google.de/maps?saddr=L1042%2FLangensaltzaer+Strasse+%4051.125340,+10.480100&daddr=51.116994,10.723944+to:Friedhofsweg+%4050.944500,+10.743250";

    private static final String INPUT5 = "http://maps.google.com/maps?f=d&hl=en&geocode=7153851080862447280,40.323122,-78.922058%3B658155100876861845,40.443995,-79.950354&saddr=326+Napoleon+St,+Johnstown,+PA+15901+(War+Memorial)&daddr=4400+Forbes+Ave,+Pittsburgh,+PA+15213+(Carnegie+Museums+)&mra=pe&mrcr=0&doflg=ptm&sll=40.412722,-79.572054&sspn=1.327888,2.39502&ie=UTF8&t=h&z=10";

    private static final String INPUT6 = "https://maps.google.com/maps?f=d&saddr=326+Napoleon+St,+Johnstown,+PA+15901+(War+Memorial)&daddr=40.159984,-78.980713+to:I-70+W%2FI-76+W%2FPennsylvania+Turnpike+%4040.064830,+-79.143020+to:PA-31+%4040.127779,+-79.434904+to:4400+Forbes+Ave,+Pittsburgh,+PA+15213+(Carnegie+Museums+)&hl=en&geocode=7153851080862447280,40.323122,-78.922058%3B12162090679892645334,40.064830,-79.143020%3B6752420830689506546,40.127779,-79.434904%3B658155100876861845,40.443995,-79.950354&mra=dpe&mrcr=0&mrsp=1&sz=9&via=1,2,3&sll=39.846504,-78.955994&sspn=1.524572,1.71936&ie=UTF8&z=9";

    private static final String INPUT7_GEOCODE = "http://maps.google.de/maps?f=d&saddr=Hamburg%2FUhlenhorst&daddr=Hauptstra%C3%9Fe%2FL160+to:53.588429,10.419159+to:Breitenfelde%2FNeuenlande&hl=de&geocode=%3BFVy1MQMdDoudAA%3B%3B&mra=dpe&mrcr=0&mrsp=2&sz=11&via=1,2&sll=53.582575,10.30528&sspn=0.234798,0.715485&ie=UTF8&z=11";

    private static final String INPUT8_WWW_NO_COORDINATES ="http://www.google.de/maps?f=d&source=s_d&saddr=hannover&daddr=hamburg&hl=de&geocode=&mra=ls&sll=51.151786,10.415039&sspn=20.697059,39.331055&ie=UTF8&z=9";

    private static final String INPUT9_NEW_GOOGLE_MAPS_2014 = "https://www.google.de/maps/dir/Aachen-Rothe+Erde/Mainz-Kastel,+Wiesbaden/Hanns-Martin-Schleyer-Stra%C3%9Fe,+Sindelfingen/@49.8065843,6.491479,8z/data=!3m1!4b1!4m20!4m19!1m5!1m1!1s0x47c09955de781093:0x8b975ed430fb3e53!2m2!1d6.116475!2d50.770202!1m5!1m1!1s0x47bd97a86ffd2e91:0xa4efa4fe12ce70c8!2m2!1d8.282168!2d50.0101878!1m5!1m1!1s0x4799dfcc3a4161f3:0xcd2a1bc2ee961675!2m2!1d9.0001511!2d48.7039074!3e0";

    private static final String INPUT10_NEW_GOOGLE_MAPS_2015 = "https://www.google.at/maps/dir/Sterzing,+Bozen,+Italien/Jaufenpass,+Sankt+Leonhard+in+Passeier,+Bozen,+Italien/Ofenpass,+7532+Cierfs,+Schweiz/Albulapassstrasse,+7482+Berg%C3%BCn%2FBravuogn,+Schweiz/Spl%C3%BCgenpass,+Spl%C3%BCgen,+Schweiz/Via+Roma,+53,+22023+Castiglione+CO,+Italien/@46.4115893,9.1625277,8z/data=!3m1!4b1!4m42!4m41!1m5!1m1!1s0x479d5340912b4fed:0xeccf91de29d6fcf9!2m2!1d11.4336186!2d46.8926725!1m5!1m1!1s0x4782b27a89809711:0xbfb1348a6269dc1!2m2!1d11.3214111!2d46.8395577!1m5!1m1!1s0x47831519f57d6e8b:0xa4a9db7fa9393319!2m2!1d10.2870515!2d46.6418315!1m5!1m1!1s0x47849d1f1792adb3:0x1b8f8898b9521cba!2m2!1d9.8000555!2d46.5815557!1m5!1m1!1s0x4784f6a1054aa397:0x1ffb7aed566559ff!2m2!1d9.33028!2d46.5056!1m5!1m1!1s0x478425a384f4a881:0xcec114200a27651!2m2!1d9.089271!2d45.95557!2m3!1b1!2b1!3b1!3e0";

    private GoogleMapsUrlFormat format = new GoogleMapsUrlFormat();

    @Test
    public void testFindURL() {
        String url = format.findURL(INPUT1_EMAIL);
        assertNotNull(url);
        assertTrue(url.startsWith("?f=d"));
        assertNull(format.findURL("don't care"));
    }

    @Test
    public void testParseStartPosition() {
        Wgs84Position position = format.parsePlainPosition("50.954318,7.311401");
        assertDoubleEquals(7.311401, position.getLongitude());
        assertDoubleEquals(50.954318, position.getLatitude());
    }

    @Test
    public void testParseNegativeStartPosition() {
        Wgs84Position position = format.parsePlainPosition("-50.954318,-7.311401");
        assertDoubleEquals(-7.311401, position.getLongitude());
        assertDoubleEquals(-50.954318, position.getLatitude());
    }

    @Test
    public void testParseDestinationPosition() {
        Wgs84Position position = format.parseCommentPosition("L339/Wuppertaler Strasse @50.918890,7.560880 ");
        assertDoubleEquals(7.560880, position.getLongitude());
        assertDoubleEquals(50.918890, position.getLatitude());
        assertEquals("L339/Wuppertaler Strasse", position.getDescription());
    }

    @Test
    public void testParseDestinationPositions() {
        List<Wgs84Position> positions = format.parseDestinationPositions("L339/Wuppertaler Strasse @50.918890,7.560880 to: B @ -1.1 , -2.2to:C@3.3,4.4");
        assertEquals(3, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertDoubleEquals(7.560880, position1.getLongitude());
        assertDoubleEquals(50.918890, position1.getLatitude());
        assertEquals("L339/Wuppertaler Strasse", position1.getDescription());
        Wgs84Position position2 = positions.get(1);
        assertDoubleEquals(-2.2, position2.getLongitude());
        assertDoubleEquals(-1.1, position2.getLatitude());
        assertEquals("B", position2.getDescription());
        Wgs84Position position3 = positions.get(2);
        assertDoubleEquals(4.4, position3.getLongitude());
        assertDoubleEquals(3.3, position3.getLatitude());
        assertEquals("C", position3.getDescription());
    }

    private List<Wgs84Position> parsePositions(String text) {
        String url = format.findURL(text);
        if (url.startsWith("/dir/")) {
            return format.parsePositions(url.substring(5));
        } else {
            Map<String, List<String>> parameters = format.parseURLParameters(url, "UTF-8");
            return format.parsePositions(parameters);

        }
    }

    @Test
    public void testParsePositionsFromInput1() {
        List<Wgs84Position> positions = parsePositions(INPUT1_EMAIL);
        assertNotNull(positions);
        assertEquals(6, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertNull(position1.getLongitude());
        assertNull(position1.getLatitude());
        assertEquals("H\u00f6lderlinstra\u00dfe, 51545 Br\u00f6l, Oberbergischer Kreis, Nordrhein-Westfalen, Deutschland", position1.getDescription());
        Wgs84Position position3 = positions.get(2);
        assertDoubleEquals(7.46395, position3.getLongitude());
        assertDoubleEquals(50.88518, position3.getLatitude());
        assertEquals("L350", position3.getDescription());
        Wgs84Position position6 = positions.get(5);
        assertNull(position6.getLongitude());
        assertNull(position6.getLatitude());
        assertEquals("K\u00f6ln, Riehler Str. 190", position6.getDescription());
    }

    @Test
    public void testParsePositionsFromInput2() {
        List<Wgs84Position> positions = parsePositions(INPUT2);
        assertNotNull(positions);
        assertEquals(3, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertNull(position1.getLongitude());
        assertNull(position1.getLatitude());
        assertEquals("51545 Waldbroel, Hoelderlinstr.", position1.getDescription());
        Wgs84Position position2 = positions.get(1);
        assertNull(position2.getLongitude());
        assertNull(position2.getLatitude());
        assertEquals("50389 Wesseling, Urfelder Strasse 221", position2.getDescription());
        Wgs84Position position3 = positions.get(2);
        assertDoubleEquals(6.962585, position3.getLongitude());
        assertDoubleEquals(50.876178, position3.getLatitude());
        assertNull(position3.getDescription());
    }

    @Test
    public void testParsePositionsFromInput3() {
        List<Wgs84Position> positions = parsePositions(INPUT3);
        assertNotNull(positions);
        assertEquals(2, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertNull(position1.getLongitude());
        assertNull(position1.getLatitude());
        assertEquals("L\u00fcbeck, Germany", position1.getDescription());
        Wgs84Position position2 = positions.get(1);
        assertNull(position2.getLongitude());
        assertNull(position2.getLatitude());
        assertEquals("Hamburg, Germany", position2.getDescription());
    }

    @Test
    public void testParsePositionsFromInput4() {
        List<Wgs84Position> positions = parsePositions(INPUT4);
        assertNotNull(positions);
        assertEquals(3, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertDoubleEquals(10.480100, position1.getLongitude());
        assertDoubleEquals(51.125340, position1.getLatitude());
        assertEquals("L1042/Langensaltzaer Strasse", position1.getDescription());
        Wgs84Position position2 = positions.get(1);
        assertDoubleEquals(10.723944, position2.getLongitude());
        assertDoubleEquals(51.116994, position2.getLatitude());
        assertNull(position2.getDescription());
        Wgs84Position position3 = positions.get(2);
        assertDoubleEquals(10.72092, position3.getLongitude());
        assertDoubleEquals(51.12645, position3.getLatitude());
        assertEquals("Friedhofsweg", position3.getDescription());
    }

    @Test
    public void testParsePositionsFromInput4Stripped() {
        List<Wgs84Position> positions = parsePositions(INPUT4_STRIPPED);
        assertNotNull(positions);
        assertEquals(3, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertDoubleEquals(10.480100, position1.getLongitude());
        assertDoubleEquals(51.125340, position1.getLatitude());
        assertEquals("L1042/Langensaltzaer Strasse", position1.getDescription());
        Wgs84Position position2 = positions.get(1);
        assertDoubleEquals(10.723944, position2.getLongitude());
        assertDoubleEquals(51.116994, position2.getLatitude());
        assertNull(position2.getDescription());
        Wgs84Position position3 = positions.get(2);
        assertDoubleEquals(10.74325, position3.getLongitude());
        assertDoubleEquals(50.9445, position3.getLatitude());
        assertEquals("Friedhofsweg", position3.getDescription());
    }

    @Test
    public void testParseGeocodePositionsFromInput5() {
        List<Wgs84Position> positions = parsePositions(INPUT5);
        assertNotNull(positions);
        assertEquals(2, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertDoubleEquals(-78.922058, position1.getLongitude());
        assertDoubleEquals(40.323122, position1.getLatitude());
        assertEquals("326 Napoleon St, Johnstown, PA 15901 (War Memorial)", position1.getDescription());
        Wgs84Position position2 = positions.get(1);
        assertDoubleEquals(-79.950354, position2.getLongitude());
        assertDoubleEquals(40.443995, position2.getLatitude());
        assertEquals("4400 Forbes Ave, Pittsburgh, PA 15213 (Carnegie Museums )", position2.getDescription());
    }

    @Test
    public void testParseGeocodePositionsWithViaFromInput6() {
        List<Wgs84Position> positions = parsePositions(INPUT6);
        assertNotNull(positions);
        assertEquals(5, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertDoubleEquals(-78.922058, position1.getLongitude());
        assertDoubleEquals(40.323122, position1.getLatitude());
        assertEquals("326 Napoleon St, Johnstown, PA 15901 (War Memorial)", position1.getDescription());
        Wgs84Position position2 = positions.get(2);
        assertDoubleEquals(-79.14302, position2.getLongitude());
        assertDoubleEquals(40.06483, position2.getLatitude());
        assertEquals("I-70 W/I-76 W/Pennsylvania Turnpike", position2.getDescription());
        Wgs84Position position3 = positions.get(3);
        assertDoubleEquals(-79.434904, position3.getLongitude());
        assertDoubleEquals(40.127779, position3.getLatitude());
        assertEquals("PA-31", position3.getDescription());
        Wgs84Position position4 = positions.get(4);
        assertDoubleEquals(-79.950354, position4.getLongitude());
        assertDoubleEquals(40.443995, position4.getLatitude());
        assertEquals("4400 Forbes Ave, Pittsburgh, PA 15213 (Carnegie Museums )", position4.getDescription());
    }

    @Test
    public void testParseEncodedGeocodePositionsFromInput7() {
        List<Wgs84Position> positions = parsePositions(INPUT7_GEOCODE);
        assertNotNull(positions);
        assertEquals(4, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertNull(position1.getLongitude());
        assertNull(position1.getLatitude());
        assertEquals("Hamburg/Uhlenhorst", position1.getDescription());
        Wgs84Position position2 = positions.get(2);
        assertDoubleEquals(10.419159, position2.getLongitude());
        assertDoubleEquals(53.588429, position2.getLatitude());
        assertEquals(null, position2.getDescription());
        Wgs84Position position3 = positions.get(3);
        assertEquals(null, position3.getLongitude());
        assertEquals(null, position3.getLatitude());
        assertEquals("Breitenfelde/Neuenlande", position3.getDescription());
    }

    @Test
    public void testParseWWWNoCoordinatesFromInput8() {
        List<Wgs84Position> positions = parsePositions(INPUT8_WWW_NO_COORDINATES);
        assertNotNull(positions);
        assertEquals(2, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertNull(position1.getLongitude());
        assertNull(position1.getLatitude());
        assertEquals("hannover", position1.getDescription());
        Wgs84Position position2 = positions.get(1);
        assertNull(position2.getLongitude());
        assertNull(position2.getLatitude());
        assertEquals("hamburg", position2.getDescription());
    }

    @Test
    public void testParseNewGoogleMaps2014FromInput9() {
        List<Wgs84Position> positions = parsePositions(INPUT9_NEW_GOOGLE_MAPS_2014);
        assertNotNull(positions);
        assertEquals(3, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertNull(position1.getLongitude());
        assertNull(position1.getLatitude());
        assertEquals("Aachen-Rothe Erde", position1.getDescription());
        Wgs84Position position2 = positions.get(1);
        assertNull(position2.getLongitude());
        assertNull(position2.getLatitude());
        assertEquals("Mainz-Kastel, Wiesbaden", position2.getDescription());
        Wgs84Position position3 = positions.get(2);
        assertNull(position3.getLongitude());
        assertNull(position3.getLatitude());
        assertEquals("Hanns-Martin-Schleyer-Stra\u00dfe, Sindelfingen", position3.getDescription());
    }

    @Test
    public void testParseNewGoogleMaps2015FromInput10() {
        List<Wgs84Position> positions = parsePositions(INPUT10_NEW_GOOGLE_MAPS_2015);
        assertNotNull(positions);
        assertEquals(6, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertNull(position1.getLongitude());
        assertNull(position1.getLatitude());
        assertEquals("Sterzing, Bozen, Italien", position1.getDescription());
        Wgs84Position position2 = positions.get(1);
        assertNull(position2.getLongitude());
        assertNull(position2.getLatitude());
        assertEquals("Jaufenpass, Sankt Leonhard in Passeier, Bozen, Italien", position2.getDescription());
        Wgs84Position position3 = positions.get(2);
        assertNull(position3.getLongitude());
        assertNull(position3.getLatitude());
        assertEquals("Ofenpass, 7532 Cierfs, Schweiz", position3.getDescription());
    }

    @Test
    public void testCreateURL() {
        List<Wgs84Position> positions = new ArrayList<>();
        positions.add(new Wgs84Position(10.02571156, 53.57497745, null, 5.5, null, "Hamburg, Germany"));
        positions.add(new Wgs84Position(10.20026067, 53.57662034, null, 4.5, null, "Stemwarde, Germany"));
        positions.add(new Wgs84Position(10.35735078, 53.59171021, null, 3.5, null, "Gro\u00dfensee, Germany"));
        positions.add(new Wgs84Position(10.45696089, 53.64781001, null, 2.5, null, "Linau, Germany"));
        String expected = "http://maps.google.com/maps?ie=UTF8&saddr=Hamburg,+Germany%4053.574977,10.025711&daddr=Stemwarde,+Germany%4053.576620,10.200260+to:Gro%C3%9Fensee,+Germany%4053.591710,10.357350+to:Linau,+Germany%4053.647810,10.456960";
        String actual = format.createURL(positions, 0, positions.size());
        assertEquals(expected, actual);
    }

    @Test
    public void testIsGoogleMapsLinkUrl() throws MalformedURLException {
        assertTrue(isGoogleMapsLinkUrl(new URL("https://maps.google.com/maps?saddr=Hamburg&daddr=Hannover+to:M%C3%BCnchen&hl=en&ie=UTF8&sll=50.844236,10.557014&sspn=6.272277,10.777588&geocode=Fe0fMQMd0n2YACm5Exh-g2GxRzGgOtZ78j0mBA%3BFVQxHwMdqn-UACmFT0lNUQuwRzEgR6yUbawlBA%3BFRCC3gIdsqWwACnZX4yj-XWeRzF9mLF9SrgMAQ&mra=ls&t=m&z=7")));
        assertTrue(isGoogleMapsLinkUrl(new URL("https://www.google.de/maps/dir/Hamburg/Hannover/M%C3%BCnchen/@50.8213415,8.3587982,7z/data=!3m1!4b1!4m20!4m19!1m5!1m1!1s0x47b161837e1813b9:0x4263df27bd63aa0!2m2!1d9.9936818!2d53.5510846!1m5!1m1!1s0x47b00b514d494f85:0x425ac6d94ac4720!2m2!1d9.7320104!2d52.3758916!1m5!1m1!1s0x479e75f9a38c5fd9:0x10cb84a7db1987d!2m2!1d11.5819806!2d48.1351253!3e0")));
    }

    @Test
    public void testIsGoogleMapsProfile() throws MalformedURLException {
        assertTrue(isGoogleMapsProfileUrl(new URL("http://maps.google.com/maps/ms?ie=UTF8&hl=de&oe=UTF8&num=200&start=37&msa=0&msid=215491296402946676738.000484ccfd83696d5b12e&z=11")));
        assertTrue(isGoogleMapsProfileUrl(new URL("https://maps.google.com/maps/ms?msa=0&msid=218347962219071576267.0004e1131e8ad4ef4fd9b")));
    }
}
