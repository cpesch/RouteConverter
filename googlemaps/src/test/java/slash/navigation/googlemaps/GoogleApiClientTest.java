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

package slash.navigation.googlemaps;

import org.junit.Test;
import slash.navigation.rest.exception.ServiceUnavailableException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class GoogleApiClientTest {
    private static final String URL_WITHOUT_KEY = "https://maps.googleapis.com/maps/api/geocode/xml?address=Mundsburg&sensor=false&language=de";

    @Test
    public void checkForErrorKeepsApiKeyOutOfLogAndException() {
        List<String> messages = new ArrayList<>();
        Logger logger = Logger.getLogger(GoogleApiClient.class.getName());
        Handler handler = new Handler() {
            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            public void flush() {
            }

            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            for (String status : new String[]{"REQUEST_DENIED", "OVER_QUERY_LIMIT"}) {
                try {
                    new GoogleApiClient().checkForError("GoogleGeocodingService", URL_WITHOUT_KEY + "&key=secret-api-key", status);
                    fail("Expected ServiceUnavailableException for " + status);
                } catch (ServiceUnavailableException e) {
                    assertEquals(URL_WITHOUT_KEY, e.getServiceUrl());
                    assertFalse(e.toString(), e.toString().contains("secret-api-key"));
                }
            }
        } finally {
            logger.removeHandler(handler);
        }

        assertEquals(2, messages.size());
        for (String message : messages)
            assertFalse(message, message.contains("secret-api-key"));
    }
}
