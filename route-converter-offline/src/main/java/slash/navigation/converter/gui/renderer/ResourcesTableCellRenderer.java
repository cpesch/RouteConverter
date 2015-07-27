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

import slash.navigation.maps.RemoteResource;

import javax.swing.*;
import java.awt.*;

import static java.lang.Math.round;

/**
 * Renders the table cells of the resources table.
 *
 * @author Christian Pesch
 */

public class ResourcesTableCellRenderer extends AlternatingColorTableCellRenderer {
    private static final long ONE_KILOBYTE = 1024;
    private static final long ONE_MEGABYTE = ONE_KILOBYTE * ONE_KILOBYTE;

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
        RemoteResource resource = (RemoteResource) value;
        switch (columnIndex) {
            case 0:
                label.setText(resource.getDataSource());
                label.setToolTipText(resource.getUrl());
                break;
            case 1:
                label.setText(resource.getDownloadable().getUri());
                label.setToolTipText(resource.getUrl());
                break;
            case 2:
                label.setText(asSize(getContentLength(resource)));
                label.setToolTipText(resource.getUrl());
                break;
            default:
                throw new IllegalArgumentException("Row " + rowIndex + ", column " + columnIndex + " does not exist");
        }
        return label;
    }

    private Long getContentLength(RemoteResource resource) {
        return resource.getDownloadable().getLatestChecksum() != null ? resource.getDownloadable().getLatestChecksum().getContentLength() : null;
    }

    private static String asSize(Long size) {
        if(size == null)
            return "?";
        if(size > ONE_MEGABYTE)
            return toNextUnit(size, ONE_MEGABYTE) + " MB";
        return toNextUnit(size, ONE_KILOBYTE) + " kB";
    }

    private static long toNextUnit(Long size, long nextUnit) {
        return round(size / (double)nextUnit + 0.5);
    }
}
