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

package slash.navigation.base;

import slash.navigation.common.NavigationPosition;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.net.URLEncoder.encode;
import static slash.common.io.Transfer.UTF8_ENCODING;
import static slash.navigation.base.RouteCharacteristics.Route;

/**
 * The base of all URL parsing navigation formats.
 *
 * @author Christian Pesch
 */

public abstract class BaseUrlParsingFormat extends BaseUrlFormat {

    @SuppressWarnings({"unchecked"})
    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected void processURL(String url, String encoding, ParserContext<Wgs84Route> context) {
        Map<String, List<String>> parameters = parseURLParameters(url, encoding);
        if (parameters == null)
            return;

        List<Wgs84Position> positions = parsePositions(parameters);
        if (positions.size() > 0)
            context.appendRoute(createRoute(Route, null, positions));
    }

    protected abstract List<Wgs84Position> parsePositions(Map<String, List<String>> parameters);

    public/*for tests*/ Map<String, List<String>> parseURLParameters(String data, String encoding) {
        if (data == null || data.length() == 0)
            return null;

        try {
            byte[] bytes = data.getBytes(encoding);
            return parseParameters(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private byte convertHexDigit(byte b) {
        if ((b >= '0') && (b <= '9')) return (byte) (b - '0');
        if ((b >= 'a') && (b <= 'f')) return (byte) (b - 'a' + 10);
        if ((b >= 'A') && (b <= 'F')) return (byte) (b - 'A' + 10);
        return 0;
    }

    private void putMapEntry(Map<String, List<String>> map, String name, String value) {
        List<String> values = map.get(name);
        if (values == null) {
            values = new ArrayList<>(1);
            map.put(name, values);
        }
        values.add(value);
    }

    private Map<String, List<String>> parseParameters(byte[] data, String encoding) throws UnsupportedEncodingException {
        if (data == null || data.length == 0)
            return null;

        Map<String, List<String>> result = new HashMap<>();
        int ix = 0;
        int ox = 0;
        String key = null;
        String value;
        while (ix < data.length) {
            byte c = data[ix++];
            switch ((char) c) {
                case '&':
                    value = new String(data, 0, ox, encoding);
                    if (key != null) {
                        putMapEntry(result, key, value);
                        key = null;
                    }
                    ox = 0;
                    break;
                case '=':
                    if (key == null) {
                        key = new String(data, 0, ox, encoding);
                        ox = 0;
                    } else {
                        data[ox++] = c;
                    }
                    break;
                case '+':
                    data[ox++] = (byte) ' ';
                    break;
                case '%':
                    int leftNibble = convertHexDigit(data[ix++]) << 4;
                    byte rightNibble = ix < data.length ? convertHexDigit(data[ix++]) : 0;
                    data[ox++] = (byte) (leftNibble + rightNibble);
                    break;
                case '?':
                    break;
                default:
                    data[ox++] = c;
            }
        }
        // The last value does not end in '&'.  So save it now.
        if (key != null) {
            value = new String(data, 0, ox, encoding);
            putMapEntry(result, key, value);
        }
        return result;
    }

    protected String decodeDescription(String description) {
        if (description == null)
            return "";
        try {
            return URLDecoder.decode(description, UTF8_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return description;
        }
    }

    protected String encodeDescription(String description) {
        if (description == null)
            return "";
        try {
            description = encode(description, UTF8_ENCODING);
            description = description.replace("%2C", ",");
            return description;
        } catch (UnsupportedEncodingException e) {
            return description;
        }
    }
}
