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
package slash.navigation.datasources.impl;

import slash.navigation.common.BoundingBox;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.File;
import slash.navigation.datasources.binding.FileType;

import static slash.navigation.datasources.helpers.DataSourcesUtil.asBoundingBox;

/**
 * Implementation of a {@link File} based on a {@link FileType}.
 *
 * @author Christian Pesch
 */

public class FileImpl extends DownloadableImpl implements File {
    public FileImpl(FileType fileType, DataSource dataSource) {
        super(fileType, dataSource);
    }

    private FileType getFileType() {
        return FileType.class.cast(getDownloadableType());
    }

    public BoundingBox getBoundingBox() {
        return asBoundingBox(getFileType().getBoundingBox());
    }
}
