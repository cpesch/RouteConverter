/*
    This file is part of BaseRouteConverter.

    BaseRouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    BaseRouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BaseRouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.converter.gui.helpers;

import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.download.Checksum;
import slash.navigation.download.ChecksumReportPolicy;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadListener;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.download.State;
import slash.navigation.feedback.domain.RouteFeedback;

/**
 * Sends checksums via the {@link RouteFeedback} upon {@link State#Succeeded} on {@link Download}s.
 *
 * @author Christian Pesch
 */
public class ChecksumSender implements DownloadListener {
    private void sendChecksums(Download download) {
        BaseRouteConverter.getInstance().sendChecksums(download);
    }

    public void initialized(Download download) {}
    public void progressed(Download download) {}

    public void failed(Download download) {
        FileAndChecksum file = download.getFile();
        if (file.getActualChecksum() == null)
            return;
        // the file's checksum is only a new known-good build if the transfer provably completed
        // and validation failed on the complete content; anything else is a broken download whose
        // checksum must not be published (see GitHub #382)
        if (ChecksumReportPolicy.isReportableChecksum(download.getState(),
                download.getAnnouncedContentLength(), file.getActualChecksum().getContentLength(),
                download.getAnnouncedLastModified(), lastModifiedMillis(file.getActualChecksum())))
            sendChecksums(download);
    }

    private Long lastModifiedMillis(Checksum checksum) {
        return checksum.getLastModified() != null ? checksum.getLastModified().getTimeInMillis() : null;
    }

    public void succeeded(Download download) {
        sendChecksums(download);
    }
}
