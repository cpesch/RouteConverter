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

package slash.navigation.rest.exception;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RestExceptionsTest {

    // --- DuplicateNameException ---

    @Test
    public void testDuplicateNameExceptionMessageContainsBothParts() {
        DuplicateNameException ex = new DuplicateNameException("Route already exists", "https://api.routeconverter.com/routes/123");
        String msg = ex.getMessage();
        assertTrue(msg.contains("Route already exists"));
        assertTrue(msg.contains("https://api.routeconverter.com/routes/123"));
    }

    @Test
    public void testDuplicateNameExceptionMessageSeparatedByNewline() {
        DuplicateNameException ex = new DuplicateNameException("Duplicate", "http://example.com");
        String msg = ex.getMessage();
        assertTrue(msg.contains("\n"));
    }

    // --- ForbiddenException ---

    @Test
    public void testForbiddenExceptionMessageContainsBothParts() {
        ForbiddenException ex = new ForbiddenException("Not allowed", "https://api.routeconverter.com/routes/42");
        String msg = ex.getMessage();
        assertTrue(msg.contains("Not allowed"));
        assertTrue(msg.contains("https://api.routeconverter.com/routes/42"));
    }

    @Test
    public void testForbiddenExceptionIsRuntimeException() {
        ForbiddenException ex = new ForbiddenException("forbidden", "http://example.com");
        assertTrue(ex instanceof RuntimeException);
    }

    // --- UnAuthorizedException ---

    @Test
    public void testUnAuthorizedExceptionMessageContainsBothParts() {
        UnAuthorizedException ex = new UnAuthorizedException("Login required", "https://api.routeconverter.com/");
        String msg = ex.getMessage();
        assertTrue(msg.contains("Login required"));
        assertTrue(msg.contains("https://api.routeconverter.com/"));
    }

    @Test
    public void testUnAuthorizedExceptionIsRuntimeException() {
        assertTrue(new UnAuthorizedException("msg", "url") instanceof RuntimeException);
    }

    // --- ServiceUnavailableException ---

    @Test
    public void testServiceUnavailableExceptionMessage() {
        ServiceUnavailableException ex = new ServiceUnavailableException("GeoNames", "https://api.geonames.org/", "quota exceeded");
        String msg = ex.getMessage();
        assertTrue(msg.contains("GeoNames"));
        assertTrue(msg.contains("https://api.geonames.org/"));
        assertTrue(msg.contains("quota exceeded"));
    }

    @Test
    public void testServiceUnavailableExceptionGetters() {
        ServiceUnavailableException ex = new ServiceUnavailableException("Photon", "https://photon.komoot.de/", "500");
        assertEquals("Photon", ex.getServiceName());
        assertEquals("https://photon.komoot.de/", ex.getServiceUrl());
    }

    @Test
    public void testServiceUnavailableExceptionToStringContainsServiceName() {
        ServiceUnavailableException ex = new ServiceUnavailableException("Nominatim", "https://nominatim.openstreetmap.org/", "429");
        String s = ex.toString();
        assertTrue(s.contains("Nominatim"));
    }

    @Test
    public void testServiceUnavailableExceptionIsIOException() {
        ServiceUnavailableException ex = new ServiceUnavailableException("X", "http://x.com", "err");
        assertTrue(ex instanceof java.io.IOException);
    }
}

