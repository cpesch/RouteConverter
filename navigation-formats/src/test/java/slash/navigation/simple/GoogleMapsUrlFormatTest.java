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

package slash.navigation.simple;

import slash.navigation.base.NavigationTestCase;
import slash.navigation.base.Wgs84Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GoogleMapsUrlFormatTest extends NavigationTestCase {
    private static final String INPUT1 = "Betreff: Route nach/zu Riehler Straﬂe 190 50735 Kˆln (Google Maps)\n" +
            "\n" +
            "> Routenplaner\n" +
            "> Link:\n" +
            "<http://maps.google.de/maps?f=d&hl=de&geocode=&saddr=H%C3%B6lderlinstra%C3%9Fe,+51545+Br%C3%B6l,+Oberbergischer+Kreis,+Nordrhein-Westfalen,+Deutschland&daddr=L339%2FWuppertaler+Stra%C3%9Fe+%4050.918890,+7.560880+to%3AL350+%4050.885180,+7.463950+to%3AB%C3%B6vingen%2FK11+%4050.917200,+7.376600+to%3AL312+%4050.916380,+7.327030+to%3AK%C3%B6ln,+Riehler+Str.+190&mrcr=2&mra=mr&sll=50.954318,7.311401&sspn=0.142091,0.32135&ie=UTF8&ll=50.952371,7.261276&spn=0.284193,0.6427&z=11&om=1>\n" +
            ">\n" +
            "> Startadresse: Hˆlderlinstraﬂe 51545 Brˆl\n" +
            "> Zieladresse: Riehler Straﬂe 190 50735 Kˆln";

    private static final String INPUT2 = "http://maps.google.de/maps?f=d&hl=de&geocode=&saddr=51545+Waldbroel,+Hoelderlinstr.&daddr=50389+Wesseling,+Urfelder+Strasse+221+to%3A50.876178,6.962585&mrcr=1&mrsp=2&sz=10&mra=mi&sll=50.892745,7.312145&sspn=0.569114,1.2854&ie=UTF8&z=10&om=1";

    private static final String INPUT3 = "http://maps.google.com/maps?f=d&hl=de&geocode=&time=&date=&ttype=&saddr=L%C3%BCbeck,+Germany&daddr=Hamburg,+Germany&sll=37.0625,-95.677068&sspn=48.374125,76.464844&ie=UTF8&z=10&om=1";

    private static final String INPUT4 = "http://maps.google.de/maps?f=d&hl=de&geocode=17223560710991701360,51.125340,10.480100%3B12158345081209133212,51.126450,10.720920%3B7678232323906648676,50.944500,10.743250&time=&date=&ttype=&saddr=L1042%2FLangensaltzaer+Straﬂe+%4051.125340,+10.480100&daddr=51.116994,10.723944+to:Friedhofsweg+%4050.944500,+10.743250&mra=dme&mrcr=0,1&mrsp=1&sz=14&sll=51.128953,10.722742&sspn=0.035766,0.079136&ie=UTF8&ll=51.021962,10.661545&spn=0.286792,0.633087&z=11&om=1";

    private static final String INPUT4_STRIPPED = "http://maps.google.de/maps?saddr=L1042%2FLangensaltzaer+Straﬂe+%4051.125340,+10.480100&daddr=51.116994,10.723944+to:Friedhofsweg+%4050.944500,+10.743250";

    private static final String INPUT5 = "http://maps.google.com/maps?f=d&hl=en&geocode=7153851080862447280,40.323122,-78.922058%3B658155100876861845,40.443995,-79.950354&saddr=326+Napoleon+St,+Johnstown,+PA+15901+(War+Memorial)&daddr=4400+Forbes+Ave,+Pittsburgh,+PA+15213+(Carnegie+Museums+)&mra=pe&mrcr=0&doflg=ptm&sll=40.412722,-79.572054&sspn=1.327888,2.39502&ie=UTF8&t=h&z=10";

    private static final String INPUT6 = "http://maps.google.com/maps?f=d&saddr=326+Napoleon+St,+Johnstown,+PA+15901+(War+Memorial)&daddr=40.159984,-78.980713+to:I-70+W%2FI-76+W%2FPennsylvania+Turnpike+%4040.064830,+-79.143020+to:PA-31+%4040.127779,+-79.434904+to:4400+Forbes+Ave,+Pittsburgh,+PA+15213+(Carnegie+Museums+)&hl=en&geocode=7153851080862447280,40.323122,-78.922058%3B12162090679892645334,40.064830,-79.143020%3B6752420830689506546,40.127779,-79.434904%3B658155100876861845,40.443995,-79.950354&mra=dpe&mrcr=0&mrsp=1&sz=9&via=1,2,3&sll=39.846504,-78.955994&sspn=1.524572,1.71936&ie=UTF8&z=9";

    private static final String INPUT7_GEOCODE = "http://maps.google.de/maps?f=d&saddr=Hamburg%2FUhlenhorst&daddr=Hauptstra%C3%9Fe%2FL160+to:53.588429,10.419159+to:Breitenfelde%2FNeuenlande&hl=de&geocode=%3BFVy1MQMdDoudAA%3B%3B&mra=dpe&mrcr=0&mrsp=2&sz=11&via=1,2&sll=53.582575,10.30528&sspn=0.234798,0.715485&ie=UTF8&z=11";

    private static final String INPUT8_WWW_NO_COORDINATES ="http://www.google.de/maps?f=d&source=s_d&saddr=hannover&daddr=hamburg&hl=de&geocode=&mra=ls&sll=51.151786,10.415039&sspn=20.697059,39.331055&ie=UTF8&z=9";

    GoogleMapsUrlFormat urlFormat = new GoogleMapsUrlFormat();

    public void testFindURL() {
        String url = GoogleMapsUrlFormat.findURL(INPUT1);
        assertNotNull(url);
        assertTrue(url.startsWith("f=d"));
        assertNull(GoogleMapsUrlFormat.findURL("don't care"));
    }

    public void testParseSingleURLParameters() {
        Map<String, List<String>> parameters = urlFormat.parseURLParameters("f=d", "ISO8859-1");
        assertNotNull(parameters);
        List<String> values = parameters.get("f");
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals("d", values.get(0));
    }

    public void testParseTwoURLParameters() {
        Map<String, List<String>> parameters = urlFormat.parseURLParameters("f=d&g=e", "UTF-8");
        assertNotNull(parameters);
        List<String> fValues = parameters.get("f");
        assertNotNull(fValues);
        assertEquals(1, fValues.size());
        assertEquals("d", fValues.get(0));
        List<String> gValues = parameters.get("g");
        assertNotNull(gValues);
        assertEquals(1, gValues.size());
        assertEquals("e", gValues.get(0));
    }

    public void testParseSetURLParameters() {
        Map<String, List<String>> parameters = urlFormat.parseURLParameters("f=d&f=e", "ISO8859-1");
        assertNotNull(parameters);
        List<String> fValues = parameters.get("f");
        assertNotNull(fValues);
        assertEquals(2, fValues.size());
        assertEquals("d", fValues.get(0));
        assertEquals("e", fValues.get(1));
    }

    public void testParseURLParameters() {
        String url = GoogleMapsUrlFormat.findURL(INPUT1);
        Map<String, List<String>> parameters = urlFormat.parseURLParameters(url, "UTF-8");
        assertNotNull(parameters);
        List<String> fValues = parameters.get("f");
        assertNotNull(fValues);
        assertEquals(1, fValues.size());
        assertEquals("d", fValues.get(0));
    }

    public void testParseStartPosition() {
        Wgs84Position position = urlFormat.parsePlainPosition("50.954318,7.311401");
        assertEquals(7.311401, position.getLongitude());
        assertEquals(50.954318, position.getLatitude());
    }

    public void testParseNegativeStartPosition() {
        Wgs84Position position = urlFormat.parsePlainPosition("-50.954318,-7.311401");
        assertEquals(-7.311401, position.getLongitude());
        assertEquals(-50.954318, position.getLatitude());
    }

    public void testParseDestinationPosition() {
        Wgs84Position position = urlFormat.parseCommentPosition("L339/Wuppertaler Straﬂe @50.918890,7.560880 ");
        assertEquals(7.560880, position.getLongitude());
        assertEquals(50.918890, position.getLatitude());
        assertEquals("L339/Wuppertaler Straﬂe", position.getComment());
    }

    public void testParseDestinationPositions() {
        List<Wgs84Position> positions = urlFormat.parseDestinationPositions("L339/Wuppertaler Straﬂe @50.918890,7.560880 to: B @ -1.1 , -2.2to:C@3.3,4.4");
        assertEquals(3, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertEquals(7.560880, position1.getLongitude());
        assertEquals(50.918890, position1.getLatitude());
        assertEquals("L339/Wuppertaler Straﬂe", position1.getComment());
        Wgs84Position position2 = positions.get(1);
        assertEquals(-2.2, position2.getLongitude());
        assertEquals(-1.1, position2.getLatitude());
        assertEquals("B", position2.getComment());
        Wgs84Position position3 = positions.get(2);
        assertEquals(4.4, position3.getLongitude());
        assertEquals(3.3, position3.getLatitude());
        assertEquals("C", position3.getComment());
    }

    public void testParsePositionsFromInput1() {
        String url = GoogleMapsUrlFormat.findURL(INPUT1);
        Map<String, List<String>> parameters = urlFormat.parseURLParameters(url, "UTF-8");
        List<Wgs84Position> positions = urlFormat.parsePositions(parameters);
        assertNotNull(positions);
        assertEquals(6, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertNull(position1.getLongitude());
        assertNull(position1.getLatitude());
        assertEquals("Hˆlderlinstraﬂe, 51545 Brˆl, Oberbergischer Kreis, Nordrhein-Westfalen, Deutschland", position1.getComment());
        Wgs84Position position3 = positions.get(2);
        assertEquals(7.46395, position3.getLongitude());
        assertEquals(50.88518, position3.getLatitude());
        assertEquals("L350", position3.getComment());
        Wgs84Position position6 = positions.get(5);
        assertNull(position6.getLongitude());
        assertNull(position6.getLatitude());
        assertEquals("Kˆln, Riehler Str. 190", position6.getComment());
    }

    public void testParsePositionsFromInput2() {
        String url = GoogleMapsUrlFormat.findURL(INPUT2);
        Map<String, List<String>> parameters = urlFormat.parseURLParameters(url, "ISO8859-1");
        List<Wgs84Position> positions = urlFormat.parsePositions(parameters);
        assertNotNull(positions);
        assertEquals(3, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertNull(position1.getLongitude());
        assertNull(position1.getLatitude());
        assertEquals("51545 Waldbroel, Hoelderlinstr.", position1.getComment());
        Wgs84Position position2 = positions.get(1);
        assertNull(position2.getLongitude());
        assertNull(position2.getLatitude());
        assertEquals("50389 Wesseling, Urfelder Strasse 221", position2.getComment());
        Wgs84Position position3 = positions.get(2);
        assertEquals(6.962585, position3.getLongitude());
        assertEquals(50.876178, position3.getLatitude());
        assertNull(position3.getComment());
    }

    public void testParsePositionsFromInput3() {
        String url = GoogleMapsUrlFormat.findURL(INPUT3);
        Map<String, List<String>> parameters = urlFormat.parseURLParameters(url, "ISO8859-1");
        List<Wgs84Position> positions = urlFormat.parsePositions(parameters);
        assertNotNull(positions);
        assertEquals(2, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertNull(position1.getLongitude());
        assertNull(position1.getLatitude());
        assertEquals("L√ºbeck, Germany", position1.getComment());
        Wgs84Position position2 = positions.get(1);
        assertNull(position2.getLongitude());
        assertNull(position2.getLatitude());
        assertEquals("Hamburg, Germany", position2.getComment());
    }

    public void testParsePositionsFromInput4() {
        String url = GoogleMapsUrlFormat.findURL(INPUT4);
        Map<String, List<String>> parameters = urlFormat.parseURLParameters(url, "ISO8859-1");
        List<Wgs84Position> positions = urlFormat.parsePositions(parameters);
        assertNotNull(positions);
        assertEquals(3, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertEquals(10.480100, position1.getLongitude());
        assertEquals(51.125340, position1.getLatitude());
        assertEquals("L1042/Langensaltzaer Straﬂe", position1.getComment());
        Wgs84Position position2 = positions.get(1);
        assertEquals(10.723944, position2.getLongitude());
        assertEquals(51.116994, position2.getLatitude());
        assertNull(position2.getComment());
        Wgs84Position position3 = positions.get(2);
        assertEquals(10.72092, position3.getLongitude());
        assertEquals(51.12645, position3.getLatitude());
        assertEquals("Friedhofsweg", position3.getComment());
    }

    public void testParsePositionsFromInput4Stripped() {
        String url = GoogleMapsUrlFormat.findURL(INPUT4_STRIPPED);
        Map<String, List<String>> parameters = urlFormat.parseURLParameters(url, "ISO8859-1");
        List<Wgs84Position> positions = urlFormat.parsePositions(parameters);
        assertNotNull(positions);
        assertEquals(3, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertEquals(10.480100, position1.getLongitude());
        assertEquals(51.125340, position1.getLatitude());
        assertEquals("L1042/Langensaltzaer Straﬂe", position1.getComment());
        Wgs84Position position2 = positions.get(1);
        assertEquals(10.723944, position2.getLongitude());
        assertEquals(51.116994, position2.getLatitude());
        assertNull(position2.getComment());
        Wgs84Position position3 = positions.get(2);
        assertEquals(10.74325, position3.getLongitude());
        assertEquals(50.9445, position3.getLatitude());
        assertEquals("Friedhofsweg", position3.getComment());
    }

    public void testParseGeocodePositionsFromInput5() {
        String url = GoogleMapsUrlFormat.findURL(INPUT5);
        Map<String, List<String>> parameters = urlFormat.parseURLParameters(url, "ISO8859-1");
        List<Wgs84Position> positions = urlFormat.parsePositions(parameters);
        assertNotNull(positions);
        assertEquals(2, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertEquals(-78.922058, position1.getLongitude());
        assertEquals(40.323122, position1.getLatitude());
        assertEquals("326 Napoleon St, Johnstown, PA 15901 (War Memorial)", position1.getComment());
        Wgs84Position position2 = positions.get(1);
        assertEquals(-79.950354, position2.getLongitude());
        assertEquals(40.443995, position2.getLatitude());
        assertEquals("4400 Forbes Ave, Pittsburgh, PA 15213 (Carnegie Museums )", position2.getComment());
    }

    public void testParseGeocodePositionsWithViaFromInput6() {
        String url = GoogleMapsUrlFormat.findURL(INPUT6);
        Map<String, List<String>> parameters = urlFormat.parseURLParameters(url, "ISO8859-1");
        List<Wgs84Position> positions = urlFormat.parsePositions(parameters);
        assertNotNull(positions);
        assertEquals(5, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertEquals(-78.922058, position1.getLongitude());
        assertEquals(40.323122, position1.getLatitude());
        assertEquals("326 Napoleon St, Johnstown, PA 15901 (War Memorial)", position1.getComment());
        Wgs84Position position2 = positions.get(2);
        assertEquals(-79.14302, position2.getLongitude());
        assertEquals(40.06483, position2.getLatitude());
        assertEquals("I-70 W/I-76 W/Pennsylvania Turnpike", position2.getComment());
        Wgs84Position position3 = positions.get(3);
        assertEquals(-79.434904, position3.getLongitude());
        assertEquals(40.127779, position3.getLatitude());
        assertEquals("PA-31", position3.getComment());
        Wgs84Position position4 = positions.get(4);
        assertEquals(-79.950354, position4.getLongitude());
        assertEquals(40.443995, position4.getLatitude());
        assertEquals("4400 Forbes Ave, Pittsburgh, PA 15213 (Carnegie Museums )", position4.getComment());
    }

    public void testParseEncodedGeocodePositionsFromInput7() {
        String url = GoogleMapsUrlFormat.findURL(INPUT7_GEOCODE);
        Map<String, List<String>> parameters = urlFormat.parseURLParameters(url, "ISO8859-1");
        List<Wgs84Position> positions = urlFormat.parsePositions(parameters);
        assertNotNull(positions);
        assertEquals(4, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertNull(position1.getLongitude());
        assertNull(position1.getLatitude());
        assertEquals("Hamburg/Uhlenhorst", position1.getComment());
        Wgs84Position position2 = positions.get(2);
        assertEquals(10.419159, position2.getLongitude());
        assertEquals(53.588429, position2.getLatitude());
        assertEquals(null, position2.getComment());
        Wgs84Position position3 = positions.get(3);
        assertEquals(null, position3.getLongitude());
        assertEquals(null, position3.getLatitude());
        assertEquals("Breitenfelde/Neuenlande", position3.getComment());
    }

    public void testParseWWWNoCoordinatesFromInput8() {
        String url = GoogleMapsUrlFormat.findURL(INPUT8_WWW_NO_COORDINATES);
        Map<String, List<String>> parameters = urlFormat.parseURLParameters(url, "ISO8859-1");
        List<Wgs84Position> positions = urlFormat.parsePositions(parameters);
        assertNotNull(positions);
        assertEquals(2, positions.size());
        Wgs84Position position1 = positions.get(0);
        assertNull(position1.getLongitude());
        assertNull(position1.getLatitude());
        assertEquals("hannover", position1.getComment());
        Wgs84Position position2 = positions.get(1);
        assertNull(position2.getLongitude());
        assertNull(position2.getLatitude());
        assertEquals("hamburg", position2.getComment());
    }

    public void testCreateURL() {
        List<Wgs84Position> positions = new ArrayList<Wgs84Position>();
        positions.add(new Wgs84Position(53.57497745, 10.02571156, null, 5.5, null, "Hamburg, Germany"));
        positions.add(new Wgs84Position(53.57662034, 10.20026067, null,4.5, null, "Stemwarde, Germany"));
        positions.add(new Wgs84Position(53.59171021, 10.35735078, null,3.5, null, "Groﬂensee, Germany"));
        positions.add(new Wgs84Position(53.64781001, 10.45696089, null,2.5, null, "Linau, Germany"));
        String expected = "http://maps.google.com/maps?ie=UTF8&saddr=Hamburg,+Germany%4010.025711,53.574977&daddr=Stemwarde,+Germany%4010.200260,53.576620+to:Gro%C3%9Fensee,+Germany%4010.357350,53.591710+to:Linau,+Germany%4010.456960,53.647810";
        String actual = urlFormat.createURL(positions, 0, positions.size());
        assertEquals(expected, actual);
    }
}
