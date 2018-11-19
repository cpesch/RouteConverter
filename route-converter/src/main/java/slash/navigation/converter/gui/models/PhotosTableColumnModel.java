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

import slash.navigation.converter.gui.renderer.ExifColumnTableCellRenderer;
import slash.navigation.converter.gui.renderer.GpsColumnTableCellRenderer;
import slash.navigation.converter.gui.renderer.PhotoColumnTableCellRenderer;
import slash.navigation.converter.gui.renderer.PositionsTableHeaderRenderer;

import javax.swing.table.TableColumnModel;

import static slash.navigation.converter.gui.models.LocalActionConstants.PHOTOS;
import static slash.navigation.converter.gui.models.PositionColumns.*;
import static slash.navigation.gui.helpers.UIHelper.getMaxWidth;

/**
 * Acts as a {@link TableColumnModel} for the Photos.
 *
 * @author Christian Pesch
 */

public class PhotosTableColumnModel extends AbstractTableColumnModel {
    public PhotosTableColumnModel() {
        super(PHOTOS);
        PositionsTableHeaderRenderer headerRenderer = new PositionsTableHeaderRenderer();
        predefineColumn(PHOTO_COLUMN_INDEX, "photo", null, true, new PhotoColumnTableCellRenderer(), headerRenderer);
        predefineColumn(EXIF_COLUMN_INDEX, "exif", getMaxWidth("Belichtungszeit: 1/1000s", 5), true, new ExifColumnTableCellRenderer(), headerRenderer);
        predefineColumn(GPS_COLUMN_INDEX, "gps", getMaxWidth("Laengengrad: 111.223344", 5), true, new GpsColumnTableCellRenderer(), headerRenderer);
        initializeColumns();
    }
}
