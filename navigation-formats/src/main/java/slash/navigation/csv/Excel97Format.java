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

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import slash.navigation.base.ParserContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Reads Excel 97-2008 (.xls) files.
 *
 * @author Christian Pesch
 */

public class Excel97Format extends ExcelFormat {
    public String getName() {
        return "Excel 97-2008 (" + getExtension() + ")";
    }

    public String getExtension() {
        return ".xls";
    }

    public void read(InputStream source, ParserContext<ExcelRoute> context) throws Exception {
        try (Workbook workbook = new HSSFWorkbook(source, true)) {
            parseWorkbook(workbook, context);
        }
    }

    public void write(ExcelRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        Workbook workbook = new HSSFWorkbook();
        populateWorkbook(workbook, route, startIndex, endIndex);
        workbook.write(target);
    }
}
