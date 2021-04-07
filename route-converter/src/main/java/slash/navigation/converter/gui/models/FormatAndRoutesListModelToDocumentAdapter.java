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

import slash.navigation.gui.helpers.AbstractListDataListener;

import javax.swing.event.ListDataEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.util.logging.Logger;

/**
 * An unidirectional adapter that extracts information from a {@link FormatAndRoutesModel} for display.
 *
 * @author Christian Pesch
 */

public abstract class FormatAndRoutesListModelToDocumentAdapter extends PlainDocument {
    private static final Logger log = Logger.getLogger(FormatAndRoutesListModelToDocumentAdapter.class.getName());
    private FormatAndRoutesModel delegate;

    FormatAndRoutesListModelToDocumentAdapter(FormatAndRoutesModel delegate) {
        setDelegate(delegate);
    }

    FormatAndRoutesModel getDelegate() {
        return delegate;
    }

    private void setDelegate(FormatAndRoutesModel formatAndRoutesModel) {
        this.delegate = formatAndRoutesModel;

        formatAndRoutesModel.addListDataListener(new AbstractListDataListener() {
            public void process(ListDataEvent e) {
                updateAdapterFromDelegate();
            }
        });
    }

    protected abstract String getDelegateValue();

    protected void updateAdapterFromDelegate() {
        try {
            String myContent = getText(0, getLength());
            String delegateContent = getDelegateValue();

            if (myContent.equals(delegateContent))
                return;

            remove(0, getLength());
            insertString(0, delegateContent, null);
        }
        catch (BadLocationException e) {
            log.severe("Error updating adapter: " + e);
        }
    }
}
