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

/**
 * Acts as a {@link Document} for numbers.
 *
 * @author Christian Pesch
 */

public class NumberDocument extends PlainDocument {
    public NumberDocument(int number) {
        try {
            super.insertString(0, Integer.toString(number), null);
        } catch (BadLocationException e) {
            // intentionally left empty
        }
    }

    public int getNumber() {
        try {
            return Integer.parseInt(getText(0, getLength()));
        } catch (BadLocationException e) {
            return 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean isValidNumberString(String str) {
        if (str.length() == 0)
            return true;

        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        // create a buffer with a simulated insert
        StringBuffer buffer = new StringBuffer(getText(0, getLength()));
        buffer.insert(offs, str);

        // test if buffer would be valid
        if (isValidNumberString(buffer.toString())) {
            super.insertString(offs, str, a);
        } else
            throw new BadLocationException("Bad integer: " + buffer + " at:" + offs, offs);
    }

    public void remove(int offs, int len) throws BadLocationException {
        // create a buffer with a simulated delete
        String content = getText(0, getLength());
        StringBuffer buffer = new StringBuffer();
        buffer.append(content.substring(0, offs));
        buffer.append(content.substring(offs + len, content.length()));

        // test if buffer would be valid
        if (isValidNumberString(buffer.toString())) {
            super.remove(offs, len);
        } else
            throw new BadLocationException("Bad integer:", offs);
    }
}
