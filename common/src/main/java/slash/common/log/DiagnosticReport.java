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

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static slash.common.helpers.ExceptionHelper.getMessageWithCauses;
import static slash.common.helpers.ExceptionHelper.getRootCause;
import static slash.common.helpers.ExceptionHelper.printStackTrace;
import static slash.common.system.Platform.getJava;
import static slash.common.system.Platform.getPlatform;

/**
 * An allowlist-only diagnostic payload assembled from an uncaught {@link Throwable}
 * plus the runtime environment. It is rendered as a stable JSON object so a crash
 * arrives as a full root-cause chain and environment instead of a screenshot of a
 * truncated dialog.
 * <p>
 * Only known fields are extracted; free text is never scraped. The one unavoidable
 * exception is the cause chain and stack trace, which are needed for root-cause
 * diagnosis yet can embed absolute file paths (a {@code FileNotFoundException} on an
 * opened document, say). Those paths would reveal the OS user name and document
 * names, so {@link #scrub} replaces the known {@code user.home} and {@code user.name}
 * values with placeholders before the payload is spooled or sent. Route/track
 * contents, file names beyond what a path segment carries, and any stable identifier
 * are still never captured.
 *
 * @author Christian Pesch
 */

public class DiagnosticReport {
    static final int SCHEMA_VERSION = 1;
    static final String BUNDLED_JRE_PROPERTY = "routeconverter.bundledJre";
    static final String HOME_PLACEHOLDER = "<USER_HOME>";
    static final String USER_PLACEHOLDER = "<USER>";

    // the OS user name and home directory leak into exception messages / stack traces
    // via absolute paths; both are known values, so they are scrubbed by literal
    // replacement (deterministic, not a heuristic mask) before anything leaves the app
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String USER_NAME = System.getProperty("user.name");

    /**
     * The bundle-integrity signatures: a stripped JRE missing a JDK module a
     * dependency touches surfaces as one of these.
     */
    private static final List<Class<? extends Throwable>> PRIORITY_SIGNATURES = asList(
            NoClassDefFoundError.class, ExceptionInInitializerError.class,
            ClassNotFoundException.class, UnsatisfiedLinkError.class);

    private final String rootCauseClass;
    private final String causeChain;
    private final String stackTrace;
    private final String threadName;
    private final boolean prioritySignature;
    private final String missingClass;
    private final String appVersion;
    private final String build;
    private final String os;
    private final String java;
    private final boolean bundledJre;

    public DiagnosticReport(String threadName, Throwable throwable, String appVersion, String build) {
        List<Throwable> chain = toChain(throwable);
        this.threadName = threadName;
        this.rootCauseClass = getRootCause(throwable).getClass().getName();
        this.causeChain = scrub(getMessageWithCauses(throwable));
        this.stackTrace = scrub(printStackTrace(throwable));
        this.prioritySignature = computePrioritySignature(chain);
        this.missingClass = scrub(extractMissingClass(chain));
        this.appVersion = appVersion;
        this.build = build;
        this.os = getPlatform();
        this.java = getJava();
        this.bundledJre = Boolean.getBoolean(BUNDLED_JRE_PROPERTY);
    }

    String getRootCauseClass() {
        return rootCauseClass;
    }

    boolean isPrioritySignature() {
        return prioritySignature;
    }

    String getMissingClass() {
        return missingClass;
    }

    boolean isBundledJre() {
        return bundledJre;
    }

    /**
     * Replaces the known {@code user.home} and {@code user.name} values with
     * placeholders so an absolute path in a message or stack trace cannot reveal
     * them. This is a literal replacement of two known strings, not a heuristic path
     * mask; the short-value guard avoids scrubbing an accidental common substring.
     */
    static String scrub(String value) {
        if (value == null)
            return null;
        String result = value;
        if (USER_HOME != null && USER_HOME.length() >= 3)
            result = result.replace(USER_HOME, HOME_PLACEHOLDER);
        if (USER_NAME != null && USER_NAME.length() >= 3)
            result = result.replace(USER_NAME, USER_PLACEHOLDER);
        return result;
    }

    private static List<Throwable> toChain(Throwable throwable) {
        List<Throwable> chain = new ArrayList<>();
        Throwable cause = throwable;
        while (cause != null && !chain.contains(cause) && chain.size() < 100) {
            chain.add(cause);
            if (cause.getCause() == cause)
                break;
            cause = cause.getCause();
        }
        return chain;
    }

    private static boolean isPrioritySignature(Throwable throwable) {
        for (Class<? extends Throwable> type : PRIORITY_SIGNATURES) {
            if (type.isInstance(throwable))
                return true;
        }
        return false;
    }

    private static boolean computePrioritySignature(List<Throwable> chain) {
        for (Throwable throwable : chain) {
            if (isPrioritySignature(throwable))
                return true;
        }
        return false;
    }

    /**
     * Extracts the missing class/library name a priority signature carries in its
     * message (e.g. a NoClassDefFoundError's "jdk/net/Sockets"). A wrapper without a
     * message of its own (e.g. ExceptionInInitializerError) is skipped in favour of
     * the next priority cause that has one.
     */
    private static String extractMissingClass(List<Throwable> chain) {
        for (Throwable throwable : chain) {
            if (isPrioritySignature(throwable) && throwable.getMessage() != null)
                return throwable.getMessage();
        }
        return null;
    }

    /**
     * Renders the report as a stable JSON object with {@code schema_version} first,
     * so the wire format can evolve. Emission is dependency-free.
     */
    public String toJson() {
        List<String> members = new ArrayList<>();
        members.add(member("schema_version", Integer.toString(SCHEMA_VERSION)));
        members.add(member("root_cause_class", string(rootCauseClass)));
        members.add(member("cause_chain", string(causeChain)));
        members.add(member("stack_trace", string(stackTrace)));
        members.add(member("thread_name", string(threadName)));
        members.add(member("priority_signature", Boolean.toString(prioritySignature)));
        members.add(member("missing_class", missingClass != null ? string(missingClass) : "null"));
        members.add(member("app_version", string(appVersion)));
        members.add(member("build", string(build)));
        members.add(member("os", string(os)));
        members.add(member("java", string(java)));
        members.add(member("bundled_jre", Boolean.toString(bundledJre)));
        return "{\n  " + String.join(",\n  ", members) + "\n}";
    }

    private static String member(String key, String value) {
        return string(key) + ": " + value;
    }

    private static String string(String value) {
        if (value == null)
            return "null";
        StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (c < 0x20)
                        builder.append(String.format("\\u%04x", (int) c));
                    else
                        builder.append(c);
                    break;
            }
        }
        return builder.append("\"").toString();
    }
}
