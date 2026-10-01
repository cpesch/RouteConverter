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

import slash.navigation.download.Download;
import slash.navigation.download.DownloadListener;
import slash.navigation.gui.Application;
import slash.navigation.gui.notifications.NotificationManager;

import javax.swing.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import static slash.common.io.Transfer.formatSize;
import static slash.navigation.download.Action.Head;

/**
 * Shows notifications via the {@link NotificationManager} upon {@link DownloadListener} events on {@link Download}s.
 *
 * @author Christian Pesch
 */
public class DownloadNotifier implements DownloadListener {
    private Action getAction() {
        return Application.getInstance().getContext().getActionManager().get("show-downloads");
    }

    ResourceBundle getBundle() {
        return Application.getInstance().getContext().getBundle();
    }

    void showNotification(String message) {
        Application.getInstance().getContext().getNotificationManager().showNotification(message, getAction());
    }

    // shown before the first byte arrives, so a slow or stalling server is visible right
    // away and not only once the download fails; a Head request only checks for updates
    // and transfers no content, so it gets no notification
    public void initialized(Download download) {
        if (download.getAction() == Head)
            return;
        String message = MessageFormat.format(getBundle().getString("download-started"), download.getUrl());
        showNotification(message);
    }

    public void progressed(Download download) {
        Integer percentage = download.getPercentage();
        if(percentage != null && percentage == 0 || download.getProcessedBytes() == 0)
            return;

        String message = MessageFormat.format(getBundle().getString("download-progressed"),
                percentage != null ? percentage + "%" : formatSize(download.getProcessedBytes()),
                percentage != null ? formatSize(download.getExpectedBytes()) : "", download.getDescription());
        showNotification(message);
    }

    public void failed(Download download) {
        String message = MessageFormat.format(getBundle().getString("download-failed"), download.getDescription());
        showNotification(message);
    }

    public void succeeded(Download download) {
        String message = MessageFormat.format(getBundle().getString("download-succeeded"), download.getDescription());
        showNotification(message);
    }
}
