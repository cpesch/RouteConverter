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

import static slash.navigation.converter.gui.helpers.PositionHelper.formatSize;

/**
 * Shows notifications via the {@link NotificationManager} upon {@link DownloadListener} events on {@link Download}s.
 *
 * @author Christian Pesch
 */
public class DownloadNotifier implements DownloadListener {
    private Action getAction() {
        return Application.getInstance().getContext().getActionManager().get("show-downloads");
    }

    private ResourceBundle getBundle() {
        return Application.getInstance().getContext().getBundle();
    }

    private void showNotification(String message) {
        Application.getInstance().getContext().getNotificationManager().showNotification(message, getAction());
    }

    public void initialized(Download download) {}

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
