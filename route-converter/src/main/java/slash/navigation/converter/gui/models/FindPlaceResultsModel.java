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
package slash.navigation.converter.gui.models;

import slash.navigation.common.NavigationPosition;
import slash.navigation.geocoding.GeocodingResult;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Table model for geocoding results shown in the Find Place dialog.
 *
 * @author Christian Pesch
 */
public class FindPlaceResultsModel extends AbstractTableModel {
    public static final int NAME_COLUMN = 0;
    public static final int LONGITUDE_COLUMN = 1;
    public static final int LATITUDE_COLUMN = 2;
    public static final int GEOCODING_SERVICE_COLUMN = 3;

    private List<GeocodingResult> results = Collections.emptyList();

    public int getRowCount() {
        return results.size();
    }

    public int getColumnCount() {
        return 4;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        GeocodingResult result = getResult(rowIndex);
        return switch (columnIndex) {
            case NAME_COLUMN, LONGITUDE_COLUMN, LATITUDE_COLUMN -> result.position();
            case GEOCODING_SERVICE_COLUMN -> result.geocodingServiceName();
            default -> null;
        };
    }

    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case NAME_COLUMN, LONGITUDE_COLUMN, LATITUDE_COLUMN -> NavigationPosition.class;
            case GEOCODING_SERVICE_COLUMN -> String.class;
            default -> Object.class;
        };
    }

    public GeocodingResult getResult(int rowIndex) {
        return results.get(rowIndex);
    }

    public List<GeocodingResult> getResults() {
        return new ArrayList<>(results);
    }

    public void setResults(List<GeocodingResult> results) {
        this.results = results != null ? new ArrayList<>(results) : Collections.emptyList();
        fireTableDataChanged();
    }
}


