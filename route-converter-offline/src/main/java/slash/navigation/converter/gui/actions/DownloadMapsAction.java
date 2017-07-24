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

package slash.navigation.converter.gui.actions;

import slash.navigation.common.BoundingBox;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.gui.Application;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.gui.notifications.NotificationManager;
import slash.navigation.maps.MapManager;
import slash.navigation.maps.RemoteMap;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.text.MessageFormat.format;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.io.Files.printArrayToDialogString;

/**
 * {@link Action} that downloads {@link RemoteMap}s from the {@link MapManager}.
 *
 * @author Christian Pesch
 */

public class DownloadMapsAction extends FrameAction {
    private static ExecutorService executor = newCachedThreadPool();

    private final JTable table;
    private final MapManager mapManager;
    private final JCheckBox checkBoxDownloadRoutingData;
    private final JCheckBox checkBoxDownloadElevationData;

    public DownloadMapsAction(JTable table, MapManager mapManager,
                              JCheckBox checkBoxDownloadRoutingData, JCheckBox checkBoxDownloadElevationData) {
        this.table = table;
        this.mapManager = mapManager;
        this.checkBoxDownloadRoutingData = checkBoxDownloadRoutingData;
        this.checkBoxDownloadElevationData = checkBoxDownloadElevationData;
    }

    private Action getAction() {
        return Application.getInstance().getContext().getActionManager().get("show-downloads");
    }

    private NotificationManager getNotificationManager() {
        return Application.getInstance().getContext().getNotificationManager();
    }

    public void run() {
        final RouteConverter r = RouteConverter.getInstance();

        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0)
            return;
        final List<RemoteMap> selectedMaps = new ArrayList<>();
        List<String> selectedMapsNames = new ArrayList<>();
        final List<BoundingBox> selectedBoundingBoxes = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            RemoteMap map = mapManager.getDownloadableMapsModel().getMap(table.convertRowIndexToModel(selectedRow));
            selectedMaps.add(map);
            selectedMapsNames.add(map.getUrl());
            BoundingBox boundingBox = map.getBoundingBox();
            if (boundingBox != null)
                selectedBoundingBoxes.add(boundingBox);

        }
        getNotificationManager().showNotification(format(RouteConverter.getBundle().getString("download-started"), printArrayToDialogString(selectedMapsNames.toArray())), getAction());

        executor.execute(new Runnable() {
            public void run() {
                try {
                    mapManager.queueForDownload(selectedMaps);

                    if (checkBoxDownloadRoutingData.isSelected())
                        r.getRoutingServiceFacade().getRoutingService().downloadRoutingData(selectedBoundingBoxes);
                    if (checkBoxDownloadElevationData.isSelected())
                        r.getElevationServiceFacade().getElevationService().downloadElevationData(selectedBoundingBoxes);

                    mapManager.scanMaps();
                } catch (final Exception e) {
                    invokeLater(new Runnable() {
                        public void run() {
                            JFrame frame = r.getFrame();
                            showMessageDialog(frame, format(RouteConverter.getBundle().getString("scan-error"), e), frame.getTitle(), ERROR_MESSAGE);
                        }
                    });
                }
            }
        });
    }
}