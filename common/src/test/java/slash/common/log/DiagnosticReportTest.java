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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.log.DiagnosticReport.BUNDLED_JRE_PROPERTY;

public class DiagnosticReportTest {

    private DiagnosticReport report(Throwable throwable) {
        return new DiagnosticReport("main", throwable, "3.5", "2026-01-02 03:04:05");
    }

    @Test
    public void testJsonShapeAndOrder() {
        String json = report(new IllegalStateException("plain")).toJson();

        assertTrue(json.startsWith("{"));
        assertTrue(json.trim().endsWith("}"));
        // schema_version is the first member so the wire format can evolve
        assertTrue(json.indexOf("\"schema_version\": 1") < json.indexOf("\"root_cause_class\""));
        assertTrue(json.contains("\"root_cause_class\": \"java.lang.IllegalStateException\""));
        assertTrue(json.contains("\"cause_chain\":"));
        assertTrue(json.contains("\"stack_trace\":"));
        assertTrue(json.contains("\"thread_name\": \"main\""));
        assertTrue(json.contains("\"app_version\": \"3.5\""));
        assertTrue(json.contains("\"build\": \"2026-01-02 03:04:05\""));
        assertTrue(json.contains("\"os\":"));
        assertTrue(json.contains("\"java\":"));
        // no allowlisted numeric/boolean is emitted as a quoted string
        assertTrue(json.contains("\"priority_signature\": false"));
    }

    @Test
    public void testJsonEscapesSpecialCharacters() {
        String json = report(new IllegalStateException("he said \"hi\"\tand\nbye")).toJson();
        assertTrue(json.contains("\\\"hi\\\""));
        assertTrue(json.contains("\\t"));
        assertTrue(json.contains("\\n"));
    }

    @Test
    public void testNoPrioritySignature() {
        DiagnosticReport report = report(new RuntimeException("just a bug"));
        assertFalse(report.isPrioritySignature());
        assertNull(report.getMissingClass());
        assertTrue(report.toJson().contains("\"missing_class\": null"));
    }

    @Test
    public void testPrioritySignatureNoClassDefFound() {
        DiagnosticReport report = report(new NoClassDefFoundError("jdk/net/Sockets"));
        assertTrue(report.isPrioritySignature());
        assertEquals("jdk/net/Sockets", report.getMissingClass());
        assertEquals("java.lang.NoClassDefFoundError", report.getRootCauseClass());
        assertTrue(report.toJson().contains("\"priority_signature\": true"));
        assertTrue(report.toJson().contains("\"missing_class\": \"jdk/net/Sockets\""));
    }

    @Test
    public void testPrioritySignatureThroughInitializerWrapper() {
        // ExceptionInInitializerError has no message of its own; the missing class
        // lives in its NoClassDefFoundError cause
        ExceptionInInitializerError error = new ExceptionInInitializerError(new NoClassDefFoundError("slash/Foo"));
        DiagnosticReport report = report(error);
        assertTrue(report.isPrioritySignature());
        assertEquals("slash/Foo", report.getMissingClass());
    }

    @Test
    public void testPrioritySignatureFromNestedCause() {
        // a priority signature anywhere in the cause chain counts
        RuntimeException wrapper = new RuntimeException("wrapper", new ClassNotFoundException("com.example.Missing"));
        DiagnosticReport report = report(wrapper);
        assertTrue(report.isPrioritySignature());
        assertEquals("com.example.Missing", report.getMissingClass());
    }

    @Test
    public void testUnsatisfiedLinkErrorIsPriority() {
        DiagnosticReport report = report(new UnsatisfiedLinkError("no gluegen in java.library.path"));
        assertTrue(report.isPrioritySignature());
        assertEquals("no gluegen in java.library.path", report.getMissingClass());
    }

