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

package slash.navigation.converter.gui.renderer;

import slash.navigation.download.Download;

import javax.swing.*;
import java.awt.*;

import static slash.navigation.converter.gui.helpers.PositionHelper.formatDate;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatSize;
import static slash.navigation.download.DownloadTableModel.*;
import static slash.navigation.download.State.*;

/**
 * Renders the table cells of the downloads table.
 *
 * @author Christian Pesch
 */

public class DownloadsTableCellRenderer extends AlternatingColorTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
        Download download = (Download) value;
        switch (columnIndex) {
            case DESCRIPTION_COLUMN:
                label.setText(download.getDescription());
                label.setToolTipText(download.getUrl());
                label.setHorizontalAlignment(LEFT);
                break;
            case STATE_COLUMN:
                String text = download.getState().name();
                if (Downloading.equals(download.getState()) || Processing.equals(download.getState()) || Resuming.equals(download.getState())) {
                    Integer percentage = download.getPercentage();
                    String progress = percentage != null ? percentage + "%" : formatSize(download.getProcessedBytes());
                    text += " (" + progress + ")";
                }
                label.setText(text);
                if ((download.getState().equals(ChecksumError) || download.getState().equals(Failed)) &&
                        download.getFile().getActualChecksum() != null && download.getFile().getExpectedChecksum() != null)
                    label.setToolTipText(download.getFile().getActualChecksum().getSHA1() + " / " + download.getFile().getExpectedChecksum().getSHA1() + "<p>" +
                            download.getFile().getActualChecksum().getContentLength() + " / " + download.getFile().getExpectedChecksum().getContentLength() + "<p>" +
                            formatDate(download.getFile().getActualChecksum().getLastModified()) + " / " + formatDate(download.getFile().getExpectedChecksum().getLastModified()));
                else
                    label.setToolTipText(download.getUrl());
                label.setHorizontalAlignment(RIGHT);
                break;
            case SIZE_COLUMN:
                label.setText(formatSize(download.getSize()));
                label.setToolTipText(download.getUrl());
                label.setHorizontalAlignment(RIGHT);
                break;
            case DATE_COLUMN:
                label.setText(formatDate(download.getLastModified()));
                label.setToolTipText(download.getUrl());
                label.setHorizontalAlignment(RIGHT);
                break;
            default:
                throw new IllegalArgumentException("Row " + rowIndex + ", column " + columnIndex + " does not exist");
        }
        return label;
    }
}
