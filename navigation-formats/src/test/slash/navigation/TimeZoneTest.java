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

package slash.navigation;

import slash.navigation.gpx.GpxPosition;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class TimeZoneTest extends NavigationTestCase {

    public void testGMTAndLocalTimeZone() {
        long now = System.currentTimeMillis();
        Calendar local = calendar(now);
        Calendar utc = utcCalendar(now);

        String localTime = DateFormat.getInstance().format(local.getTime().getTime());
        String utcTime = DateFormat.getInstance().format(utc.getTime().getTime());
        assertEquals(localTime, utcTime);
        assertNotEquals(local, utc);

        local.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertCalendarEquals(local, utc);
    }

    public void testXMLGregorianCalendarViaDatatypeFactory() throws DatatypeConfigurationException {
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        XMLGregorianCalendar xml = datatypeFactory.newXMLGregorianCalendar("2007-06-07T14:04:42Z");
        GregorianCalendar java = xml.toGregorianCalendar(TimeZone.getDefault(), null, null);
        String javaTime = DateFormat.getInstance().format(java.getTime().getTime());
        assertEquals("07.06.07 14:04", javaTime);
        Calendar parsed = XmlNavigationFormat.parseTime(xml);
        assertEquals(parsed, java);
    }

    public void testXMLGregorianCalendarWithZasTimeZone() throws DatatypeConfigurationException {
        String xmlString = "2007-06-07T14:04:42Z";
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        XMLGregorianCalendar xml = datatypeFactory.newXMLGregorianCalendar(xmlString);
        assertEquals("2007-06-07T14:04:42Z", xml.toXMLFormat());
        GregorianCalendar java = xml.toGregorianCalendar(TimeZone.getDefault(), null, null);
        XMLGregorianCalendar formatted = XmlNavigationFormat.formatTime(java);
        assertEquals("2007-06-07T14:04:42.000Z", formatted.toXMLFormat());
    }

    public void testTimeZone() {
        long now = System.currentTimeMillis();
        Calendar local = calendar(now);
        Calendar utc = utcCalendar(now);
        assertNotEquals(local, utc);

        GpxPosition gpxPosition = new GpxPosition(3.0, 2.0, 1.0, local, "gpx");
        assertCalendarEquals(utc, gpxPosition.getTime());
    }
}
