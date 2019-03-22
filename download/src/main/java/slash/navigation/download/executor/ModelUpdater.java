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
package slash.navigation.download.executor;

import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.actions.CopierListener;

import static slash.navigation.download.State.Downloading;
import static slash.navigation.download.State.Resuming;

/**
 * Propagates {@link CopierListener} of a {@link Download} to the {@link DownloadManager}.
 *
 * @author Christian Pesch
 */
public class ModelUpdater implements CopierListener {
    private final Download download;
    private final DownloadManager downloadManager;

    public ModelUpdater(Download download, DownloadManager downloadManager) {
        this.download = download;
        this.downloadManager = downloadManager;
    }

    public void expectingBytes(long byteCount) {
        download.setExpectedBytes(byteCount);
    }

    public void processedBytes(long byteCount) {
        download.setProcessedBytes(byteCount);
        downloadManager.updateDownload(download);
        if (download.getState().equals(Downloading) || download.getState().equals(Resuming))
            downloadManager.fireProgressed(download);
    }
}
