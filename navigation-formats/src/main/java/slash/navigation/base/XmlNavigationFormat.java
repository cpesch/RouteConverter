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

import slash.common.io.CompactCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

/**
 * The base of all XML based navigation formats.
 *
 * @author Christian Pesch
 */

public abstract class XmlNavigationFormat<R extends BaseRoute> extends BaseNavigationFormat<R> {
    public static final String HEADER = "<!-- " + GENERATED_BY + " -->\n";

    protected String asDescription(List<String> strings) {
        StringBuffer buffer = new StringBuffer();
        if (strings != null) {
            for (String string : strings) {
                buffer.append(string).append(",\n");
            }
        }
        if (buffer.indexOf(GENERATED_BY) == -1)
            buffer.append(GENERATED_BY);
        return buffer.toString();
    }

    public static CompactCalendar parseTime(XMLGregorianCalendar calendar) {
        if (calendar == null)
            return null;
        // by using TimeZone default, the original hours are not corrupted
        return CompactCalendar.fromCalendar(calendar.toGregorianCalendar(TimeZone.getDefault(), null, null));
    }

    private static DatatypeFactory datatypeFactory = null;

    private static synchronized DatatypeFactory getDataTypeFactory() throws DatatypeConfigurationException {
        if (datatypeFactory == null) {
            datatypeFactory = DatatypeFactory.newInstance();
        }
        return datatypeFactory;
    }

    public static XMLGregorianCalendar formatTime(CompactCalendar time) {
        if (time == null)
            return null;
        try {
            // by using GMT no timezone is written
            GregorianCalendar gregorianCalendar = new GregorianCalendar(CompactCalendar.GMT, Locale.getDefault());
            gregorianCalendar.clear();
            Calendar calendar = time.getCalendar();
            gregorianCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE),
                    calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
            gregorianCalendar.set(Calendar.MILLISECOND, calendar.get(Calendar.MILLISECOND));
            return getDataTypeFactory().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            return null;
        }
    }
}
