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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.logging.Logger;

/**
 * Provides Catalog Client helper functions.
 *
 * @author Christian Pesch
 */

public class Helper {
    private static final Logger log = Logger.getLogger(Helper.class.getName());
    static final String UTF8_ENCODING = "UTF-8";

    public static String encodeUri(String uri) {
        try {
            String encoded = URLEncoder.encode(uri, UTF8_ENCODING);
            return encoded.replace("%2F", "/"); // better not .replace("%3A", ":");
        } catch (UnsupportedEncodingException e) {
            log.severe("Cannot encode uri " + uri + ": " + e.getMessage());
            return uri;
        }
    }

    public static String decodeUri(String uri) {
        try {
            return URLDecoder.decode(uri, UTF8_ENCODING);
        } catch (UnsupportedEncodingException e) {
            log.severe("Cannot decode uri " + uri + ": " + e.getMessage());
            return uri;
        }
    }

    public static String asUtf8(String string) {
        try {
            byte[] bytes = string.getBytes(UTF8_ENCODING);
            return new String(bytes);
        } catch (UnsupportedEncodingException e) {
            log.severe("Cannot encode " + string + " as " + UTF8_ENCODING + ": " + e.getMessage());
            return string;
        }
    }
}
