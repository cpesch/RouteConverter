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
package slash.navigation.download.tools.helpers;

import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.Source;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds wget mirror commands from a {@link DataSource} and its {@link Source}.
 *
 * Target URL = {@code source.getUrl()} when set, else {@code datasource.getBaseUrl()}.
 * Accept patterns = {@code source.getIncludes()}; when empty, derived from distinct
 * uri extensions of files/maps/themes on the datasource.
 *
 * @author Christian Pesch
 */
public class WgetCommandBuilder {

    public List<String> buildCommand(DataSource dataSource, Source source, Path mirrorRoot) {
        if (source == null)
            throw new IllegalArgumentException("Datasource " + dataSource.getId() + " has no <source>");
        String target = source.getUrl() != null && !source.getUrl().isBlank() ? source.getUrl() : dataSource.getBaseUrl();
        if (target == null)
            throw new IllegalArgumentException("Datasource " + dataSource.getId() + " has no mirror URL");

        List<String> result = new ArrayList<>();
        result.add("wget");
        result.add("-m");
        result.add("-np");
        result.add("-e");
        result.add("robots=off");
        result.add("--wait");
        result.add("1");
        result.add("-P");
        result.add(mirrorRoot.toString());
        if (source.getLevel() != null) {
            result.add("-l");
            result.add(String.valueOf(source.getLevel()));
        }
        for (String accept : resolveAccepts(dataSource, source)) {
            result.add("--accept");
            result.add(accept);
        }
        result.add(target);
        return result;
    }

    /**
     * Resolve wget --accept patterns from the source includes, or derive from the
     * distinct uri extensions on the datasource when includes are empty.
     */
    Set<String> resolveAccepts(DataSource dataSource, Source source) {
        Set<String> result = new LinkedHashSet<>();
        if (source.getIncludes() != null && !source.getIncludes().isEmpty()) {
            result.addAll(source.getIncludes());
            return result;
        }
        addExtensionGlobs(result, dataSource.getFiles());
        addExtensionGlobs(result, dataSource.getMaps());
        addExtensionGlobs(result, dataSource.getThemes());
        return result;
    }

    private void addExtensionGlobs(Set<String> result, List<? extends Downloadable> downloadables) {
        if (downloadables == null)
            return;
        for (Downloadable d : downloadables) {
            String uri = d.getUri();
            int dot = uri.lastIndexOf('.');
            if (dot >= 0 && dot < uri.length() - 1)
                result.add("*" + uri.substring(dot));
        }
    }

    public String formatCommand(List<String> command) {
        List<String> quoted = new ArrayList<>();
        for (String argument : command)
            quoted.add(quote(argument));
        return String.join(" ", quoted);
    }

    private String quote(String value) {
        if (value.matches("[A-Za-z0-9_./:=+-]+"))
            return value;
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
