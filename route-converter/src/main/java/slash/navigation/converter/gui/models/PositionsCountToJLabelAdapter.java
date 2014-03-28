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

import javax.swing.*;
import javax.swing.event.TableModelEvent;

import static javax.swing.event.TableModelEvent.UPDATE;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;

/**
 * A bidirectional adapter that extracts the route format name from the selected route
 * of a {@link FormatAndRoutesModel} for display.
 *
 * @author Christian Pesch
 */

public class PositionsCountToJLabelAdapter extends PositionsModelToDocumentAdapter {
    private final JLabel label;

    public PositionsCountToJLabelAdapter(PositionsModel positionsModel, JLabel label) {
        super(positionsModel);
        this.label = label;
    }

    protected String getDelegateValue() {
        throw new UnsupportedOperationException();
    }

    protected void updateAdapterFromDelegate(TableModelEvent e) {
        // ignored updates on columns not relevant for row count
        if (e.getType() == UPDATE &&
                !isFirstToLastRow(e))
            return;
        if (getDelegate().isContinousRange())
            return;

        label.setText(Integer.toString(getDelegate().getRowCount()));
    }
}
