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

package slash.navigation.rest;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpRequestTest {
    @Test
    public void withoutQueryDropsApiKeysAndSearchTerms() throws Exception {
        assertEquals("https://maps.googleapis.com/maps/api/geocode/xml",
                HttpRequest.withoutQuery(new URI("https://maps.googleapis.com/maps/api/geocode/xml?address=Mundsburg&key=secret-api-key")));
        assertEquals("https://api.routeconverter.com/v1/datasources",
                HttpRequest.withoutQuery(new URI("https://api.routeconverter.com/v1/datasources")));
    }

    @Test
    public void hasCredentialsIsFalseForNullCredentials() {
        assertFalse(HttpRequest.hasCredentials(null));
    }

    @Test
    public void hasCredentialsIsFalseForNullUserName() {
        assertFalse(HttpRequest.hasCredentials(new SimpleCredentials(null, "pw".toCharArray())));
    }

    @Test
    public void hasCredentialsIsFalseForEmptyUserName() {
        assertFalse(HttpRequest.hasCredentials(new SimpleCredentials("", "pw".toCharArray())));
    }

    @Test
    public void hasCredentialsIsFalseForBlankUserName() {
        assertFalse(HttpRequest.hasCredentials(new SimpleCredentials("  ", "pw".toCharArray())));
    }

    @Test
    public void hasCredentialsIsFalseForNullPassword() {
        assertFalse(HttpRequest.hasCredentials(new SimpleCredentials("user", null)));
    }

    @Test
    public void hasCredentialsIsTrueForUserNameAndPassword() {
        assertTrue(HttpRequest.hasCredentials(new SimpleCredentials("user", "pw".toCharArray())));
    }

    @Test
    public void hasCredentialsIsTrueForUserNameAndEmptyPassword() {
        assertTrue(HttpRequest.hasCredentials(new SimpleCredentials("user", "".toCharArray())));
    }
}
