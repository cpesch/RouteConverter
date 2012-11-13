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

import slash.common.type.CompactCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import static java.util.Calendar.DATE;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static slash.common.type.CompactCalendar.UTC;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * The base of all XML based navigation formats.
 *
 * @author Christian Pesch
 */

public abstract class XmlNavigationFormat<R extends BaseRoute> extends BaseNavigationFormat<R> {
    public static final String HEADER_LINE = "<!-- " + GENERATED_BY + " -->\n";

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    protected String asDescription(List<String> strings) {
        StringBuilder buffer = new StringBuilder();
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
        GregorianCalendar gregorianCalendar = calendar.toGregorianCalendar(UTC, null, null);
        return fromMillis(gregorianCalendar.getTimeInMillis());
    }

    private static DatatypeFactory datatypeFactory = null;

    private static synchronized DatatypeFactory getDataTypeFactory() throws DatatypeConfigurationException {
        if (datatypeFactory == null) {
            datatypeFactory = DatatypeFactory.newInstance();
        }
        return datatypeFactory;
    }

    @SuppressWarnings("MagicConstant")
    public static XMLGregorianCalendar formatTime(CompactCalendar time) {
        if (time == null)
            return null;
        try {
            GregorianCalendar gregorianCalendar = new GregorianCalendar(UTC, Locale.getDefault());
            gregorianCalendar.clear();
            Calendar calendar = time.getCalendar();
            gregorianCalendar.set(calendar.get(YEAR), calendar.get(MONTH), calendar.get(DATE),
                    calendar.get(HOUR_OF_DAY), calendar.get(MINUTE), calendar.get(SECOND));
            gregorianCalendar.set(MILLISECOND, calendar.get(MILLISECOND));
            return getDataTypeFactory().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            return null;
        }
    }
}
