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
import slash.navigation.download.State;
import slash.navigation.feedback.domain.RouteFeedback;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.HashSet;
import java.util.Set;

import static slash.navigation.download.State.Succeeded;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;

/**
 * Sends checksums via the {@link RouteFeedback} upon {@link State#Succeeded} on {@link Download}s.
 *
 * @author Christian Pesch
 */
public class ChecksumSender implements TableModelListener {
    private final Set<Download> alreadyProcessed = new HashSet<>();

    public void tableChanged(TableModelEvent e) {
        if (isFirstToLastRow(e))
            return;

        for (int i = e.getFirstRow(); i <= e.getLastRow(); i++) {
            Download download = RouteConverter.getInstance().getDataSourceManager().
                    getDownloadManager().getModel().getDownloads().get(i);
            process(download);
        }
    }

    private void process(Download download) {
        if (download.getState().equals(Succeeded)) {
            if (alreadyProcessed.contains(download))
                return;

            final DataSource dataSource = RouteConverter.getInstance().getDataSourceManager().
                    getDataSourceService().getDataSourceByUrlPrefix(download.getUrl());
            if (dataSource != null) {
                alreadyProcessed.add(download);
                RouteConverter.getInstance().sendChecksums(dataSource, download);
            }
        }
    }
}
