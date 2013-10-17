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
package slash.navigation.converter.gui.mapview;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Renders the {@link File} names of the map and theme combo box relatively to a root.
 *
 * @author Christian Pesch
 */

public class FileListCellRenderer extends DefaultListCellRenderer {
    private File root;

    public FileListCellRenderer(File root) {
        this.root = root;
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        File file = (File) value;
        if (file != null) {
            String filePath = removePrefix(root, file);
            label.setText(filePath);
        }
        return label;
    }

    static String removePrefix(File root, File file) {
        String rootPath = root.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (filePath.startsWith(rootPath))
            filePath = filePath.substring(rootPath.length());
        else
            filePath = file.getName();
        return filePath;
    }
}
