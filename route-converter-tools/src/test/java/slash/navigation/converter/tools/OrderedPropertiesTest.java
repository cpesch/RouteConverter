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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tests for {@link OrderedProperties}.
 *
 * @author Christian Pesch
 */
public class OrderedPropertiesTest {

    @Test
    public void testPutAndGet() {
        OrderedProperties props = new OrderedProperties();
        props.put("alpha", "one");
        assertEquals("one", props.get("alpha"));
    }

    @Test
    public void testGetUnknownKeyReturnsNull() {
        OrderedProperties props = new OrderedProperties();
        assertNull(props.get("missing"));
    }

    @Test
    public void testOverwriteValue() {
        OrderedProperties props = new OrderedProperties();
        props.put("key", "first");
        props.put("key", "second");
        assertEquals("second", props.get("key"));
    }

    @Test
    public void testRemove() {
        OrderedProperties props = new OrderedProperties();
        props.put("key", "value");
        props.remove("key");
        assertNull(props.get("key"));
    }

    @Test
    public void testKeysInsertionOrder() {
        OrderedProperties props = new OrderedProperties();
        props.put("charlie", "3");
        props.put("alpha", "1");
        props.put("bravo", "2");

        Enumeration<Object> keys = props.keys();
        List<String> keyList = new ArrayList<>();
        while (keys.hasMoreElements()) {
            keyList.add((String) keys.nextElement());
        }
        assertEquals(List.of("charlie", "alpha", "bravo"), keyList);
    }

    @Test
    public void testGetKeysInsertionOrder() {
        OrderedProperties props = new OrderedProperties();
        props.put("z", "last");
        props.put("a", "first");
        props.put("m", "middle");

        Set<String> keySet = props.getKeys();
        List<String> keyList = new ArrayList<>(keySet);
        assertEquals(List.of("z", "a", "m"), keyList);
    }

    @Test
    public void testGetKeysIsEmptyInitially() {
        OrderedProperties props = new OrderedProperties();
        assertTrue(props.getKeys().isEmpty());
    }
}

