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
package slash.navigation.download.tools.gui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model for mirror jobs enriched with snapshot metadata.
 *
 * @author Christian Pesch
 */
public class MirrorJobsTableModel extends AbstractTableModel {
    private static final String[] COLUMN_NAMES = {
            "ID", "Description", "Files", "Maps", "Themes", "Items", "Size", "Mirror URL", "Status"
    };
    private final List<MirrorJobRow> rows = new ArrayList<>();

    public void setRows(List<MirrorJobRow> newRows) {
        rows.clear();
        rows.addAll(newRows);
        fireTableDataChanged();
    }

    public MirrorJobRow getRow(int modelRow) {
        return rows.get(modelRow);
    }

    public List<MirrorJobRow> getRows() {
        return List.copyOf(rows);
    }

    public int indexOf(MirrorJobRow row) {
        return rows.indexOf(row);
    }

    public void updateRow(MirrorJobRow row) {
        int index = indexOf(row);
        if (index >= 0)
            fireTableRowsUpdated(index, index);
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 2, 3, 4, 5 -> Integer.class;
            case 6 -> Long.class;
            default -> String.class;
        };
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        MirrorJobRow row = rows.get(rowIndex);
        SnapshotJobInfo info = row.getSnapshotJobInfo();
        return switch (columnIndex) {
            case 0 -> row.getId();
            case 1 -> info.name();
            case 2 -> info.fileCount();
            case 3 -> info.mapCount();
            case 4 -> info.themeCount();
            case 5 -> info.downloadableCount();
            case 6 -> info.totalSize();
            case 7 -> info.mirrorUrl();
            case 8 -> row.getStatus();
            default -> "";
        };
    }
}

