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
package slash.navigation.gui.helpers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link UIHelper}.
 *
 * @author Christian Pesch
 */
public class UIHelperTest {

    private static final String SYSTEM = "system.LookAndFeel";

    @Test
    public void testAutomaticLookAndFeelFollowsOperatingSystemOnWindowsAndLinux() {
        assertEquals("com.formdev.flatlaf.FlatLightLaf",
                UIHelper.getLookAndFeelClassName(UIHelper.AUTOMATIC_LOOK_AND_FEEL, false, () -> false, SYSTEM));
        assertEquals("com.formdev.flatlaf.FlatDarkLaf",
                UIHelper.getLookAndFeelClassName(UIHelper.AUTOMATIC_LOOK_AND_FEEL, false, () -> true, SYSTEM));
    }

    @Test
    public void testAutomaticLookAndFeelKeepsSystemLookAndFeelOnMac() {
        assertEquals(SYSTEM, UIHelper.getLookAndFeelClassName(UIHelper.AUTOMATIC_LOOK_AND_FEEL, true, () -> {
            throw new AssertionError("must not query the operating system on macOS");
        }, SYSTEM));
    }

    @Test
    public void testLightAndDarkLookAndFeel() {
        assertEquals("com.formdev.flatlaf.FlatLightLaf",
                UIHelper.getLookAndFeelClassName(UIHelper.LIGHT_LOOK_AND_FEEL, false, () -> true, SYSTEM));
        assertEquals("com.formdev.flatlaf.FlatDarkLaf",
                UIHelper.getLookAndFeelClassName(UIHelper.DARK_LOOK_AND_FEEL, false, () -> false, SYSTEM));
        assertEquals("com.formdev.flatlaf.themes.FlatMacLightLaf",
                UIHelper.getLookAndFeelClassName(UIHelper.LIGHT_LOOK_AND_FEEL, true, () -> true, SYSTEM));
        assertEquals("com.formdev.flatlaf.themes.FlatMacDarkLaf",
                UIHelper.getLookAndFeelClassName(UIHelper.DARK_LOOK_AND_FEEL, true, () -> false, SYSTEM));
    }

    @Test
    public void testSystemAndCustomLookAndFeel() {
        assertEquals(SYSTEM, UIHelper.getLookAndFeelClassName(UIHelper.SYSTEM_LOOK_AND_FEEL, false, () -> true, SYSTEM));
        assertEquals("javax.swing.plaf.metal.MetalLookAndFeel",
                UIHelper.getLookAndFeelClassName("javax.swing.plaf.metal.MetalLookAndFeel", false, () -> true, SYSTEM));
    }

    @Test
    public void testIsWindowsAppsDark() {
        String header = "\r\nHKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\r\n";
        assertTrue(UIHelper.isWindowsAppsDark(header + "    AppsUseLightTheme    REG_DWORD    0x0\r\n\r\n"));
        assertFalse(UIHelper.isWindowsAppsDark(header + "    AppsUseLightTheme    REG_DWORD    0x1\r\n\r\n"));
        assertFalse(UIHelper.isWindowsAppsDark(header + "    AppsUseLightTheme    REG_DWORD    0x10\r\n\r\n"));
        assertFalse(UIHelper.isWindowsAppsDark(""));
    }

    @Test
    public void testIsGnomeDark() {
        assertTrue(UIHelper.isGnomeDark("'prefer-dark'\n", "'Adwaita'\n"));
        assertTrue(UIHelper.isGnomeDark("'default'\n", "'Adwaita-dark'\n"));
        assertFalse(UIHelper.isGnomeDark("'default'\n", "'Adwaita'\n"));
        assertFalse(UIHelper.isGnomeDark("", ""));
    }
}
