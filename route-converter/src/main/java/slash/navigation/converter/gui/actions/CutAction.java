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

package slash.navigation.converter.gui.actions;

import slash.navigation.converter.gui.dnd.ClipboardInteractor;
import slash.navigation.converter.gui.dnd.PositionSelection;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;

import static javax.swing.SwingUtilities.invokeLater;
import static slash.navigation.gui.helpers.JTableHelper.selectAndScrollToPosition;

/**
 * {@link Action} that cuts the selected rows of a {@link JTable}.
 *
 * @author Christian Pesch
 */

public class CutAction extends FrameAction {
    private final JTable table;
    private final PositionsModel positionsModel;
    private final ClipboardInteractor clipboardInteractor;

    public CutAction(JTable table, PositionsModel positionsModel, ClipboardInteractor clipboardInteractor) {
        this.table = table;
        this.positionsModel = positionsModel;
        this.clipboardInteractor = clipboardInteractor;
    }

    public void run() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length > 0) {
            clipboardInteractor.putIntoClipboard(new PositionSelection(positionsModel.getPositions(selectedRows),
                    positionsModel.getRoute().getFormat()));

            positionsModel.remove(selectedRows);
            final int cutRow = selectedRows[0] > 0 ? selectedRows[0] - 1 : 0;
            if (table.getRowCount() > 0) {
                invokeLater(new Runnable() {
                    public void run() {
                        selectAndScrollToPosition(table, cutRow, cutRow);
                    }
                });
            }
        }
    }
}