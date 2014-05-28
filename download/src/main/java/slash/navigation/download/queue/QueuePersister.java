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

import slash.common.type.CompactCalendar;
import slash.navigation.download.Action;
import slash.navigation.download.Checksum;
import slash.navigation.download.Download;
import slash.navigation.download.State;
import slash.navigation.download.queue.binding.*;

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

    public Result load(File file) throws IOException {
        if (!file.exists())
            return null;

        QueueType queueType;
        try {
            queueType = unmarshal(new FileInputStream(file));
        } catch (JAXBException e) {
            e.printStackTrace();
            throw new IOException("Cannot unmarshall " + file + ": " + e, e);
        }
        return new Result(asDownloads(queueType), parseTime(queueType.getLastSync()));
    }

    public static class Result {
        private final CompactCalendar lastSync;
        private final List<Download> downloads;

        public Result(List<Download> downloads, CompactCalendar lastSync) {
            this.downloads = downloads;
            this.lastSync = lastSync;
        }

        public List<Download> getDownloads() {
            return downloads;
        }

        public CompactCalendar getLastSync() {
            return lastSync;
        }
    }

    private List<Download> asDownloads(QueueType queueType) {
        List<Download> result = new ArrayList<>();
        for (DownloadType downloadType : queueType.getDownload())
            result.add(asDownload(downloadType));
        return result;
    }

    private Download asDownload(DownloadType downloadType) {
        return new Download(downloadType.getDescription(), downloadType.getUrl(), Action.valueOf(downloadType.getAction()),
                new File(downloadType.getDownloadable().getTarget()), asChecksum(downloadType.getDownloadable().getChecksum()),
                asFiles(downloadType.getDownloadable().getFragment()), asChecksums(downloadType.getDownloadable().getFragment()),
                downloadType.getETag(), State.valueOf(downloadType.getState()), new File(downloadType.getTempFile()));
    }

    private List<File> asFiles(List<FragmentType> fragmentTypes) {
        List<File> files = new ArrayList<>();
        for (FragmentType fragmentType : fragmentTypes)
            files.add(new File(fragmentType.getTarget()));
        return files;
    }

    private List<Checksum> asChecksums(List<FragmentType> fragmentTypes) {
        List<Checksum> checksums = new ArrayList<>();
        for (FragmentType fragmentType : fragmentTypes)
            checksums.add(asChecksum(fragmentType.getChecksum()));
        return checksums;
    }

    private Checksum asChecksum(ChecksumType checksumType) {
        if(checksumType == null)
            return null;

        return new Checksum(parseTime(checksumType.getLastModified()), checksumType.getContentLength(), checksumType.getSha1());
    }

    public void save(File file, List<Download> downloads, CompactCalendar lastSync) throws IOException {
        QueueType queueType = asQueueType(downloads);
        queueType.setLastSync(formatTime(lastSync));
        try {
            marshal(queueType, new FileOutputStream(file));
        } catch (JAXBException e) {
            e.printStackTrace();
            throw new IOException("Cannot marshall " + file + ": " + e, e);
        }
    }

    private QueueType asQueueType(List<Download> downloads) {
        QueueType queueType = new ObjectFactory().createQueueType();
        for (Download download : downloads)
            queueType.getDownload().add(asDownloadType(download));
        return queueType;
    }

    private DownloadType asDownloadType(Download download) {
        DownloadType downloadType = new ObjectFactory().createDownloadType();
        downloadType.setDownloadable(asDownloadableType(download));
        downloadType.setDescription(download.getDescription());
        downloadType.setUrl(download.getUrl());
        downloadType.setAction(download.getAction().name());
        downloadType.setState(download.getState().name());
        downloadType.setETag(download.getETag());
        downloadType.setTempFile(download.getTempFile().getPath());
        return downloadType;
    }

    private DownloadableType asDownloadableType(Download download) {
        DownloadableType downloadableType = new ObjectFactory().createDownloadableType();
        downloadableType.setChecksum(asChecksumType(download.getFileChecksum()));
        downloadableType.setTarget(download.getFileTarget().getPath());
        List<FragmentType> fragmentTypes = asFragmentTypes(download.getFragmentTargets(), download.getFragmentChecksums());
        if (fragmentTypes != null)
            downloadableType.getFragment().addAll(fragmentTypes);
        return downloadableType;
    }

    private ChecksumType asChecksumType(Checksum checksum) {
        if (checksum == null)
            return null;

        ChecksumType checksumType = new ObjectFactory().createChecksumType();
        checksumType.setContentLength(checksum.getContentLength());
        checksumType.setLastModified(formatTime(checksum.getLastModified(), true));
        checksumType.setSha1(checksum.getSHA1());
        return checksumType;
    }

    private List<FragmentType> asFragmentTypes(List<File> targets, List<Checksum> checksums) {
        if (targets == null || checksums == null)
            return null;

        List<FragmentType> fragmentTypes = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            fragmentTypes.add(asFragmentType(targets.get(i), checksums.get(i)));
        }
        return fragmentTypes;
    }

    private FragmentType asFragmentType(File target, Checksum checksum) {
        FragmentType fragmentType = new ObjectFactory().createFragmentType();
        fragmentType.setChecksum(asChecksumType(checksum));
        fragmentType.setTarget(target.getPath());
        return fragmentType;
    }
}
