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

import java.awt.Font;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link UIHelper}.
 *
 * Assertions use {@link Font#getName()} rather than {@link Font#getFamily()}: the
 * latter resolves the logical name against the host's native font manager, which
 * throws ("Fontconfig head is null") on headless CI runners without a fontconfig
 * setup. getName() just returns the name passed to the constructor, which is all
 * these tests need to verify.
 *
 * @author Christian Pesch
 */
public class UIHelperTest {

    @Test
    public void testScaleFont100PercentReturnsSameSize() {
        Font base = new Font("SansSerif", Font.PLAIN, 12);
        Font result = UIHelper.scaleFont(base, 100);
        assertEquals(12, result.getSize());
        assertEquals("SansSerif", result.getName());
        assertEquals(Font.PLAIN, result.getStyle());
    }

    @Test
    public void testScaleFont150PercentIncreasesSize() {
        Font base = new Font("SansSerif", Font.PLAIN, 12);
        Font result = UIHelper.scaleFont(base, 150);
        assertEquals(18, result.getSize());
        assertEquals("SansSerif", result.getName());
        assertEquals(Font.PLAIN, result.getStyle());
    }

    @Test
    public void testScaleFont50PercentDecreasesSize() {
        Font base = new Font("SansSerif", Font.PLAIN, 12);
        Font result = UIHelper.scaleFont(base, 50);
        assertEquals(6, result.getSize());
        assertEquals("SansSerif", result.getName());
        assertEquals(Font.PLAIN, result.getStyle());
    }

    @Test
    public void testScaleFontRoundsToNearestInt() {
        Font base = new Font("SansSerif", Font.PLAIN, 11);
        Font result = UIHelper.scaleFont(base, 125);
        assertEquals(14, result.getSize()); // 11 * 1.25 = 13.75 -> rounds to 14
        assertEquals("SansSerif", result.getName());
        assertEquals(Font.PLAIN, result.getStyle());
    }

    @Test
    public void testScaleFontClampsToMinimumOnePoint() {
        Font base = new Font("SansSerif", Font.PLAIN, 4);
        Font result = UIHelper.scaleFont(base, 10);
        assertEquals(1, result.getSize()); // 4 * 0.10 = 0.4 -> rounds to 0 -> clamped to 1
        assertEquals("SansSerif", result.getName());
        assertEquals(Font.PLAIN, result.getStyle());
    }

    @Test
    public void testScaleFontPreservesFamilyAndStyle() {
        Font base = new Font("Serif", Font.BOLD | Font.ITALIC, 12);
        Font result = UIHelper.scaleFont(base, 150);
        assertEquals(18, result.getSize());
        assertEquals("Serif", result.getName());
        assertEquals(Font.BOLD | Font.ITALIC, result.getStyle());
    }

    @Test
    public void testDpiToPercentage96DpiIs100Percent() {
        assertEquals(100, UIHelper.dpiToPercentage(96));
    }

    @Test
    public void testDpiToPercentage120DpiIs125Percent() {
        assertEquals(125, UIHelper.dpiToPercentage(120));
    }

    @Test
    public void testDpiToPercentage144DpiIs150Percent() {
        assertEquals(150, UIHelper.dpiToPercentage(144));
    }

    @Test
    public void testDpiToPercentage192DpiIs200Percent() {
        assertEquals(200, UIHelper.dpiToPercentage(192));
    }

    @Test
    public void testDpiToPercentageClampsLowExtremeTo50() {
        assertEquals(50, UIHelper.dpiToPercentage(40));
    }

    @Test
    public void testDpiToPercentageClampsHighExtremeTo200() {
        assertEquals(200, UIHelper.dpiToPercentage(500));
    }
}
