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

package slash.navigation.converter.gui.helper;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.awt.*;

/**
 * A helper for simplified {@link JTable} operations.
 *
 * @author Christian Pesch
 */

public class JTableHelper {
    public static void scrollToPosition(JTable table, int insertRow) {
        Rectangle rectangle = table.getCellRect(insertRow, 1, true);
        table.scrollRectToVisible(rectangle);
    }

    public static void selectPositions(final JTable table, final int index0, final int index1) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                table.getSelectionModel().setSelectionInterval(index0, index1);
            }
        });
    }

    public static boolean isFirstToLastRow(TableModelEvent e) {
        return e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE;
    }
}