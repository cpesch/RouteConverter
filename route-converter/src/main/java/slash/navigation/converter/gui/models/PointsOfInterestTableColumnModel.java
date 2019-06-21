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
import slash.navigation.converter.gui.comparators.DescriptionComparator;
import slash.navigation.converter.gui.renderer.*;

import javax.swing.table.TableColumnModel;

import static slash.navigation.converter.gui.models.LocalActionConstants.POINTS_OF_INTEREST;
import static slash.navigation.converter.gui.models.PositionColumns.*;
import static slash.navigation.gui.helpers.UIHelper.getMaxWidth;

/**
 * Acts as a {@link TableColumnModel} for the points of interest of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class PointsOfInterestTableColumnModel extends AbstractTableColumnModel {
    public PointsOfInterestTableColumnModel() {
        super(POINTS_OF_INTEREST);
        PositionsTableHeaderRenderer headerRenderer = new PositionsTableHeaderRenderer();
        predefineColumn(WAYPOINT_TYPE_COLUMN_INDEX, "waypoint-type", getMaxWidth(POINTS_OF_INTEREST, 10), true, new WaypointTypeColumnTableCellEditor(), headerRenderer);
        predefineColumn(DESCRIPTION_COLUMN_INDEX, "description", null, true, new DescriptionColumnTableCellEditor(), headerRenderer, new DescriptionComparator());
        predefineColumn(PHOTO_COLUMN_INDEX, "photo", null, false, new PhotoColumnTableCellRenderer(), headerRenderer);
        predefineColumn(DATE_TIME_COLUMN_INDEX, "datetime", getMaxWidth(getExampleDateTimeFromCurrentLocale(), 10), false, new DateTimeColumnTableCellEditor(), headerRenderer);
        predefineColumn(LONGITUDE_COLUMN_INDEX, "longitude", 84, true, new LongitudeColumnTableCellEditor(), headerRenderer);
        predefineColumn(LATITUDE_COLUMN_INDEX, "latitude", 84, true, new LatitudeColumnTableCellEditor(), headerRenderer);
        predefineColumn(ELEVATION_COLUMN_INDEX, "elevation", getMaxWidth("9999 m", 5), false, new ElevationColumnTableCellEditor(), headerRenderer);
        initializeColumns();
    }
}
