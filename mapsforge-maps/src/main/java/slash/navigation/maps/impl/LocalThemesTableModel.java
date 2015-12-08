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
package slash.navigation.maps.impl;

import slash.navigation.maps.LocalTheme;
import slash.navigation.maps.MapManager;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

import static slash.common.helpers.ThreadHelper.invokeInAwtEventQueue;

/**
 * Acts as a {@link TableModel} for the {@link LocalTheme}s of the {@link MapManager}.
 *
 * @author Christian Pesch
 */

public class LocalThemesTableModel extends AbstractTableModel {
    public static final int DESCRIPTION_COLUMN = 0;

    private List<LocalTheme> themes = new ArrayList<>();

    public List<LocalTheme> getThemes() {
        return themes;
    }

    public int getRowCount() {
        return themes.size();
    }

    public int getColumnCount() {
        return 1;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getTheme(rowIndex);
    }

    public LocalTheme getTheme(int rowIndex) {
        return themes.get(rowIndex);
    }

    public LocalTheme getThemeByUrl(String url) {
        for (LocalTheme theme : new ArrayList<>(themes)) {
            if (theme.getUrl().equals(url))
                return theme;
        }
        return null;
    }

    public LocalTheme getThemeByDescription(String description) {
        for (LocalTheme theme : new ArrayList<>(themes)) {
            if (theme.getDescription().equals(description))
                return theme;
        }
        return null;
    }

    public int getIndex(LocalTheme theme) {
        return themes.indexOf(theme);
    }

    private void addTheme(LocalTheme theme) {
        if (!themes.add(theme))
            throw new IllegalArgumentException("Theme " + theme + " not added to " + themes);

        final int index = getIndex(theme);
        if (index == -1)
            throw new IllegalArgumentException("Theme " + theme + " not found in " + themes);

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableRowsInserted(index, index);
            }
        });
    }

    void updateTheme(LocalTheme theme) {
        final int index = getIndex(theme);
        if (index == -1)
            throw new IllegalArgumentException("Theme " + theme + " not found in " + themes);

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableRowsUpdated(index, index);
            }
        });
    }

    public void addOrUpdateTheme(LocalTheme theme) {
        int index = getIndex(theme);
        if (index == -1)
            addTheme(theme);
        else
            updateTheme(theme);
    }

    public void clear() {
        this.themes = new ArrayList<>();

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableDataChanged();
            }
        });
    }
}