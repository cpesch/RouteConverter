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
 * Tests for {@link IntegerDocument}.
 *
 * @author Christian Pesch
 */
public class IntegerDocumentTest {

    private static String text(IntegerDocument document) throws BadLocationException {
        return document.getText(0, document.getLength());
    }

    @Test
    public void constructorRendersTheInteger() throws BadLocationException {
        assertEquals("42", text(new IntegerDocument(42)));
        assertEquals("-7", text(new IntegerDocument(-7)));
    }

    @Test
    public void getIntRoundTrips() {
        assertEquals(42, new IntegerDocument(42).getInt());
    }

    @Test
    public void getIntOnEmptyIsZero() throws BadLocationException {
        IntegerDocument document = new IntegerDocument(5);
        document.remove(0, document.getLength());

        assertEquals(0, document.getInt());
    }

    @Test
    public void insertingDigitsAppends() throws BadLocationException {
        IntegerDocument document = new IntegerDocument(1);
        document.insertString(document.getLength(), "0", null);

        assertEquals("10", text(document));
        assertEquals(10, document.getInt());
    }

    @Test
    public void insertingADecimalPointIsRejected() throws BadLocationException {
        IntegerDocument document = new IntegerDocument(1);

        assertThrows(BadLocationException.class, () -> document.insertString(document.getLength(), ".5", null));
        assertEquals("1", text(document));
    }

    @Test
    public void insertingLettersIsRejected() {
        IntegerDocument document = new IntegerDocument(1);

        assertThrows(BadLocationException.class, () -> document.insertString(document.getLength(), "x", null));
    }

    @Test
    public void loneMinusSignIsRejected() throws BadLocationException {
        IntegerDocument document = new IntegerDocument(0);
        document.remove(0, document.getLength());

        assertThrows(BadLocationException.class, () -> document.insertString(0, "-", null));
    }

    @Test
    public void removeProducingInvalidNumberIsRejected() throws BadLocationException {
        IntegerDocument document = new IntegerDocument(-5);

        // deleting the '5' leaves a lone "-" which is not a valid integer
        assertThrows(BadLocationException.class, () -> document.remove(1, 1));
    }
}
