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

import slash.navigation.download.Download;
import slash.navigation.download.DownloadTableModel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper for simplified {@link Download} operations.
 *
 * @author Christian Pesch
 */

public class DownloadHelper {
    public static List<Download> getSelectedDownloads(JTable table) {
        int[] selectedRows = table.getSelectedRows();
        List<Download> downloads = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            int row = table.convertRowIndexToView(selectedRow);
            downloads.add(((DownloadTableModel) table.getModel()).getDownload(row));
        }
        return downloads;
    }
}
