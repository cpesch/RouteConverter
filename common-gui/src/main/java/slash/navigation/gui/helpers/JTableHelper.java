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

package slash.navigation.gui.helpers;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.logging.Logger;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.max;
import static java.lang.String.format;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * A helper for simplified {@link JTable} operations.
 *
 * @author Christian Pesch
 */

public class JTableHelper {
    private static final Logger log = Logger.getLogger(JTableHelper.class.getName());

    private static final int MINIMUM_ROW_HEIGHT = 16;
    private static final int ROW_HEIGHT_MAGIC_CONSTANT = 4;

    public static int calculateRowHeight(Object objectWithTable, TableCellEditor cellEditor, Object cellValue) {
        Component component = cellEditor.getTableCellEditorComponent(null, cellValue, true, 0, 0);
        int rowHeight = max(component.getPreferredSize().height - ROW_HEIGHT_MAGIC_CONSTANT, MINIMUM_ROW_HEIGHT);
        log.info(format("Using row height %d for table %s", rowHeight, objectWithTable));
        return rowHeight;
    }

    public static void scrollToPosition(JTable table, int insertRow) {
        Rectangle rectangle = table.getCellRect(insertRow, 1, true);
        table.scrollRectToVisible(rectangle);
    }

    public static void selectPositions(final JTable table, final int index0, final int index1) {
        invokeLater(new Runnable() {
            public void run() {
                table.getSelectionModel().setSelectionInterval(index0, index1);
            }
        });
    }

    public static void selectAndScrollToPosition(JTable table, int index0, int index1) {
        selectPositions(table, index0, index1);
        scrollToPosition(table, index0);
    }

    public static boolean isFirstToLastRow(TableModelEvent e) {
        return e.getFirstRow() == 0 && e.getLastRow() == MAX_VALUE;
    }
}