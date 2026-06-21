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

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A {@link DocumentListener} that treats all events the same.
 *
 * @author Christian Pesch
 */

public abstract class AbstractDocumentListener implements DocumentListener {

    public abstract void process(DocumentEvent e);

    public void insertUpdate(DocumentEvent e) {
        process(e);
    }

    public void removeUpdate(DocumentEvent e) {
        process(e);
    }

    public void changedUpdate(DocumentEvent e) {
        process(e);
    }
}