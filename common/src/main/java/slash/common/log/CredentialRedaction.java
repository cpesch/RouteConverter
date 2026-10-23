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

import java.util.regex.Pattern;

/**
 * Removes credential-shaped tokens a URL or header embedded in free text can carry --
 * a query/matrix parameter such as {@code apikey} or {@code token}, a {@code Bearer}/
 * {@code Basic} auth header value, or a {@code user:password@} URL authority -- so a
 * tile-service API key or session token never leaves the device. Used by
 * {@link DiagnosticReport#scrub} for crash reports and by any other code path that
 * forwards a raw log or exception text, e.g. the error-report dialog and
 * {@code RouteFeedback.sendErrorReport}.
 *
 * @author Christian Pesch
 */

public class CredentialRedaction {
    // a credential-bearing query/matrix parameter value, e.g. ?apikey=... or ;jsessionid=...;
    // the name is matched case-insensitively but only right after a ?, & or ; separator, so
    // an unrelated identifier that merely ends or starts with the same letters (monkey=banana,
    // ?keyboard=1) is left alone; the value runs up to the next separator, whitespace, quote
    // or markup character. Names and spellings (api-key, access-token, client-secret) follow
    // rc-site's feedback/redaction.py, so client and server redact the same parameters
    private static final Pattern CREDENTIAL_PARAMETER = Pattern.compile(
            "([?&;](?:api[_-]?key|access[_-]?token|client[_-]?secret|signature|jsessionid|session"
                    + "|password|passwd|token|secret|pwd|sig|auth|key)=)[^&;\\s\"'<>|#]+",
            Pattern.CASE_INSENSITIVE);
    // Bearer/Basic auth material in an Authorization header value embedded in a message: a
    // token of at least 8 base64/JWT characters, as on rc-site, so the next word of a plain
    // sentence ("a basic error") stays readable
    private static final Pattern BEARER_OR_BASIC_AUTH = Pattern.compile(
            "\\b(Bearer|Basic)\\s+[A-Za-z0-9._~+/=-]{8,}", Pattern.CASE_INSENSITIVE);
    // a user:password@ credential embedded in a URL's authority component
    private static final Pattern URL_USERINFO = Pattern.compile("([A-Za-z][A-Za-z0-9+.-]*://)[^/\\s@]+:[^/\\s@]+@");
    private static final String REDACTED = "<redacted>";

    private CredentialRedaction() {
    }

    /**
     * Replaces credential-shaped tokens in {@code value} with {@code <redacted>}; a
     * {@code null} value is returned unchanged.
     */
    public static String redact(String value) {
        if (value == null)
            return null;
        String result = value;
        result = CREDENTIAL_PARAMETER.matcher(result).replaceAll("$1" + REDACTED);
        result = BEARER_OR_BASIC_AUTH.matcher(result).replaceAll("$1 " + REDACTED);
        result = URL_USERINFO.matcher(result).replaceAll("$1" + REDACTED + "@");
        return result;
    }
}
