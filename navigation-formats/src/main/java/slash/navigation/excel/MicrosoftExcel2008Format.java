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

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import slash.navigation.base.ParserContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.apache.poi.ss.util.WorkbookUtil.createSafeSheetName;

/**
 * Reads Microsoft Excel 2008 (.xlsx) files.
 *
 * @author Christian Pesch
 */

public class MicrosoftExcel2008Format extends ExcelFormat {
    public String getName() {
        return "Microsoft Excel 2008 (" + getExtension() + ")";
    }

    public String getExtension() {
        return ".xlsx";
    }

    Sheet createSheet(String name) {
        Workbook workbook = new XSSFWorkbook();
        return workbook.createSheet(createSafeSheetName(name));
    }

    public void read(InputStream source, ParserContext<ExcelRoute> context) throws Exception {
        Workbook workbook = new XSSFWorkbook(source);
        parseWorkbook(workbook, context);
        // do not close Workbook since this would close the underlying OPCPackage which has to be open to write later
    }

    public void write(ExcelRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        Workbook workbook = route.getWorkbook();
        try {
            workbook.write(target);
        }
        finally {
            target.flush();
            target.close();
        }
    }

    public void write(List<ExcelRoute> routes, OutputStream target) throws IOException {
        if(routes.size() == 0)
            return;

        Workbook workbook = routes.get(0).getWorkbook();
        if(!(workbook instanceof XSSFWorkbook))
            throw new IllegalArgumentException("Workbook " + workbook + " is not XSSFWorkbook");

        try {
            workbook.write(target);
        }
        finally {
            target.flush();
            target.close();
        }
    }
}
