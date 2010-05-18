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

import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.converter.gui.dnd.ClipboardInteractor;
import slash.navigation.converter.gui.dnd.PositionSelection;
import slash.navigation.converter.gui.helper.JTableHelper;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.FrameAction;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

/**
 * {@link Action} that copies the selected rows of a {@link JTable}.
 *
 * @author Christian Pesch
 */

public class PasteAction extends FrameAction {
    private final JTable table;
    private final PositionsModel positionsModel;
    private final ClipboardInteractor clipboardInteractor;

    public PasteAction(JTable table, PositionsModel positionsModel, ClipboardInteractor clipboardInteractor) {
        this.table = table;
        this.positionsModel = positionsModel;
        this.clipboardInteractor = clipboardInteractor;
    }

    public void run() {
        Transferable transferable = clipboardInteractor.getFromClipboard();
        if (transferable == null)
            return;

        Object data = null;
        try {
            data = transferable.getTransferData(PositionSelection.positionFlavor);
        } catch (UnsupportedFlavorException e) {
            // intentionally left empty
        } catch (IOException e) {
            // intentionally left empty
        }

        if (data != null) {
            paste((List<BaseNavigationPosition>)data);
        }
    }

    protected void paste(List<BaseNavigationPosition> positions) {
        int[] selectedRows = table.getSelectedRows();
        final int insertRow = selectedRows.length > 0 ? selectedRows[0] + 1 : table.getRowCount();

        positionsModel.add(insertRow, positions);

        final int lastRow = insertRow - 1 + positions.size();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JTableHelper.scrollToPosition(table, lastRow);
                JTableHelper.selectPositions(table, insertRow, lastRow);
            }
        });
    }
}