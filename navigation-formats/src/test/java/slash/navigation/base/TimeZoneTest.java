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

package slash.navigation.base;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import slash.common.io.Transfer;
import slash.common.type.CompactCalendar;
import slash.navigation.gpx.GpxPosition;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static com.jcabi.matchers.RegexMatchers.matchesPattern;
import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.*;
import static slash.common.io.Transfer.parseXMLTime;
import static slash.common.type.CompactCalendar.fromCalendar;

public class TimeZoneTest {

    @Test
    public void testGMTAndLocalTimeZone() {
        long now = System.currentTimeMillis();
        Calendar local = localCalendar(now).getCalendar();
        Calendar utc = utcCalendar(now).getCalendar();

        DateFormat format = DateFormat.getInstance();
        format.setTimeZone(CompactCalendar.UTC);
        String localTime = format.format(local.getTime().getTime());
        String utcTime = format.format(utc.getTime().getTime());
        assertEquals(localTime, utcTime);

        local.setTimeZone(CompactCalendar.UTC);
        assertCalendarEquals(local, utc);
    }

    @Test
    public void testXMLGregorianCalendarViaDatatypeFactory() throws DatatypeConfigurationException {
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        XMLGregorianCalendar xml = datatypeFactory.newXMLGregorianCalendar("2007-06-07T14:04:42Z");
        GregorianCalendar java = xml.toGregorianCalendar();
        assertEquals(TimeZone.getTimeZone("GMT+00:00"), java.getTimeZone());
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.ENGLISH);
        dateFormat.setTimeZone(java.getTimeZone());
        assertEquals(TimeZone.getTimeZone("GMT+00:00"), dateFormat.getTimeZone());
        String javaTime = dateFormat.format(java.getTime().getTime());
        MatcherAssert.assertThat(javaTime, matchesPattern("6/7/07,? 2:04 PM"));
        Calendar parsed = parseXMLTime(xml).getCalendar();
        assertEquals(TimeZone.getTimeZone("UTC"), parsed.getTimeZone());
        assertCalendarEquals(parsed, java);
    }

    @Test
    public void testXMLGregorianCalendarWithTimeZoneViaDatatypeFactory() throws DatatypeConfigurationException {
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        XMLGregorianCalendar xml = datatypeFactory.newXMLGregorianCalendar("2007-06-07T14:04:42+02:00");
        GregorianCalendar java = xml.toGregorianCalendar();
        assertEquals(TimeZone.getTimeZone("GMT+02:00"), java.getTimeZone());
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.ENGLISH);
        dateFormat.setTimeZone(java.getTimeZone());
        String javaTime = dateFormat.format(java.getTime().getTime());
        MatcherAssert.assertThat(javaTime, matchesPattern("6/7/07,? 2:04 PM"));
        Calendar parsed = parseXMLTime(xml).getCalendar();
        assertEquals(TimeZone.getTimeZone("UTC"), parsed.getTimeZone());
        java.roll(Calendar.HOUR, 2);
        assertCalendarEquals(parsed, java);
    }

    @Test
    public void testXMLGregorianCalendarWithZasTimeZone() throws DatatypeConfigurationException {
        String xmlString = "2007-06-07T14:04:42Z";
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        XMLGregorianCalendar xml = datatypeFactory.newXMLGregorianCalendar(xmlString);
        assertEquals("2007-06-07T14:04:42Z", xml.toXMLFormat());
        GregorianCalendar java = xml.toGregorianCalendar(TimeZone.getDefault(), null, null);
        XMLGregorianCalendar formatted = Transfer.formatXMLTime(fromCalendar(java));
        assertEquals("2007-06-07T14:04:42.000Z", formatted.toXMLFormat());
    }

    @Test
    public void testXMLGregorianCalendarWithTimeZone() throws DatatypeConfigurationException {
        String xmlString = "2007-06-07T14:04:42+02:00";
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        XMLGregorianCalendar xml = datatypeFactory.newXMLGregorianCalendar(xmlString);
        assertEquals("2007-06-07T14:04:42+02:00", xml.toXMLFormat());
        GregorianCalendar java = xml.toGregorianCalendar(TimeZone.getDefault(), null, null);
        XMLGregorianCalendar formatted = Transfer.formatXMLTime(fromCalendar(java));
        assertEquals("2007-06-07T14:04:42.000Z", formatted.toXMLFormat());
    }

    @Test
    public void testTimeZone() {
        long now = System.currentTimeMillis();
        Calendar local = localCalendar(now).getCalendar();
        CompactCalendar compactLocal = fromCalendar(local);
        Calendar utc = utcCalendar(now).getCalendar();
        CompactCalendar compactUtc = fromCalendar(utc);

        GpxPosition gpxPosition = new GpxPosition(3.0, 2.0, 1.0, null, compactLocal, "gpx");
        assertCalendarEquals(compactUtc, gpxPosition.getTime());
    }
}
