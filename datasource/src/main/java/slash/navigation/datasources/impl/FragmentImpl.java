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

import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.Fragment;
import slash.navigation.datasources.binding.ChecksumType;
import slash.navigation.datasources.binding.FragmentType;
import slash.navigation.download.Checksum;

import java.util.ArrayList;
import java.util.List;

import static slash.navigation.datasources.helpers.DataSourcesUtil.asChecksum;

/**
 * Implementation of a {@link Fragment} based on a {@link FragmentType}.
 *
 * @author Christian Pesch
 */

public class FragmentImpl<T extends Downloadable> implements Fragment<T> {
    private final FragmentType fragmentType;
    private final T downloadable;

    public FragmentImpl(FragmentType fragmentType, T downloadable) {
        this.fragmentType = fragmentType;
        this.downloadable = downloadable;
    }

    public T getDownloadable() {
        return downloadable;
    }

    public String getKey() {
        return fragmentType.getKey();
    }

    public List<Checksum> getChecksums() {
        List<Checksum> result = new ArrayList<>();
        for (ChecksumType checksumType : fragmentType.getChecksum())
            result.add(asChecksum(checksumType));
        return result;
    }

    public Checksum getLatestChecksum() {
        return Checksum.getLatestChecksum(getChecksums());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FragmentImpl<?> fragment = (FragmentImpl<?>) o;

        return getDownloadable().equals(fragment.getDownloadable()) && getKey().equals(fragment.getKey());
    }

    public int hashCode() {
        int result;
        result = getDownloadable().hashCode();
        result = 31 * result + getKey().hashCode();
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[key=" + getKey() + "]";
    }
}
