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

package slash.common.log;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.common.log.CredentialRedaction.redact;

public class CredentialRedactionTest {

    @Test
    public void testRedactIsNoOpWithoutCredentials() {
        assertNull(redact(null));
        assertEquals("nothing sensitive here", redact("nothing sensitive here"));
    }

    @Test
    public void testRedactsApiKeyFromTileUrl() {
        String redacted = redact("Cannot load tile https://tile.thunderforest.com/cycle/1/2/3.png?apikey=abc123def456");
        assertTrue(redacted.contains("apikey=<redacted>"));
        assertFalse(redacted.contains("abc123def456"));
    }

    @Test
    public void testRedactsApiKeyWithNestedUrlValue() {
        String redacted = redact("failed for ?apikey=https%3A%2F%2Fevil.example%2Fcallback%3Fx%3D1");
        assertTrue(redacted.contains("apikey=<redacted>"));
        assertFalse(redacted.contains("evil.example"));
    }

    @Test
    public void testRedactsAccessTokenAmongOtherParams() {
        String redacted = redact("GET https://api.example.com/v1?foo=bar&access_token=secretvalue123&baz=qux");
        assertTrue(redacted.contains("access_token=<redacted>"));
        assertFalse(redacted.contains("secretvalue123"));
        assertTrue(redacted.contains("foo=bar"));
        assertTrue(redacted.contains("baz=qux"));
    }

    @Test
    public void testRedactsJsessionIdMatrixParameter() {
        String redacted = redact("https://example.com/app;jsessionid=1234567890ABCDEF?x=1");
        assertTrue(redacted.contains("jsessionid=<redacted>"));
        assertFalse(redacted.contains("1234567890ABCDEF"));
    }

    @Test
    public void testRedactsBearerAuthMaterial() {
        String redacted = redact("Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.secret.token");
        assertTrue(redacted.contains("Bearer <redacted>"));
        assertFalse(redacted.contains("eyJhbGciOiJIUzI1NiJ9"));
    }

    @Test
    public void testRedactsBasicAuthMaterial() {
        String redacted = redact("Authorization: Basic dXNlcjpwYXNzd29yZA==");
        assertTrue(redacted.contains("Basic <redacted>"));
        assertFalse(redacted.contains("dXNlcjpwYXNzd29yZA"));
    }

    @Test
    public void testRedactsUserPasswordFromUrlAuthority() {
        String redacted = redact("Cannot connect to https://user:password@example.com/path");
        assertTrue(redacted.contains("https://<redacted>@example.com/path"));
        assertFalse(redacted.contains("user:password"));
    }

    @Test
    public void testRedactsHyphenatedParameterSpellingsLikeTheServer() {
        String redacted = redact("GET https://api.example.com/v1?api-key=secret1"
                + "&access-token=secret2&client-secret=secret3&clientsecret=secret4&x=1");
        assertTrue(redacted.contains("api-key=<redacted>"));
        assertTrue(redacted.contains("access-token=<redacted>"));
        assertTrue(redacted.contains("client-secret=<redacted>"));
        assertTrue(redacted.contains("clientsecret=<redacted>"));
        assertTrue(redacted.contains("x=1"));
        for (String secret : new String[]{"secret1", "secret2", "secret3", "secret4"})
            assertFalse(redacted.contains(secret), secret);
    }

    @Test
    public void testRedactKeepsTheWordAfterBasicOrBearerInPlainText() {
        assertEquals("a basic error happened", redact("a basic error happened"));
        assertEquals("Bearer token missing", redact("Bearer token missing"));
    }

    @Test
    public void testRedactLeavesUnrelatedMessagesIntact() {
        assertEquals("Could not parse longitude 477286.7334", redact("Could not parse longitude 477286.7334"));
        assertEquals("monkey=banana", redact("monkey=banana"));
        assertEquals("?keyboard=1", redact("?keyboard=1"));
    }
}
