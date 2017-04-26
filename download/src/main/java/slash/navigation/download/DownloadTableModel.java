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

package slash.navigation.download;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

import static slash.common.helpers.ThreadHelper.invokeInAwtEventQueue;

/**
 * Acts as a {@link TableModel} for the {@link Download}s of the {@link DownloadManager}.
 *
 * @author Christian Pesch
 */

public class DownloadTableModel extends AbstractTableModel {
    public static final int DESCRIPTION_COLUMN = 0;
    public static final int STATE_COLUMN = 1;
    public static final int SIZE_COLUMN = 2;
    public static final int DATE_COLUMN = 3;

    private List<Download> downloads = new ArrayList<>();

    public List<Download> getDownloads() {
        return new ArrayList<>(downloads);
    }

    public void setDownloads(List<Download> downloads) {
        this.downloads = downloads;
        fireTableDataChanged();
    }

    public int getRowCount() {
        return downloads.size();
    }

    public int getColumnCount() {
        return DATE_COLUMN + 1;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getDownload(rowIndex);
    }

    public Download getDownload(int rowIndex) {
        return downloads.get(rowIndex);
    }

    public Download getDownload(String url) {
        for(Download download : downloads) {
            if(download.getUrl().equals(url))
                return download;
        }
        return null;
    }

    private void addDownload(Download download) {
        if (!downloads.add(download))
            throw new IllegalArgumentException("Download " + download + " not added to " + downloads);

        final int index = downloads.indexOf(download);
        if (index == -1)
            throw new IllegalArgumentException("Download " + download + " not found in " + downloads);

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableRowsInserted(index, index);
            }
        });
    }

    void updateDownload(Download download) {
        final int index = downloads.indexOf(download);
        if (index == -1)
            throw new IllegalArgumentException("Download " + download + " not found in " + downloads);

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableRowsUpdated(index, index);
            }
        });
    }

    void addOrUpdateDownload(Download download) {
        int index = downloads.indexOf(download);
        if (index == -1)
            addDownload(download);
        else
            updateDownload(download);
    }

    void removeDownload(Download download) {
        final int index = downloads.indexOf(download);
        if (index == -1)
            throw new IllegalArgumentException("Download " + download + " not found in " + downloads);

        if (!downloads.remove(download))
            throw new IllegalArgumentException("Download " + download + " not removed from " + downloads);

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableRowsDeleted(index, index);
            }
        });
    }
}
