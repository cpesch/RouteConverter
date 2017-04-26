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

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import static java.lang.Double.parseDouble;

/**
 * Acts as a {@link Document} for {@link double}s.
 *
 * @author Christian Pesch
 */

public class DoubleDocument extends PlainDocument {
    public DoubleDocument(double aDouble) {
        try {
            // eliminate fraction if possible
            String string = Math.round(aDouble) == (int) aDouble ? Integer.toString((int) aDouble) : Double.toString(aDouble);
            super.insertString(0, string, null);
        } catch (BadLocationException e) {
            // intentionally left empty
        }
    }

    public double getDouble() {
        try {
            return parseDouble(getText(0, getLength()));
        } catch (BadLocationException | NumberFormatException e) {
            return 0;
        }
    }

    private boolean isValidNumberString(String str) {
        if (str.length() == 0)
            return true;

        try {
            parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        // create a buffer with a simulated insert
        StringBuilder buffer = new StringBuilder(getText(0, getLength()));
        buffer.insert(offs, str);

        // test if buffer would be valid
        if (isValidNumberString(buffer.toString())) {
            super.insertString(offs, str, a);
        } else
            throw new BadLocationException("Bad double: " + buffer + " at:" + offs, offs);
    }

    public void remove(int offs, int len) throws BadLocationException {
        // create a buffer with a simulated delete
        String content = getText(0, getLength());
        StringBuilder buffer = new StringBuilder();
        buffer.append(content.substring(0, offs));
        buffer.append(content.substring(offs + len, content.length()));

        // test if buffer would be valid
        if (isValidNumberString(buffer.toString())) {
            super.remove(offs, len);
        } else
            throw new BadLocationException("Bad double:", offs);
    }
}
