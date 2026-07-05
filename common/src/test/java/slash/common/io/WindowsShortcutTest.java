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

package slash.common.io;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link WindowsShortcut}. The fixture microsoft_example.lnk is
 * the sample from the [MS-SHLLINK] specification (via the LnkParse3 corpus); it
 * points to the file C:\test\a.txt.
 *
 * @author Christian Pesch
 */
public class WindowsShortcutTest {

    private File shortcut(String name) throws URISyntaxException {
        return new File(getClass().getResource(name).toURI());
    }

    @Test
    public void testDetectsValidLink() throws IOException, URISyntaxException {
        assertTrue(WindowsShortcut.isPotentialValidLink(shortcut("microsoft_example.lnk")));
    }

    @Test
    public void testRejectsNonLink() throws IOException, URISyntaxException {
        // a real file that is not a .lnk (the fixture renamed via a File without the extension check target)
        File notALink = shortcut("microsoft_example.lnk").getParentFile();
        assertFalse(WindowsShortcut.isPotentialValidLink(notALink));
    }

    @Test
    public void testResolvesTarget() throws IOException, URISyntaxException {
        WindowsShortcut shortcut = new WindowsShortcut(shortcut("microsoft_example.lnk"));
        // separator is platform dependent when joining, so normalize before comparing
        String target = shortcut.getRealFilename().replace('\\', '/');
        assertEquals("C:/test/a.txt", target);
    }

    @Test
    public void testResolvesToFile() throws IOException, URISyntaxException {
        WindowsShortcut shortcut = new WindowsShortcut(shortcut("microsoft_example.lnk"));
        assertTrue(shortcut.isFile());
        assertFalse(shortcut.isDirectory());
    }
}
