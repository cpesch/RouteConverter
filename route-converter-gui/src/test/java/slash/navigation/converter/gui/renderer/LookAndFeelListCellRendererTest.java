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

package slash.navigation.converter.gui.renderer;

import org.junit.Test;

import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;
import static slash.navigation.converter.gui.renderer.LookAndFeelListCellRenderer.resolveText;

/**
 * Tests for {@link LookAndFeelListCellRenderer}.
 *
 * @author Christian Pesch
 */
public class LookAndFeelListCellRendererTest {

    private static final ResourceBundle BUNDLE = new ListResourceBundle() {
        protected Object[][] getContents() {
            return new Object[][]{
                    {"look-and-feel-default", "Automatic"},
                    {"look-and-feel-light", "Light"},
                    {"look-and-feel-dark", "Dark"},
                    {"look-and-feel-system", "System"},
            };
        }
    };

    @Test
    public void testResolveTextForKnownPreferenceValues() {
        assertEquals("Automatic", resolveText(BUNDLE, "default"));
        assertEquals("Light", resolveText(BUNDLE, "light"));
        assertEquals("Dark", resolveText(BUNDLE, "dark"));
        assertEquals("System", resolveText(BUNDLE, "system"));
    }

    @Test
    public void testResolveTextFallsBackToRawValueForUnknownClassName() {
        String className = "javax.swing.plaf.metal.MetalLookAndFeel";
        assertEquals(className, resolveText(BUNDLE, className));
    }
}
