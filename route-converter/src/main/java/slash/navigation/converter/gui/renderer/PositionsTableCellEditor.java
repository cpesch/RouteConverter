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

import slash.navigation.common.NavigationPosition;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.EventObject;

/**
 * Renders the table cells of the positions table.
 *
 * @author Christian Pesch
 */

public abstract class PositionsTableCellEditor extends AlternatingColorTableCellRenderer implements TableCellEditor {
    private final int alignment;

    public PositionsTableCellEditor(int alignment) {
        this.alignment = alignment;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
        label.setHorizontalAlignment(alignment);
        NavigationPosition position = NavigationPosition.class.cast(value);
        formatCell(label, position);
        return label;
    }

    protected abstract void formatCell(JLabel label, NavigationPosition position);

    private DefaultCellEditor editor = new DefaultCellEditor(new JTextField());
    {
        editor.setClickCountToStart(2);
    }

    public Object getCellEditorValue() {
        return editor.getCellEditorValue();
    }

    public boolean isCellEditable(EventObject anEvent) {
        return editor.isCellEditable(anEvent);
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return editor.shouldSelectCell(anEvent);
    }

    public boolean stopCellEditing() {
        return editor.stopCellEditing();
    }

    public void cancelCellEditing() {
        editor.cancelCellEditing();
    }

    public void addCellEditorListener(CellEditorListener l) {
        editor.addCellEditorListener(l);
    }

    public void removeCellEditorListener(CellEditorListener l) {
        editor.removeCellEditorListener(l);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        NavigationPosition position = NavigationPosition.class.cast(value);
        Object editedValue = extractValue(position);
        return editor.getTableCellEditorComponent(table, editedValue, isSelected, row, column);
    }

    protected abstract String extractValue(NavigationPosition position);
}
