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

import slash.navigation.base.BaseRoute;
import slash.navigation.converter.gui.comparators.DateTimeComparator;
import slash.navigation.converter.gui.comparators.DescriptionComparator;
import slash.navigation.converter.gui.renderer.DateColumnTableCellEditor;
import slash.navigation.converter.gui.renderer.DateTimeColumnTableCellEditor;
import slash.navigation.converter.gui.renderer.DescriptionColumnTableCellEditor;
import slash.navigation.converter.gui.renderer.DistanceColumnTableCellRenderer;
import slash.navigation.converter.gui.renderer.ElevationColumnTableCellEditor;
import slash.navigation.converter.gui.renderer.ElevationDeltaColumnTableCellRenderer;
import slash.navigation.converter.gui.renderer.LatitudeColumnTableCellEditor;
import slash.navigation.converter.gui.renderer.LongitudeColumnTableCellEditor;
import slash.navigation.converter.gui.renderer.PhotoColumnTableCellRenderer;
import slash.navigation.converter.gui.renderer.PositionsTableHeaderRenderer;
import slash.navigation.converter.gui.renderer.PressureColumnTableCellEditor;
import slash.navigation.converter.gui.renderer.SpeedColumnTableCellEditor;
import slash.navigation.converter.gui.renderer.TemperatureColumnTableCellEditor;
import slash.navigation.converter.gui.renderer.TimeColumnTableCellEditor;

import javax.swing.table.TableColumnModel;

import static slash.navigation.converter.gui.models.LocalActionConstants.POSITIONS;
import static slash.navigation.converter.gui.models.PositionColumns.DATE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.DATE_TIME_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.DESCRIPTION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.DISTANCE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.DISTANCE_DIFFERENCE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_ASCEND_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_DESCEND_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_DIFFERENCE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LATITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LONGITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.PHOTO_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.PRESSURE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.SPEED_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.TEMPERATURE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.TIME_COLUMN_INDEX;
import static slash.navigation.gui.helpers.UIHelper.getMaxWidth;

/**
 * Acts as a {@link TableColumnModel} for the positions of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class PositionsTableColumnModel extends AbstractTableColumnModel {
    public PositionsTableColumnModel() {
        super(POSITIONS);
        PositionsTableHeaderRenderer headerRenderer = new PositionsTableHeaderRenderer();
        predefineColumn(DESCRIPTION_COLUMN_INDEX, "description", null, true, new DescriptionColumnTableCellEditor(), headerRenderer, new DescriptionComparator());
        predefineColumn(PHOTO_COLUMN_INDEX, "photo", null, false, new PhotoColumnTableCellRenderer(), headerRenderer);
        predefineColumn(DATE_TIME_COLUMN_INDEX, "datetime", getMaxWidth(getExampleDateTimeFromCurrentLocale(), 10), false, new DateTimeColumnTableCellEditor(), headerRenderer, new DateTimeComparator());
        predefineColumn(DATE_COLUMN_INDEX, "date", getMaxWidth(getExampleDateFromCurrentLocale(), 10), false, new DateColumnTableCellEditor(), headerRenderer);
        predefineColumn(TIME_COLUMN_INDEX, "time", getMaxWidth(getExampleTimeFromCurrentLocale(), 10), false, new TimeColumnTableCellEditor(), headerRenderer);
        predefineColumn(SPEED_COLUMN_INDEX, "speed", getMaxWidth("999 Km/h", 15), false, new SpeedColumnTableCellEditor(), headerRenderer);
        predefineColumn(TEMPERATURE_COLUMN_INDEX, "temperature", getMaxWidth("100°C", 5), false, new TemperatureColumnTableCellEditor(), headerRenderer);
        predefineColumn(PRESSURE_COLUMN_INDEX, "pressure", getMaxWidth("1150 hPa", 5), false, new PressureColumnTableCellEditor(), headerRenderer);
        predefineColumn(DISTANCE_COLUMN_INDEX, "distance", getMaxWidth("12345 Km", 7), false, new DistanceColumnTableCellRenderer(), headerRenderer);
        predefineColumn(DISTANCE_DIFFERENCE_COLUMN_INDEX, "distance-difference", getMaxWidth("12345 Km", 7), false, new DistanceColumnTableCellRenderer(), headerRenderer);
        predefineColumn(LONGITUDE_COLUMN_INDEX, "longitude", 84, true, new LongitudeColumnTableCellEditor(), headerRenderer);
        predefineColumn(LATITUDE_COLUMN_INDEX, "latitude", 84, true, new LatitudeColumnTableCellEditor(), headerRenderer);
        predefineColumn(ELEVATION_COLUMN_INDEX, "elevation", getMaxWidth("9999 m", 5), true, new ElevationColumnTableCellEditor(), headerRenderer);
        predefineColumn(ELEVATION_ASCEND_COLUMN_INDEX, "elevation-ascend", getMaxWidth("9999 m", 5), false, new ElevationDeltaColumnTableCellRenderer(), headerRenderer);
        predefineColumn(ELEVATION_DESCEND_COLUMN_INDEX, "elevation-descend", getMaxWidth("9999 m", 5), false, new ElevationDeltaColumnTableCellRenderer(), headerRenderer);
        predefineColumn(ELEVATION_DIFFERENCE_COLUMN_INDEX, "elevation-difference", getMaxWidth("999 m", 4), false, new ElevationDeltaColumnTableCellRenderer(), headerRenderer);
        initializeColumns();
    }
}
