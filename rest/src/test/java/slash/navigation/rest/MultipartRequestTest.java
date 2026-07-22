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

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Test;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MultipartRequestTest {
    // a multi-byte character proves the body is encoded as UTF-8, not the platform charset
    private static final String BODY = "{\"schema_version\":1,\"note\":\"café résumé\"}";

    @Test
    public void testRawBodyIsSentVerbatimAsUtf8StringEntity() throws IOException {
        Post request = new Post("http://localhost/");
        request.setBody(BODY, APPLICATION_JSON);
        request.prepareEntity();

        HttpEntity entity = request.getMethod().getEntity();
        assertTrue("a raw body must produce a StringEntity", entity instanceof StringEntity);

        byte[] expected = BODY.getBytes(UTF_8);
        assertArrayEquals(expected, entity.getContent().readAllBytes());
        assertEquals(expected.length, entity.getContentLength());
        assertTrue("content type " + entity.getContentType(),
                entity.getContentType().contains("application/json"));
        assertTrue("content type " + entity.getContentType(),
                entity.getContentType().toUpperCase().contains("UTF-8"));
    }

    @Test
    public void testRawBodyTakesPrecedenceOverAddStringParts() throws IOException {
        Post request = new Post("http://localhost/");
        // a multipart part is added first, then a raw body: the raw body must win so the
        // server receives exactly the bytes it will sign-verify
        request.addString("part", "ignored-multipart-part");
        request.setBody(BODY, APPLICATION_JSON);
        request.prepareEntity();

        HttpEntity entity = request.getMethod().getEntity();
        assertTrue(entity instanceof StringEntity);
        byte[] actual = entity.getContent().readAllBytes();
        assertArrayEquals(BODY.getBytes(UTF_8), actual);
        assertFalse("the multipart part must not leak into a raw body request",
                new String(actual, UTF_8).contains("ignored-multipart-part"));
    }

    @Test
    public void testAddStringEncodesUmlautsAsUtf8TextPlain() throws IOException {
        Post request = new Post("http://localhost/");
        request.addString("description", "Zwischenpunkt einfügen: Straße, Grüße");
        request.prepareEntity();

        byte[] payload = request.getMethod().getEntity().getContent().readAllBytes();
        String asLatin1 = new String(payload, java.nio.charset.StandardCharsets.ISO_8859_1);
        // the part must be text/plain with a UTF-8 charset, so servers decode umlauts correctly
        assertTrue("part must be text/plain, was:\n" + asLatin1, asLatin1.contains("text/plain"));
        assertTrue("part charset must be UTF-8, was:\n" + asLatin1, asLatin1.toUpperCase().contains("UTF-8"));
        // "ü" must appear as its UTF-8 bytes (0xC3 0xBC), not a single ISO-8859-1 byte (0xFC)
        assertTrue("umlaut must be UTF-8 encoded",
                indexOf(payload, "ü".getBytes(UTF_8)) >= 0);
        assertFalse("umlaut must not be ISO-8859-1 encoded",
                indexOf(payload, "ü".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)) >= 0);
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++)
                if (haystack[i + j] != needle[j])
                    continue outer;
            return i;
        }
        return -1;
    }
}
