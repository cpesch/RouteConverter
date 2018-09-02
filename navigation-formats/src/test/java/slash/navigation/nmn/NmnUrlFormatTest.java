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

package slash.navigation.nmn;

import org.junit.Test;
import slash.navigation.base.Wgs84Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;

public class NmnUrlFormatTest {
    private static final String TARGET_COORDINATE_URL = "<a href=\"navigonDEU://route/?target=coordinate//7.273085/51.411354&amp;target=coordinate//7.305422/51.410168&amp;target=coordinate//7.340269/51.428253&amp;target=coordinate//7.388512/51.407173&amp;target=coordinate//7.455643/51.422134&amp;target=coordinate//7.506969/51.422310&amp;target=coordinate//7.538937/51.405663&amp;target=coordinate//7.591082/51.411518&amp;target=coordinate//7.654833/51.411278&amp;target=coordinate//7.677935/51.402477&amp;target=coordinate//7.720705/51.407127&amp;target=coordinate//7.746279/51.434059&amp;target=coordinate//7.838520/51.438408&amp;target=coordinate//7.911991/51.426926&amp;target=coordinate//7.932663/51.411076&amp;target=coordinate//7.935606/51.377350&amp;target=coordinate//7.933142/51.357689&amp;target=coordinate//7.942778/51.343704&amp;target=coordinate//8.006210/51.285271&amp;target=coordinate//8.079313/51.290840&amp;target=coordinate//8.079969/51.346352&amp;target=coordinate//8.055506/51.392109&amp;target=coordinate//8.076488/51.437889&amp;target=coordinate//8.118202/51.483131&amp;target=coordinate//8.163364/51.491737\">51ö24'41\"N, 07ö16'23\"O, Kömpenstrasse, 58456 Witten ? 51ö24'37\"N, 07ö18'20\"O, Rauendahlstrasse, 58452 Witten ? 51ö25'42\"N, 07ö20'25\"O, Ruhrdeich, 58452 Witten ? 51ö24'26\"N, 07ö23'19\"O, Ender Talstrasse, 58313 Herdecke ? 51ö25'20\"N, 07ö27'20\"O, Wittbröucker Strasse, 58313 Herdecke ? 51ö25'20\"N, 07ö30'25\"O, Westhofener Strasse, 44265 Dortmund ? 51ö24'20\"N, 07ö32'20\"O, Ruhrtalstrasse, 58239 Schwerte ? 51ö24'41\"N, 07ö35'28\"O, Steinberg, 58239 Schwerte ? 51ö24'41\"N, 07ö39'17\"O, Schirrnbergstrasse, 58640 Iserlohn ? 51ö24'09\"N, 07ö40'41\"O, Leckingser Strasse, 58640 Iserlohn ? 51ö24'26\"N, 07ö43'15\"O, Landhauser Strasse, 58640 Iserlohn ? 51ö26'03\"N, 07ö44'47\"O, Bröukerweg, 58708 Menden (Sauerland) ? 51ö26'18\"N, 07ö50'19\"O, K21, 58708 Menden (Sauerland) ? 51ö25'37\"N, 07ö54'43\"O, Bieberstrasse, 59757 Arnsberg ? 51ö24'40\"N, 07ö55'58\"O, K1, 59757 Arnsberg ? 51ö22'38\"N, 07ö56'08\"O, L544, 59846 Sundern (Sauerland) ? 51ö21'28\"N, 07ö55'59\"O, L544, 59846 Sundern (Sauerland) ? 51ö20'37\"N, 07ö56'34\"O, L687, 59846 Sundern (Sauerland) ? 51ö17'07\"N, 08ö00'22\"O, L842, 59846 Sundern (Sauerland) ? 51ö17'27\"N, 08ö04'46\"O, K24, 59846 Sundern (Sauerland) ? 51ö20'47\"N, 08ö04'48\"O, L839, 59846 Sundern (Sauerland) ? 51ö23'32\"N, 08ö03'20\"O, Altes Feld, 59821 Arnsberg ? 51ö26'16\"N, 08ö04'35\"O, K8, 59823 Arnsberg ? 51ö28'59\"N, 08ö07'06\"O, L857, 59519 Möhnesee ? 51ö29'30\"N, 08ö09'48\"O, Seeuferstrasse, 59519 Möhnesee</a>";
    private static final String TARGET_ADDRESS_URL = "<a href=\"navigonDEU://route/?target=address//DEU/44797/BOCHUM/UNTERM%20KOLM/11/7.23153/51.43851&amp;target=address//DEU/44227/DORTMUND/MARTIN-SCHMEISSER-WEG/8/7.40361/51.49144\">Unterm Kolm 11, 44797 Bochum ? Martin-Schmeisser-Weg 8, 44227 Dorstfeld, Dortmund</a>";
    private static final String TARGET_ADDRESS_URL2 = "<a\n" +
            "href=\"navigonDEU://route/?target=address//DEU/79539/L%C3%96RRACH/BAHNHOFSPLATZ//7.66474/47.61403/////BAHNHOF%20L%C3%96RRACH&amp;target=coordinate//7.804354/47.537415&amp;target=coordinate//7.899921/47.467670&amp;target=coordinate//8.053896/46.813164&amp;target=address//CHE/3863/GADMEN/SUSTENPASS//8.44719/46.72941/////SUSTENPASS&amp;target=address//CHE/7482/BERG%C3%9CN%252FBRAVUOGN/ALBULA//9.79694/46.57997&amp;target=coordinate//10.379776/46.466843&amp;target=coordinate//10.581059/46.258007&amp;target=address//AUT/1050/WIEN/MARGARETENG%C3%9CRTEL//16.35795/48.18033/////BAHNHOF%20WIEN%20MATZLEINSDORFER%20PLATZ\">Bahnhof\n" +
            "Lörrach, Bahnhofsplatz, 79539 Lörrach ? 47ö32'15&quot;N, 07ö48'16&quot;O,\n" +
            "Hauptstrasse, 4312 Magden ? 47ö28'04&quot;N, 07ö54'00&quot;O,\n" +
            "Ormalingerstrasse, 4467 Rothenfluh ? 46ö48'47&quot;N, 08ö03'14&quot;O,\n" +
            "Panoramastrasse, 6173 Flöhli ? Sustenpass, Sustenpass, 3863 Sustenpass, Gadmen\n" +
            "? Albula, 7482 Preda, Bergön/Bravuogn ? 46ö28'01&quot;N, 10ö22'47&quot;O, Via\n" +
            "Monte Cristallo, 23032 Bormio ? 46ö15'29&quot;N, 10ö34'52&quot;O, Passo Del\n" +
            "Tonale, 25056 Ponte Di Legno ? Bahnhof Wien Matzleinsdorfer Platz,\n" +
            "Margaretengörtel, 1050 5. Bezirk-Margareten, Wien</a>";
    private static final String USA_URL = "navigonUSA-CA://route/?target=address//USA-CA/CA%2092120/SAN%20DIEGO/ALVARADO%20CANYON%20RD/4620/-117.09521/32.78042&amp;target=address//USA-NV/NV%2089101/LAS%20VEGAS///-115.13997/36.17191";
    private static final String NO_MAP_URL = "<a href=\"navigon://route/?target=address//DEU/44797/BOCHUM/UNTERM%20KOLM/11/7.23153/51.43851&amp;target=address//DEU/44227/DORTMUND/MARTIN-SCHMEISSER-WEG/8/7.40361/51.49144\">Unterm Kolm 11, 44797 Bochum ? Martin-Schmeisser-Weg 8, 44227 Dorstfeld, Dortmund</a>";

