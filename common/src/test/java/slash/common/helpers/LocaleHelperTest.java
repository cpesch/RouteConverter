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

package slash.common.helpers;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static slash.common.helpers.LocaleHelper.resolveDomain;

/**
 * Tests for {@link LocaleHelper}.
 *
 * @author Christian Pesch
 */

public class LocaleHelperTest {

    @Test
    public void testResolveDomainForGermany() {
        assertEquals("https://www.routeconverter.de", resolveDomain(Locale.GERMANY));
    }

    @Test
    public void testResolveDomainForGermanLanguageOtherCountry() {
        assertEquals("https://www.routeconverter.de", resolveDomain(Locale.of("de", "AT")));
    }

    @Test
    public void testResolveDomainIsCaseInsensitive() {
        assertEquals("https://www.routeconverter.de", resolveDomain(Locale.forLanguageTag("DE")));
    }

    @Test
    public void testResolveDomainForUs() {
        assertEquals("https://www.routeconverter.com", resolveDomain(Locale.US));
    }

    @Test
    public void testResolveDomainForOtherLanguage() {
        assertEquals("https://www.routeconverter.com", resolveDomain(Locale.FRANCE));
    }

    @Test
    public void testResolveDomainForNullLocale() {
        assertEquals("https://www.routeconverter.com", resolveDomain(null));
    }
}
