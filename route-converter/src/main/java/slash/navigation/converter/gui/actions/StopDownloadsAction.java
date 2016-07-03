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

import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import java.util.List;

import static javax.swing.SwingUtilities.invokeLater;
import static slash.navigation.converter.gui.helpers.DownloadHelper.getSelectedDownloads;
import static slash.navigation.gui.helpers.JTableHelper.selectAndScrollToPosition;

/**
 * {@link Action} that stops {@link Download}s from the {@link DownloadManager}.
 *
 * @author Christian Pesch
 */

public class StopDownloadsAction extends FrameAction {
    private final JTable table;
    private final DownloadManager downloadManager;

    public StopDownloadsAction(JTable table, DownloadManager downloadManager) {
        this.table = table;
        this.downloadManager = downloadManager;
    }

    public void run() {
        List<Download> downloads = getSelectedDownloads(table);
        if(downloads.size() == 0)
            return;

        int[] selectedRows = table.getSelectedRows();

        downloadManager.stopDownloads(downloads);

        final int row = selectedRows[0] < table.getRowCount() ?
                selectedRows[0] : table.getRowCount() - 1;
        if (table.getRowCount() > 0) {
            invokeLater(new Runnable() {
                public void run() {
                    selectAndScrollToPosition(table, row, row);
                }
            });
        }
    }
}