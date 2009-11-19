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

import slash.navigation.NavigationTestCase;
import slash.navigation.Wgs84Position;

public class Nmn6FormatTest extends NavigationTestCase {
    Nmn6Format format = new Nmn6Format();

    public void testIsPosition() {
        assertTrue(format.isPosition("[|][0][10]|||8.8128300|49.0006140[0]||"));
        assertTrue(format.isPosition("[|][1][11]|||8.8128300|49.0006140[0]||"));
        assertTrue(format.isPosition("[Comment|][2][12]|||8.8128300|49.0006140[0]||"));
        assertTrue(format.isPosition("[Comment|][2][12]|||-8.8128300|-49.0006140[0]||"));
        assertTrue(format.isPosition("[COMMENT|][2][12]|||-8.8128300|-49.0006140[0]||"));
        assertTrue(format.isPosition("[D 22081,Hamburg/Uhlenhorst,Finkenau,0,|][0][10]|||10.03200|53.56949"));
        assertTrue(format.isPosition("[||][0][10]|7.89442,50.57314|56459|7.89442|50.57314[8]|L304|56459|7.88732|50.57211[6]|ROTHENBACH|56459|7.90153|50.56437[3]|WESTERWALDKREIS|[2]|Rheinland-Pfalz||4363[0]|Deutschland||17"));
        assertFalse(format.isPosition("[HYGIENE4YOU|UserWords3|][0][10]|15.43511,47.07848||15.43511|47.07848[8]|WICKENBURGGASSE|8010|15.43655|47.07876[6]|GRAZ|8010|15.44273|47.06833[3]|GRAZ|[2]|Steiermark||1030[0]|Österreich||4"));
        assertFalse(format.isPosition("[||][2]"));
        assertFalse(format.isPosition("[|][0][10]||8.8128300|49.0006140[0]||"));
    }

    public void testIsValidLine() {
        assertTrue(format.isValidLine("[||][2]"));
        assertTrue(format.isValidLine("[15:29:65; {6'} Schlosswiese 1-3, Ratzeburg|][0][10]|||10.03200|53.56949"));
        assertTrue(format.isValidLine("[15:29:65; {6'} Schlosswiese 1-3, Ratzeburg|][0][10]|||-10.03200|-53.56949"));
        assertTrue(format.isValidLine("[15:29:65; {6'} Schlosswiese 1-3, Ratzeburg|][0][10]|||-10.76617|-53.69928||"));

        assertFalse(format.isValidLine("a"));
        assertFalse(format.isValidLine("[]"));
    }

    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("[|][0][10]|||8.8128300|49.0006140[0]||", null);
        assertEquals(8.8128300, position.getLongitude());
        assertEquals(49.0006140, position.getLatitude());
        assertNull(position.getComment());
    }

    public void testParseNegativePosition() {
        Wgs84Position position = format.parsePosition("[|][0][10]|||-8.8128300|-49.0006140[0]||", null);
        assertEquals(-8.8128300, position.getLongitude());
        assertEquals(-49.0006140, position.getLatitude());
        assertNull(position.getComment());
    }

    public void testParseITNConvPosition() {
        Wgs84Position position = format.parsePosition("[D 22081,Hamburg/Uhlenhorst,Finkenau,0,|][0][10]|||10.03200|53.56949", null);
        assertEquals(10.03200, position.getLongitude());
        assertEquals(53.56949, position.getLatitude());
        assertEquals("D 22081,Hamburg/Uhlenhorst,Finkenau,0,", position.getComment());
    }

    public void testParseWrittenFormat() {
        Wgs84Position position = format.parsePosition("[Rheinuferstr. bei Kaub|][0][10]|||7.74957|50.09721", null);
        assertEquals(7.74957, position.getLongitude());
        assertEquals(50.09721, position.getLatitude());
        assertEquals("Rheinuferstr. bei Kaub", position.getComment());
    }

    public void testParseNavigonFormat() {
        Wgs84Position position = format.parsePosition("[Rheinuferstr. bei Kaub||][0][10]|7.74957,50.09721||7.74957|50.09721[6]|KAUB|56349|7.76240|50.08817[3]|RHEIN-LAHN-KREIS|[2]|Rheinland-Pfalz||4363[0]|Deutschland||17", null);
        assertEquals(7.74957, position.getLongitude());
        assertEquals(50.09721, position.getLatitude());
        assertEquals("Rheinuferstr. bei Kaub", position.getComment());

        Wgs84Position position2 = format.parsePosition("[||][0][10]|B42|56348|7.65285|50.17757[6]|KESTERT|56348|7.64715|50.18503[3]|RHEIN-LAHN-KREIS|[2]|Rheinland-Pfalz||4363[0]|Deutschland||17", null);
        assertEquals(7.65285, position2.getLongitude());
        assertEquals(50.17757, position2.getLatitude());
        assertNull(position2.getComment());
    }

    public void testParsePOIonDeviceFormat() {
        Wgs84Position position = format.parsePosition("[||][0][10]|B42|56112|7.62424|50.29042[7]|OBERLAHNSTEIN|[6]|LAHNSTEIN|56112|7.60183|50.31752[3]|RHEIN-LAHN-KREIS|[2]|Rheinland-Pfalz||4363[0]|Deutschland||17[Rheinuferstr. bei Kaub||][0][10]|7.74957,50.09721||7.74957|50.09721[6]|KAUB|56349|7.76240|50.08817[3]|RHEIN-LAHN-KREIS|[2]|Rheinland-Pfalz||4363[0]|Deutschland||17", null);
        assertEquals(7.62424, position.getLongitude());
        assertEquals(50.29042, position.getLatitude());
        assertNull(position.getComment());
    }
}
