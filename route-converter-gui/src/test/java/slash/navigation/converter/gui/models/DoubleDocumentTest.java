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
package slash.navigation.converter.gui.models;

import org.junit.Test;

import javax.swing.text.BadLocationException;

import static org.junit.Assert.*;

/**
 * Tests for {@link DoubleDocument}.
 *
 * @author Christian Pesch
 */
public class DoubleDocumentTest {

    private static String text(DoubleDocument document) throws BadLocationException {
        return document.getText(0, document.getLength());
    }

    @Test
    public void constructorStripsFractionOfWholeNumbers() throws BadLocationException {
        assertEquals("3", text(new DoubleDocument(3.0)));
    }

    @Test
    public void constructorKeepsFraction() throws BadLocationException {
        assertEquals("3.5", text(new DoubleDocument(3.5)));
    }

    @Test
    public void getDoubleRoundTrips() {
        assertEquals(3.5, new DoubleDocument(3.5).getDouble(), 0.0);
    }

    @Test
    public void getDoubleOnEmptyIsZero() throws BadLocationException {
        DoubleDocument document = new DoubleDocument(7.0);
        document.remove(0, document.getLength());

        assertEquals(0.0, document.getDouble(), 0.0);
    }

    @Test
    public void insertingDigitsAppends() throws BadLocationException {
        DoubleDocument document = new DoubleDocument(1.0);
        document.insertString(document.getLength(), "2", null);

        assertEquals("12", text(document));
        assertEquals(12.0, document.getDouble(), 0.0);
    }

    @Test
    public void insertingLettersIsRejectedAndTextUnchanged() throws BadLocationException {
        DoubleDocument document = new DoubleDocument(1.0);

        assertThrows(BadLocationException.class, () -> document.insertString(document.getLength(), "x", null));
        assertEquals("1", text(document));
    }

    @Test
    public void loneMinusSignIsRejected() throws BadLocationException {
        DoubleDocument document = new DoubleDocument(0.0);
        document.remove(0, document.getLength());

        // "-" alone is not a parseable double
        assertThrows(BadLocationException.class, () -> document.insertString(0, "-", null));
    }

    @Test
    public void removeDownToEmptyIsAllowed() throws BadLocationException {
        DoubleDocument document = new DoubleDocument(42.0);

        document.remove(0, document.getLength());

        assertEquals("", text(document));
    }

    @Test
    public void removeProducingInvalidNumberIsRejected() throws BadLocationException {
        DoubleDocument document = new DoubleDocument(0.0);
        document.remove(0, document.getLength());
        document.insertString(0, "-5", null);

        // deleting the '5' leaves a lone "-" which is not a valid double
        assertThrows(BadLocationException.class, () -> document.remove(1, 1));
    }
}