    @Test
    public void testScrubsUserHomeAndNameFromMessages() {
        String home = System.getProperty("user.home");
        // an exception carrying an absolute path (e.g. a FileNotFoundException) must not
        // leak the home directory (and hence the OS user name) into the payload
        String json = report(new RuntimeException("Cannot read " + home + "/routes/secret.gpx")).toJson();
        assertTrue(json.contains("<USER_HOME>"));
        assertFalse(json.contains(home));

        String user = System.getProperty("user.name");
        if (user != null && user.length() >= 3) {
            String userJson = report(new RuntimeException("access for " + user + " denied")).toJson();
            assertTrue(userJson.contains("<USER>"));
        }
    }

    @Test
    public void testScrubIsNoOpWithoutKnownValues() {
        assertNull(DiagnosticReport.scrub(null));
        assertEquals("nothing sensitive here", DiagnosticReport.scrub("nothing sensitive here"));
    }

    @Test
    public void testScrubsWindowsPathOutsideHome() {
        // a document opened from a different drive than user.home still leaks its name
        // through a free-form message; the whole path token is redacted
        String scrubbed = DiagnosticReport.scrub("Cannot read D:\\Trips\\vacation2024.gpx", "C:\\Users\\me", "me");
        assertTrue(scrubbed.contains("<PATH>"));
        assertFalse(scrubbed.contains("vacation2024"));
        assertFalse(scrubbed.contains("Trips"));
    }

    @Test
    public void testScrubsUnixExternalPath() {
        String scrubbed = DiagnosticReport.scrub("Cannot read /Volumes/Backup/trips/secret.gpx", "/home/me", "me");
        assertTrue(scrubbed.contains("<PATH>"));
        assertFalse(scrubbed.contains("secret.gpx"));
    }

    @Test
    public void testScrubsShortUserNameOnWordBoundary() {
        // a two-character user name must still be scrubbed (the old length>=3 guard let
        // it leak) but only as a whole word, never inside an unrelated token like "json"
        String scrubbed = DiagnosticReport.scrub("parsing json failed for jo", null, "jo");
        assertTrue("standalone user name is scrubbed", scrubbed.contains("<USER>"));
        assertTrue("substring inside another word is left intact", scrubbed.contains("json"));
        assertFalse(scrubbed.contains("for jo"));
    }

    @Test
    public void testTruncateCapsLongValueWithMarker() {
        assertNull(DiagnosticReport.truncate(null));
        assertEquals("short", DiagnosticReport.truncate("short"));

        StringBuilder builder = new StringBuilder();
        while (builder.length() < DiagnosticReport.MAXIMUM_FIELD_LENGTH + 100)
            builder.append("x");
        String truncated = DiagnosticReport.truncate(builder.toString());
        assertTrue(truncated.endsWith(DiagnosticReport.TRUNCATION_MARKER));
        assertEquals(DiagnosticReport.MAXIMUM_FIELD_LENGTH + DiagnosticReport.TRUNCATION_MARKER.length(),
                truncated.length());
    }

    @Test
    public void testHugeMessageProducesBoundedTruncatedJson() {
        StringBuilder builder = new StringBuilder();
        while (builder.length() < 200 * 1024)
            builder.append("boom ");
        String json = report(new RuntimeException(builder.toString())).toJson();
        // the cause chain carrying the huge message is truncated, so the payload stays
        // well under the 64 KB the crash-report endpoint accepts (no forever-retry loop)
        assertTrue(json.contains("[truncated]"));
        assertTrue("payload must stay under the sender limit, was " + json.length(), json.length() < 64 * 1024);
    }

    @Test
    public void testBundledJreFlag() {
        String previous = System.getProperty(BUNDLED_JRE_PROPERTY);
        try {
            System.setProperty(BUNDLED_JRE_PROPERTY, "true");
            assertTrue(report(new RuntimeException("x")).isBundledJre());
            assertTrue(report(new RuntimeException("x")).toJson().contains("\"bundled_jre\": true"));

            System.clearProperty(BUNDLED_JRE_PROPERTY);
            assertFalse(report(new RuntimeException("x")).isBundledJre());
            assertTrue(report(new RuntimeException("x")).toJson().contains("\"bundled_jre\": false"));
        } finally {
            if (previous != null)
                System.setProperty(BUNDLED_JRE_PROPERTY, previous);
            else
                System.clearProperty(BUNDLED_JRE_PROPERTY);
        }
    }
}