    private NmnUrlFormat format = new NmnUrlFormat();

    @Test
    public void testFindURL() {
        String url = format.findURL(TARGET_COORDINATE_URL);
        assertNotNull(url);
        assertTrue(url.startsWith("target=coordinate"));
        assertNull(format.findURL("don't care"));
    }

    @Test
    public void testCoordinatesPosition() {
        Wgs84Position position = format.parsePosition("coordinate//7.273085/51.411354");
        assertDoubleEquals(7.273085, position.getLongitude());
        assertDoubleEquals(51.411354, position.getLatitude());
    }

    @Test
    public void testAddressPosition() {
        Wgs84Position position = format.parsePosition("address//DEU/44797/BOCHUM/UNTERM%20KOLM/11/7.23153/51.43851");
        assertDoubleEquals(7.23153, position.getLongitude());
        assertDoubleEquals(51.43851, position.getLatitude());
        assertEquals("44797 Bochum, Unterm Kolm 11", position.getDescription());
    }

    private List<Wgs84Position> parsePositions(String text) {
        String url = format.findURL(text);
        Map<String, List<String>> parameters = format.parseURLParameters(url, "UTF-8");
        return format.parsePositions(parameters);
    }

    @Test
    public void testParsePositionsFromTargetCoordinateUrl() {
        List<Wgs84Position> positions = parsePositions(TARGET_COORDINATE_URL);
        assertNotNull(positions);
        assertEquals(25, positions.size());
        Wgs84Position position6 = positions.get(5);
        assertDoubleEquals(7.506969, position6.getLongitude());
        assertDoubleEquals(51.42231, position6.getLatitude());
        assertNull(position6.getDescription());
    }

