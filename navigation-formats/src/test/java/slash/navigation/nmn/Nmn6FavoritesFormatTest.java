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

import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;

public class Nmn6FavoritesFormatTest {
    private Nmn6FavoritesFormat format = new Nmn6FavoritesFormat();

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("[P HALLENEU CINEMAXX||][0][10]|11.92417,51.47978|06122|11.92417|51.47978[8]|NEUSTÄDTER PASSAGE|06122|11.92678|51.48087[7]|NEUSTADT|[6]|HALLE (SAALE)|06108|11.97546|51.48129[3]|HALLE (SAALE)|[2]|Sachsen-Anhalt||4366[0]|Deutschland||4"));
        assertTrue(format.isPosition("[P SCHIERKE||][0][15]|AM THÄLCHEN|38879|10.66664|51.76459|633,0|1[14]|Alle Kategorien||196658,0[13]|Parken||3,0[6]|SCHIERKE|38879|10.65527|51.76586[3]|WERNIGERODE|[2]|Sachsen-Anhalt||4366[0]|Deutschland||4"));
        assertTrue(format.isPosition("[JET MÄNCHEN||][0][15]|JET|80687|11.52046|48.14122|316,0|2[14]|Jet||131102,0[13]|Tankstelle||2,0[6]|MÄNCHEN|80331|11.57732|48.13649[3]|MÄNCHEN (STADT)|[2]|Bayern|Bayern|4354[0]|Deutschland||4"));
        assertTrue(format.isPosition("[WIESN||][0][8]|THERESIENWIESE|80336|11.54970|48.13577[6]|MÄNCHEN|80331|11.57732|48.13649[3]|MÄNCHEN (STADT)|[2]|Bayern|Bayern|4354[0]|Deutschland||4"));
        assertTrue(format.isPosition("[IRGENDWO||][1][17]|ÄUSSERE BAYREUTHER STRASSE|90491|11.09851|49.46748[16]|48|[8]|ÄUSSERE BAYREUTHER STRASSE|90411|11.12481|49.48861[6]|NÄRNBERG|90403|11.07394|49.45432[3]|NÄRNBERG|[2]|Bayern|Bayern|4354[0]|Deutschland||4"));
        assertTrue(format.isPosition("[HYGIENE4YOU|][0][10]|||15.43511|47.07848|||15.43511|47.07848|HYGIENE4YOU||HYGIENE4YOU||15.43511|47.07848|HYGIENE4YOU|||||||4"));
        assertTrue(format.isPosition("[HYGIENE4YOU||][0][10]|15.43511,47.07848||15.43511|47.07848[8]|WICKENBURGGASSE|8010|15.43655|47.07876[6]|GRAZ|8010|15.44273|47.06833[3]|GRAZ|[2]|Steiermark||1030[0]|Ästerreich||4"));
        assertTrue(format.isPosition("[||][0][10]|7.89442,50.57314|56459|7.89442|50.57314[8]|L304|56459|7.88732|50.57211[6]|ROTHENBACH|56459|7.90153|50.56437[3]|WESTERWALDKREIS|[2]|Rheinland-Pfalz||4363[0]|Deutschland||4"));
        assertFalse(format.isPosition("[||][0][10]|7.89442,50.57314|56459|7.89442|50.57314[8]|L304|56459|7.88732|50.57211[6]|ROTHENBACH|56459|7.90153|50.56437[3]|WESTERWALDKREIS|[2]|Rheinland-Pfalz||4363[0]|Deutschland||17"));
        assertFalse(format.isPosition("[HYGIENE4YOU|UserWords3|][0][10]|15.43511,47.07848||15.43511|47.07848[8]|WICKENBURGGASSE|8010|15.43655|47.07876[6]|GRAZ|8010|15.44273|47.06833[3]|GRAZ|[2]|Steiermark||1030[0]|Ästerreich||4"));
        assertFalse(format.isPosition("[Hygiene4You||][0][10]|15.43511,47.07848||15.43511|47.07848[8]|WICKENBURGGASSE|8010|15.43655|47.07876[6]|GRAZ|8010|15.44273|47.06833[3]|GRAZ|[2]|Steiermark||1030[0]|Ästerreich||4"));
        assertFalse(format.isPosition("[Hygiene4You||][0][10]|15.43511,47.07848||15.43511|47.07848[8]|Wickenburggasse|8010|15.43655|47.07876[6]|GRAZ|8010|15.44273|47.06833[3]|GRAZ|[2]|Steiermark||1030[0]|Ästerreich||4"));
        assertFalse(format.isPosition("[Hygiene4You||][0][10]|15.43511,47.07848||15.43511|47.07848[8]|Wickenburggasse|8010|15.43655|47.07876[6]|Graz|8010|15.44273|47.06833[3]|GRAZ|[2]|Steiermark||1030[0]|Ästerreich||4"));
        assertFalse(format.isPosition("[Hygiene4You||][0][10]|15.43511,47.07848||15.43511|47.07848[8]|Wickenburggasse|8010|15.43655|47.07876[6]|Graz|8010|15.44273|47.06833[3]|Graz|[2]|Steiermark||1030[0]|Ästerreich||4"));
    }

    @Test
    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("[P HALLENEU CINEMAXX||][0][10]|11.92517,51.47558|06122|11.92417|51.47978[8]|NEUSTAEDTER PASSAGE|06122|11.92978|51.48097[7]|NEUSTADT|[6]|HALLE (SAALE)|06108|11.99546|51.49129[3]|HALLE (SAALE)|[2]|Sachsen-Anhalt||4366[0]|Deutschland||4", null);
        assertDoubleEquals(11.92417, position.getLongitude());
        assertDoubleEquals(51.47978, position.getLatitude());
        assertEquals("P Halleneu Cinemaxx, Neustaedter Passage", position.getDescription());
    }
}
