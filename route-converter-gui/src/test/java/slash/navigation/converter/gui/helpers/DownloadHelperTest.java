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

import org.junit.Test;
import slash.navigation.download.Checksum;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadTableModel;
import slash.navigation.download.FileAndChecksum;

import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.TableRowSorter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.DownloadTableModel.DESCRIPTION_COLUMN;
import static slash.navigation.download.State.Queued;

public class DownloadHelperTest {
    private static File tempFile() {
        try {
            File f = File.createTempFile("download-helper-test", ".tmp");
            f.deleteOnExit();
            return f;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Download> createDownloads() {
        List<Download> result = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            FileAndChecksum file = new FileAndChecksum(tempFile(), new Checksum(fromMillis(1_000_000_000_000L), 100L, "sha1"));
            result.add(new Download("desc" + i, "http://example.com/file" + i, Copy, file, null, null, Queued, tempFile()));
        }
        return result;
    }

    // view rows are sorted (descending by description) while the model keeps insertion order;
    // getSelectedDownloads() must convert the selected view rows to model rows before indexing
    @Test
    public void getSelectedDownloadsMapsViewRowsToModelRows() throws Exception {
        List<Download> downloads = createDownloads();
        DownloadTableModel model = new DownloadTableModel();
        model.setDownloads(new ArrayList<>(downloads));
        SwingUtilities.invokeAndWait(() -> { });

        JTable table = new JTable(model);
        TableRowSorter<DownloadTableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(DESCRIPTION_COLUMN,
                (Comparator<Download>) (d1, d2) -> d1.getDescription().compareTo(d2.getDescription()));
        sorter.setSortKeys(List.of(new RowSorter.SortKey(DESCRIPTION_COLUMN, SortOrder.DESCENDING)));
        table.setRowSorter(sorter);

        table.getSelectionModel().setSelectionInterval(0, 0);
        assertEquals(List.of(downloads.get(2)), DownloadHelper.getSelectedDownloads(table));

        table.getSelectionModel().setSelectionInterval(0, 0);
        table.getSelectionModel().addSelectionInterval(2, 2);
        assertEquals(List.of(downloads.get(2), downloads.get(0)), DownloadHelper.getSelectedDownloads(table));
    }
}
