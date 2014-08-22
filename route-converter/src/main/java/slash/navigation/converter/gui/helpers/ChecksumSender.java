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
package slash.navigation.converter.gui.helpers;

import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.datasources.DataSource;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadListener;
import slash.navigation.download.State;
import slash.navigation.feedback.domain.RouteFeedback;

/**
 * Sends checksums via the {@link RouteFeedback} upon {@link State#Succeeded} on {@link Download}s.
 *
 * @author Christian Pesch
 */
public class ChecksumSender implements DownloadListener {
    public void progressed(Download download, int percentage) {
    }

    public void failed(Download download) {
    }

    public void succeeded(Download download) {
        final DataSource dataSource = RouteConverter.getInstance().getDataSourceManager().
                getDataSourceService().getDataSourceByUrlPrefix(download.getUrl());
        if (dataSource != null)
            RouteConverter.getInstance().sendChecksums(dataSource, download);
    }
}
