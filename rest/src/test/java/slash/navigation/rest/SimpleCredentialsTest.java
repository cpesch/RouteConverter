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

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SimpleCredentialsTest {

    @Test
    public void testUserName() {
        SimpleCredentials creds = new SimpleCredentials("alice", new char[]{'s', 'e', 'c'});
        assertEquals("alice", creds.userName());
    }

    @Test
    public void testPassword() {
        char[] pw = {'p', 'a', 's', 's'};
        SimpleCredentials creds = new SimpleCredentials("bob", pw);
        assertArrayEquals(pw, creds.password());
    }

    @Test
    public void testEqualsAndHashCodeSameValues() {
        char[] pw = {'x'};
        SimpleCredentials a = new SimpleCredentials("user", pw);
        SimpleCredentials b = new SimpleCredentials("user", pw);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualDifferentUserName() {
        char[] pw = {'x'};
        SimpleCredentials a = new SimpleCredentials("alice", pw);
        SimpleCredentials b = new SimpleCredentials("bob", pw);
        assertNotEquals(a, b);
    }
}

