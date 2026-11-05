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
package slash.navigation.excel;

import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;
import java.util.List;

/**
 * A workbook together with the position lists whose sheet lives in it; a sheet of the
 * workbook that no {@link ExcelRoute} of the holder uses, e.g. a notes sheet, is never touched.
 *
 * @author Christian Pesch
 */

class ExcelWorkbook {
    private final Workbook workbook;
    private final List<ExcelRoute> routes = new ArrayList<>();

    ExcelWorkbook(Workbook workbook) {
        this.workbook = workbook;
    }

    Workbook getWorkbook() {
        return workbook;
    }

    List<ExcelRoute> getRoutes() {
        return routes;
    }

    void addRoute(ExcelRoute route) {
        routes.add(route);
    }

    void removeRoute(ExcelRoute route) {
        routes.remove(route);
    }
}
