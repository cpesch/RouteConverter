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
package slash.navigation.csv;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.NavigationPosition;

import java.util.ArrayList;
import java.util.List;

import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * The base of all Excel formats.
 *
 * @author Christian Pesch
 */

public abstract class ExcelFormat extends BaseNavigationFormat<ExcelRoute> {
    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> ExcelRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new ExcelRoute(this, characteristics, name, (List<Wgs84Position>) positions);
    }

    protected void parseWorkbook(Workbook workbook, ParserContext<ExcelRoute> context) {
        if (workbook.getNumberOfSheets() == 0)
            return;

        Sheet sheet = workbook.getSheetAt(0);
        if (sheet.getPhysicalNumberOfRows() < 2)
            return;

        Row header = sheet.getRow(0);
        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
            rows.add(sheet.getRow(i));
        }
        context.appendRoute(new ExcelRoute(this, Waypoints, header, rows));
    }
}