    @Test
    public void testParsePositionsFromTargetAddressUrl() {
        List<Wgs84Position> positions = parsePositions(TARGET_ADDRESS_URL);
        assertNotNull(positions);
        assertEquals(2, positions.size());
        Wgs84Position position2 = positions.get(1);
        assertDoubleEquals(7.40361, position2.getLongitude());
        assertDoubleEquals(51.49144, position2.getLatitude());
        assertEquals("44227 Dortmund, Martin-Schmeisser-Weg 8", position2.getDescription());
    }

    @Test
    public void testParsePositionsFromTargetAddressUrl2() {
        List<Wgs84Position> positions = parsePositions(TARGET_ADDRESS_URL2);
        assertNotNull(positions);
        assertEquals(9, positions.size());
        Wgs84Position position4 = positions.get(4);
        assertDoubleEquals(8.44719, position4.getLongitude());
        assertDoubleEquals(46.72941, position4.getLatitude());
        assertEquals("3863 Gadmen, Sustenpass", position4.getDescription());
    }

    @Test
    public void testParsePositionsFromUsaUrl() {
        List<Wgs84Position> positions = parsePositions(USA_URL);
        assertNotNull(positions);
        assertEquals(2, positions.size());
        Wgs84Position position2 = positions.get(1);
        assertDoubleEquals(-115.13997, position2.getLongitude());
        assertDoubleEquals(36.17191, position2.getLatitude());
        assertEquals("Nv 89101 Las Vegas", position2.getDescription());
    }

    @Test
    public void testParsePositionsFromNoMapUrl() {
        List<Wgs84Position> positions = parsePositions(NO_MAP_URL);
        assertNotNull(positions);
        assertEquals(2, positions.size());
        Wgs84Position position2 = positions.get(1);
        assertDoubleEquals(7.40361, position2.getLongitude());
        assertDoubleEquals(51.49144, position2.getLatitude());
        assertEquals("44227 Dortmund, Martin-Schmeisser-Weg 8", position2.getDescription());
    }

    @Test
    public void testCreateDeURL() {
        List<Wgs84Position> positions = new ArrayList<>();
        positions.add(new Wgs84Position(10.02571156, 53.57497745, null, 5.5, null, "Hamburg, Germany"));
        positions.add(new Wgs84Position(10.20026067, 53.57662034, null, 4.5, null, "Stemwarde, Germany"));
        positions.add(new Wgs84Position(10.35735078, 53.59171021, null, 3.5, null, "Groöensee, Germany"));
        positions.add(new Wgs84Position(10.45696089, 53.64781001, null, 2.5, null, "Linau, Germany"));
        String expected = "navigon://route/?target=coordinate//10.025711/53.574977&target=coordinate//10.200260/53.576620&target=coordinate//10.357350/53.591710&target=coordinate//10.456960/53.647810";
        String actual = format.createURL(positions, 0, positions.size());
        assertEquals(expected, actual);
    }

    @Test
    public void testCreateUsaURL() {
        List<Wgs84Position> positions = new ArrayList<>();
        positions.add(new Wgs84Position(-113.240014, 36.114526, 1134.0, null, null, "Grand Canyon, Arizona, USA"));
        positions.add(new Wgs84Position(-115.139973, 53.574977, 648.0, null, null, "Las Vegas, Nevada, USA"));
        String expected = "navigon://route/?target=coordinate//-113.240014/36.114526&target=coordinate//-115.139973/53.574977";
        String actual = format.createURL(positions, 0, positions.size());
        assertEquals(expected, actual);
    }
}
