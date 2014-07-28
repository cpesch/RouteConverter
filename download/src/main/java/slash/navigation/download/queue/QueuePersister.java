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

package slash.navigation.download.queue;

import slash.navigation.download.Action;
import slash.navigation.download.Download;
import slash.navigation.download.State;
import slash.navigation.download.actions.Checksum;
import slash.navigation.download.queue.binding.DownloadType;
import slash.navigation.download.queue.binding.ObjectFactory;
import slash.navigation.download.queue.binding.QueueType;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static slash.common.io.Transfer.formatTime;
import static slash.common.io.Transfer.parseTime;
import static slash.navigation.download.queue.QueueUtil.marshal;
import static slash.navigation.download.queue.QueueUtil.unmarshal;

/**
 * Loads and stores {@link Download}s
 *
 * @author Christian Pesch
 */

public class QueuePersister {
    private final File file;

    public QueuePersister(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public List<Download> load() throws IOException, JAXBException {
        if (!file.exists())
            return null;
        QueueType queueType = unmarshal(new FileInputStream(file));
        return asDownloads(queueType);
    }

    private List<Download> asDownloads(QueueType queueType) {
        List<Download> result = new ArrayList<Download>();
        for (DownloadType downloadType : queueType.getDownload())
            result.add(asDownload(downloadType));
        return result;
    }

    private Download asDownload(DownloadType downloadType) {
        return new Download(downloadType.getDescription(), downloadType.getUrl(),
                new Checksum(downloadType.getChecksum(), downloadType.getSize(), parseTime(downloadType.getTimestamp())),
                Action.valueOf(downloadType.getAction()), new File(downloadType.getTarget()),
                parseTime(downloadType.getLastSync()), State.valueOf(downloadType.getState()),
                new File(downloadType.getTempFile()), parseTime(downloadType.getLastModified()),
                downloadType.getContentLength());
    }

    public void save(List<Download> downloads) throws IOException, JAXBException {
        QueueType queueType = asQueueType(downloads);
        marshal(queueType, new FileOutputStream(file));
    }

    private QueueType asQueueType(List<Download> downloads) {
        QueueType queueType = new ObjectFactory().createQueueType();
        for (Download download : downloads)
            queueType.getDownload().add(asDownloadType(download));
        return queueType;
    }

    private DownloadType asDownloadType(Download download) {
        DownloadType downloadType = new ObjectFactory().createDownloadType();
        downloadType.setDescription(download.getDescription());
        downloadType.setUrl(download.getUrl());
        downloadType.setSize(download.getChecksum().getSize());
        downloadType.setChecksum(download.getChecksum().getChecksum());
        downloadType.setTimestamp(formatTime(download.getChecksum().getTimestamp(), true));
        downloadType.setState(download.getState().name());
        downloadType.setLastSync(formatTime(download.getLastSync(), true));
        downloadType.setAction(download.getAction().name());
        downloadType.setTarget(download.getTarget().getPath());
        downloadType.setTempFile(download.getTempFile().getPath());
        downloadType.setLastModified(formatTime(download.getLastModified(), true));
        downloadType.setContentLength(download.getContentLength());
        return downloadType;
    }
}
