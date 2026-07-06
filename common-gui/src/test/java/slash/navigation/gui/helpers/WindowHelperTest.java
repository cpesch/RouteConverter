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

import javax.swing.*;

import static org.junit.Assert.*;
import static slash.navigation.gui.helpers.WindowHelper.boundMessage;
import static slash.navigation.gui.helpers.WindowHelper.needsScrolling;

/**
 * Tests for the message-bounding decision in {@link WindowHelper}.
 *
 * @author Christian Pesch
 */
public class WindowHelperTest {

    @Test
    public void shortTextDoesNotScroll() {
        assertFalse(needsScrolling("something went wrong"));
    }

    @Test
    public void manyLinesScroll() {
        assertTrue(needsScrolling("a\n".repeat(20)));
    }

    @Test
    public void veryLongTextScrolls() {
        assertTrue(needsScrolling("x".repeat(1000)));
    }

    @Test
    public void htmlIsNeverScrolled() {
        // JOptionPane renders HTML itself; wrapping it in a JTextArea would show raw markup
        assertFalse(needsScrolling("<html>a very " + "long ".repeat(500) + "message</html>"));
    }

    @Test
    public void boundMessageWrapsLongTextInScrollPane() {
        Object bound = boundMessage("line\n".repeat(20));
        assertTrue(bound instanceof JScrollPane);
    }

    @Test
    public void boundMessageLeavesShortTextAsString() {
        Object bound = boundMessage("short");
        assertEquals("short", bound);
    }

    @Test
    public void boundMessagePassesComponentsThrough() {
        JLabel label = new JLabel("component message");
        assertSame(label, boundMessage(label));
    }
}
