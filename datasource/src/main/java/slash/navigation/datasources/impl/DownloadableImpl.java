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
package slash.navigation.datasources.impl;

import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.Fragment;
import slash.navigation.datasources.binding.ChecksumType;
import slash.navigation.datasources.binding.DownloadableType;
import slash.navigation.datasources.binding.FragmentType;
import slash.navigation.download.Checksum;

import java.util.ArrayList;
import java.util.List;

import static slash.navigation.datasources.helpers.DataSourcesUtil.asChecksum;

/**
 * Implementation of a {@link Downloadable} based on a {@link DownloadableType}.
 *
 * @author Christian Pesch
 */

public class DownloadableImpl implements Downloadable {
    private final DownloadableType downloadableType;
    private final DataSource dataSource;

    public DownloadableImpl(DownloadableType downloadableType, DataSource dataSource) {
        this.downloadableType = downloadableType;
        this.dataSource = dataSource;
    }

    protected DownloadableType getDownloadableType() {
        return downloadableType;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public String getUri() {
        return downloadableType.getUri();
    }

    public List<Checksum> getChecksums() {
        List<Checksum> result = new ArrayList<>();
        if (downloadableType != null)
            for (ChecksumType checksumType : downloadableType.getChecksum())
                result.add(asChecksum(checksumType));
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Fragment<Downloadable>> getFragments() {
        List<Fragment<Downloadable>> result = new ArrayList<>();
        if (downloadableType != null) {
            for (FragmentType fragmentType : downloadableType.getFragment()) {
                result.add(new FragmentImpl<Downloadable>(fragmentType, this));
            }
        }
        return result;
    }

    public Checksum getLatestChecksum() {
        return Checksum.getLatestChecksum(getChecksums());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Downloadable downloadable = (Downloadable) o;

        return getDataSource().equals(downloadable.getDataSource()) && getUri().equals(downloadable.getUri());
    }

    public int hashCode() {
        int result;
        result = getDataSource().hashCode();
        result = 31 * result + getUri().hashCode();
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[uri=" + getUri() + "]";
    }
}
