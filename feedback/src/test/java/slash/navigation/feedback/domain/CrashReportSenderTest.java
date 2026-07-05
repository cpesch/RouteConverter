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

package slash.navigation.feedback.domain;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.feedback.domain.CrashReportSender.MAXIMUM_BYTES;
import static slash.navigation.feedback.domain.CrashReportSender.SECRET_PROPERTY;
import static slash.navigation.feedback.domain.CrashReportSender.warnIfDefaultSecret;

public class CrashReportSenderTest {

    @After
    public void tearDown() {
        System.clearProperty(SECRET_PROPERTY);
        CrashReportSender.resetDefaultSecretWarningForTesting();
    }

    @Test
    public void testSignatureMatchesKnownVector() throws Exception {
        // reference value computed with: printf '%s' '{"schema_version":1}' |
        //   openssl dgst -sha256 -hmac 'test-secret-key'
        String signature = CrashReportSender.sign("test-secret-key", "{\"schema_version\":1}".getBytes(UTF_8));
        assertEquals("5d79e3584425269286b5edff9d925f452dc23101cb269b06ecf007737fb66478", signature);
    }

    @Test
    public void testOversizePayloadIsSkipped() {
        final boolean[] posted = {false};
        CrashReportSender sender = new CrashReportSender() {
            boolean post(String url, String json, String signature) {
                posted[0] = true;
                return true;
            }
        };

        StringBuilder builder = new StringBuilder();
        while (builder.length() <= MAXIMUM_BYTES)
            builder.append("x");

        assertFalse(sender.send("https://api.routeconverter.com/", builder.toString()));
        assertFalse("an oversized payload must not be posted", posted[0]);
    }

    @Test
    public void testFailsSilentlyOnTransportError() {
        CrashReportSender sender = new CrashReportSender() {
            boolean post(String url, String json, String signature) throws IOException {
                throw new IOException("simulated transport error");
            }
        };

        // no exception must propagate; the failed send simply reports false
        assertFalse(sender.send("https://api.routeconverter.com/", "{\"schema_version\":1}"));
    }

    @Test
    public void testWarnsOnceWhenSecretIsPlaceholder() {
        System.clearProperty(SECRET_PROPERTY);
        CrashReportSender.resetDefaultSecretWarningForTesting();

        // the committed placeholder is still in effect: warn exactly once
        assertTrue(warnIfDefaultSecret());
        assertFalse("the placeholder warning must not repeat", warnIfDefaultSecret());
        assertFalse(warnIfDefaultSecret());
    }

    @Test
    public void testDoesNotWarnWhenRealSecretInjected() {
        System.setProperty(SECRET_PROPERTY, "a-real-injected-secret");
        CrashReportSender.resetDefaultSecretWarningForTesting();

        assertFalse(warnIfDefaultSecret());
    }
}
