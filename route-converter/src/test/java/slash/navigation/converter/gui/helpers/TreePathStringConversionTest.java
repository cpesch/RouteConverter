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
package slash.navigation.converter.gui.helpers;

import org.junit.Test;

import static org.junit.Assert.*;
import static slash.navigation.converter.gui.helpers.TreePathStringConversion.isRemote;

/**
 * Tests for the {@code isRemote} utility in {@link TreePathStringConversion}.
 *
 * @author Christian Pesch
 */
public class TreePathStringConversionTest {

    @Test
    public void testIsRemoteReturnsTrueForRemotePath() {
        assertTrue(isRemote("REMOTE:/some/path"));
    }

    @Test
    public void testIsRemoteReturnsTrueForBareRemotePrefix() {
        assertTrue(isRemote("REMOTE:"));
    }

    @Test
    public void testIsRemoteReturnsFalseForLocalPath() {
        assertFalse(isRemote("LOCAL:/some/path"));
    }

    @Test
    public void testIsRemoteReturnsFalseForEmptyString() {
        assertFalse(isRemote(""));
    }

    @Test
    public void testIsRemoteReturnsFalseForArbitraryString() {
        assertFalse(isRemote("nothing-special"));
    }

    @Test
    public void testIsRemoteIsCaseSensitive() {
        // prefix is "REMOTE:" ? lowercase should NOT match
        assertFalse(isRemote("remote:/path"));
    }
}

