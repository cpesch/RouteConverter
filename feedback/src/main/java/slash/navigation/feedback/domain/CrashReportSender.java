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

import org.apache.hc.core5.http.ContentType;
import slash.navigation.rest.Post;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static slash.navigation.datasources.DataSourceManager.V1;

/**
 * Sends an anonymous, opt-in crash telemetry report (a {@code DiagnosticReport} JSON
 * payload) to the {@code /v1/crash-report/} endpoint. The request carries no
 * credentials; authenticity is proven with an {@code X-RouteConverter-Signature}
 * header holding the lowercase hex HMAC-SHA256 over the exact raw request body bytes,
 * keyed with a shared secret.
 * <p>
 * The sender is fail-silent by contract: it never throws, never blocks on the EDT and
 * never surfaces a dialog. A caller runs it on a background thread. Oversized payloads
 * (the server rejects anything over 64 KB) are skipped client-side.
 *
 * @author Christian Pesch
 */

public class CrashReportSender {
    private static final Logger log = Logger.getLogger(CrashReportSender.class.getName());

    static final String CRASH_REPORT_URI = V1 + "crash-report/";
    static final String SIGNATURE_HEADER = "X-RouteConverter-Signature";

    static final String SECRET_PROPERTY = "routeconverter.crashReportSecret";
    static final String SECRET_RESOURCE = "crash-report.properties";
    static final String SECRET_KEY = "crashReportSecret";
    // The real HMAC key is injected at build time into crash-report.properties (Maven
    // resource filtering from -DcrashReportSecret) to match the server's
    // CRASH_REPORT_HMAC_SECRET, with the routeconverter.crashReportSecret system
    // property as a runtime override. This obvious placeholder must never be a live
    // secret and must never be committed as one.
    static final String DEFAULT_SECRET = "CHANGEME-crash-report-hmac-secret";

    static final int MAXIMUM_BYTES = 64 * 1024;

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    // warns exactly once per run: a build that forgot to inject the real key would sign
    // every report with the placeholder, so the server rejects them all silently. One
    // warning surfaces the misconfiguration without spamming the log on every crash.
    private static final AtomicBoolean warnedAboutDefaultSecret = new AtomicBoolean(false);

    /**
     * Signs and POSTs the given JSON payload to {@code apiUrl}. Returns true only on a
     * 2xx response (the report was accepted and may be removed from the spool); returns
     * false on an oversized payload, any non-2xx response or any transport error, and
     * never throws.
     */
    public boolean send(String apiUrl, String json) {
        try {
            byte[] body = json.getBytes(UTF_8);
            if (body.length > MAXIMUM_BYTES) {
                log.log(FINE, "Skipping oversized crash report of " + body.length + " bytes");
                return false;
            }
            warnIfDefaultSecret();
            String signature = sign(secret(), body);
            return post(apiUrl + CRASH_REPORT_URI, json, signature);
        } catch (Exception e) {
            log.log(FINE, "Cannot send crash report: " + e.getMessage());
            return false;
        }
    }

    /**
     * Performs the raw HTTP POST. A separate method so tests can substitute the
     * transport without a live server.
     */
    boolean post(String url, String json, String signature) throws IOException {
        Post request = new Post(url);
        request.setBody(json, ContentType.APPLICATION_JSON);
        request.setHeader(SIGNATURE_HEADER, signature);
        request.executeAsString();
        return request.isSuccessful();
    }

    static String secret() {
        String property = System.getProperty(SECRET_PROPERTY);
        if (property != null && !property.isEmpty())
            return property;
        String bundled = bundledSecret();
        if (bundled != null)
            return bundled;
        return DEFAULT_SECRET;
    }

    /**
     * Reads the build-time-injected key from the filtered {@code crash-report.properties}
     * resource, or null when it is absent or still the unresolved Maven token (a local
     * build without {@code -DcrashReportSecret}). The token-name guard mirrors
     * {@code APIKeyRegistry}.
     */
    static String bundledSecret() {
        try (InputStream inputStream = CrashReportSender.class.getResourceAsStream(SECRET_RESOURCE)) {
            if (inputStream == null)
                return null;
            Properties properties = new Properties();
            properties.load(inputStream);
            String value = properties.getProperty(SECRET_KEY);
            if (value != null && !value.isEmpty() && !value.contains(SECRET_KEY))
                return value;
        } catch (IOException e) {
            log.log(FINE, "Cannot read bundled crash-report secret: " + e.getMessage());
        }
        return null;
    }

    /**
     * Logs a single warning if the effective secret is still the committed placeholder.
     * Returns true only on the call that emitted the warning, so it can be exercised.
     */
    static boolean warnIfDefaultSecret() {
        if (DEFAULT_SECRET.equals(secret()) && warnedAboutDefaultSecret.compareAndSet(false, true)) {
            log.log(WARNING, "Crash report secret is the committed placeholder; set -D" + SECRET_PROPERTY +
                    " to the server's key or reports will fail signature verification");
            return true;
        }
        return false;
    }

    static void resetDefaultSecretWarningForTesting() {
        warnedAboutDefaultSecret.set(false);
    }

    /**
     * Computes the lowercase hex HMAC-SHA256 over {@code body} keyed with {@code secret}.
     */
    static String sign(String secret, byte[] body) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(UTF_8), HMAC_ALGORITHM));
        return toHex(mac.doFinal(body));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }
}
