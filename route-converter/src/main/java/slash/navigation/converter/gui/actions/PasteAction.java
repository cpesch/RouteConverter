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

import slash.navigation.base.*;
import slash.navigation.converter.gui.dnd.ClipboardInteractor;
import slash.navigation.converter.gui.dnd.PositionSelection;
import slash.navigation.converter.gui.helper.JTableHelper;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.FrameAction;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static slash.navigation.base.NavigationFormats.asFormat;

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

    @SuppressWarnings("unchecked")
    public void run() {
        Transferable transferable = clipboardInteractor.getFromClipboard();
        if (transferable == null)
            return;

        try {
            if (transferable.isDataFlavorSupported(PositionSelection.positionFlavor)) {
                Object selection = transferable.getTransferData(PositionSelection.positionFlavor);
                if (selection != null) {
                    PositionSelection positionsSelection = (PositionSelection) selection;
                    paste(positionsSelection.getPositions());
                }
            } else if (transferable.isDataFlavorSupported(PositionSelection.stringFlavor)) {
                Object string = transferable.getTransferData(PositionSelection.stringFlavor);
                if (string != null) {
                    paste((String) string);
                }
            }
        } catch (UnsupportedFlavorException e) {
            // intentionally left empty
        } catch (IOException e) {
            // intentionally left empty
        }
    }

    protected void paste(List<BaseNavigationPosition> sourcePositions) throws IOException {
        int[] selectedRows = table.getSelectedRows();
        final int insertRow = selectedRows.length > 0 ? selectedRows[0] + 1 : table.getRowCount();

        List<BaseNavigationPosition> targetPositions = asFormat(sourcePositions, positionsModel.getRoute().getFormat());
        positionsModel.add(insertRow, targetPositions);

        final int lastRow = insertRow - 1 + targetPositions.size();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JTableHelper.scrollToPosition(table, lastRow);
                JTableHelper.selectPositions(table, insertRow, lastRow);
            }
        });
    }

    protected void paste(String string) {
        NavigationFileParser parser = new NavigationFileParser();
        try {
            if (parser.read(new ByteArrayInputStream(string.getBytes()))) {
                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = parser.getTheRoute();
                paste(route.getPositions());
            }
        } catch (IOException e) {
            // intentionally left empty
        }
    }
}