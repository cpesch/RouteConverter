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

import slash.navigation.datasources.Source;
import slash.navigation.datasources.binding.SourceType;

import java.util.List;

/**
 * Implementation of {@link Source} based on a {@link SourceType}.
 *
 * @author Christian Pesch
 */

public class SourceImpl implements Source {
    private final SourceType sourceType;

    public SourceImpl(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getUrl() {
        return sourceType.getUrl();
    }

    public Integer getLevel() {
        return sourceType.getLevel();
    }

    public List<String> getIncludes() {
        return sourceType.getInclude();
    }

    public List<String> getExcludes() {
        return sourceType.getExclude();
    }
}
