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

/**
 * Tests for {@link TagStrategy}.
 *
 * @author Christian Pesch
 */

public class TagStrategyTest {

    @Test
    public void testTwoValues() {
        assertEquals(2, TagStrategy.values().length);
    }

    @Test
    public void testCreateBackupInSubdirectoryOrdinal() {
        assertEquals(0, TagStrategy.Create_Backup_In_Subdirectory.ordinal());
    }

    @Test
    public void testCreateTaggedPhotoInSubdirectoryOrdinal() {
        assertEquals(1, TagStrategy.Create_Tagged_Photo_In_Subdirectory.ordinal());
    }

    @Test
    public void testValueOfCreateBackup() {
        assertEquals(TagStrategy.Create_Backup_In_Subdirectory,
                TagStrategy.valueOf("Create_Backup_In_Subdirectory"));
    }

    @Test
    public void testValueOfCreateTaggedPhoto() {
        assertEquals(TagStrategy.Create_Tagged_Photo_In_Subdirectory,
                TagStrategy.valueOf("Create_Tagged_Photo_In_Subdirectory"));
    }

    @Test
    public void testName() {
        assertEquals("Create_Backup_In_Subdirectory",
                TagStrategy.Create_Backup_In_Subdirectory.name());
    }
}

