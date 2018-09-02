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
package slash.navigation.converter.tools;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * {@link Properties} but ordered for {@link OrderedResourceBundle}.
 *
 * @author Christian Pesch
 */

public class OrderedProperties extends Properties {
    private Map<String,Object> properties = new LinkedHashMap<>();

    public synchronized Object put(Object key, Object value) {
        return properties.put(key.toString(), value);
    }

    public synchronized Object get(Object key) {
        return properties.get(key);
    }

    public synchronized Object remove(Object key) {
        return properties.remove(key);
    }

    public synchronized Enumeration<Object> keys() {
        final Iterator<String> it = properties.keySet().iterator();
        return new Enumeration<Object>() {
            public boolean hasMoreElements() {
                return it.hasNext();
            }
            public Object nextElement() {
                return it.next();
            }
        };
    }

    public synchronized Set<String> getKeys() {
        return properties.keySet();
    }
}
