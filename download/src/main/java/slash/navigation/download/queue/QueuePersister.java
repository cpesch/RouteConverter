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

import slash.navigation.download.*;
import slash.navigation.download.queue.binding.*;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static slash.common.helpers.ExceptionHelper.printStackTrace;
import static slash.common.io.Transfer.formatXMLTime;
import static slash.common.io.Transfer.parseXMLTime;
import static slash.navigation.download.queue.QueueUtil.marshal;
import static slash.navigation.download.queue.QueueUtil.unmarshal;

/**
 * Loads and stores {@link Download}s
 *
 * @author Christian Pesch
 */

public class QueuePersister {

    public List<Download> load(File file) throws IOException {
        if (!file.exists())
            return null;

        QueueType queueType;
        try (InputStream inputStream = new FileInputStream(file)) {
            queueType = unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new IOException("Cannot unmarshall " + file + ": " + e, e);
        }
        return asDownloads(queueType);
    }

    private List<Download> asDownloads(QueueType queueType) {
        List<Download> result = new ArrayList<>();
        for (DownloadType downloadType : queueType.getDownload())
            result.add(asDownload(downloadType));
        return result;
    }

    private Download asDownload(DownloadType downloadType) {
        return new Download(downloadType.getDescription(), downloadType.getUrl(), Action.valueOf(downloadType.getAction()),
                new FileAndChecksum(new File(downloadType.getDownloadable().getTarget()), asChecksum(downloadType.getDownloadable().getChecksum())),
                asFileAndChecksums(downloadType.getDownloadable().getFragment()),
                downloadType.getETag(), State.valueOf(downloadType.getState()), new File(downloadType.getTempFile()));
    }

    private List<FileAndChecksum> asFileAndChecksums(List<FragmentType> fragmentTypes) {
        List<FileAndChecksum> files = new ArrayList<>();
        for (FragmentType fragmentType : fragmentTypes)
            files.add(new FileAndChecksum(new File(fragmentType.getTarget()), asChecksum(fragmentType.getChecksum())));
        return files;
    }

    private Checksum asChecksum(ChecksumType checksumType) {
        if(checksumType == null)
            return null;

        return new Checksum(parseXMLTime(checksumType.getLastModified()), checksumType.getContentLength(), checksumType.getSha1());
    }

    public void save(File file, List<Download> downloads) throws IOException {
        QueueType queueType = asQueueType(downloads);
        try {
            marshal(queueType, new FileOutputStream(file));
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + file + ": " + e + "\n" + printStackTrace(e), e);
        }
    }

    private QueueType asQueueType(List<Download> downloads) {
        QueueType queueType = new ObjectFactory().createQueueType();
        for (Download download : downloads) {
            // make more robust against strange effects seen on chinese Macs
            if(download == null)
                continue;
            queueType.getDownload().add(asDownloadType(download));
        }
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
        downloadableType.setChecksum(asChecksumType(download.getFile().getExpectedChecksum()));
        downloadableType.setTarget(download.getFile().getFile().getPath());
        List<FragmentType> fragmentTypes = asFragmentTypes(download.getFragments());
        if (fragmentTypes != null)
            downloadableType.getFragment().addAll(fragmentTypes);
        return downloadableType;
    }

    private ChecksumType asChecksumType(Checksum checksum) {
        if (checksum == null)
            return null;

        ChecksumType checksumType = new ObjectFactory().createChecksumType();
        checksumType.setContentLength(checksum.getContentLength());
        checksumType.setLastModified(formatXMLTime(checksum.getLastModified(), true));
        checksumType.setSha1(checksum.getSHA1());
        return checksumType;
    }

    private List<FragmentType> asFragmentTypes(List<FileAndChecksum> fragments) {
        if (fragments == null)
            return null;

        List<FragmentType> fragmentTypes = new ArrayList<>();
        for (FileAndChecksum fragment : fragments) {
            fragmentTypes.add(asFragmentType(fragment));
        }
        return fragmentTypes;
    }

    private FragmentType asFragmentType(FileAndChecksum fileAndChecksum) {
        FragmentType fragmentType = new ObjectFactory().createFragmentType();
        fragmentType.setChecksum(asChecksumType(fileAndChecksum.getExpectedChecksum()));
        fragmentType.setTarget(fileAndChecksum.getFile().getPath());
        return fragmentType;
    }
}
