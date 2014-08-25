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
package slash.navigation.download.tools;

import slash.navigation.datasources.binding.FileType;
import slash.navigation.datasources.binding.FragmentType;
import slash.navigation.datasources.binding.MapType;
import slash.navigation.datasources.binding.ThemeType;
import slash.navigation.download.tools.base.FileDataSourcesXmlGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Creates a HGT data sources XML from a file system mirror.
 *
 * @author Christian Pesch
 */

public class CreateHgtDataSourcesXml extends FileDataSourcesXmlGenerator {
    private static final Pattern KEY_PATTERN = Pattern.compile(".*([N|S]\\d{2}[E|W]\\d{3}).*", CASE_INSENSITIVE);

    private String extractKey(String string) {
        Matcher matcher = KEY_PATTERN.matcher(string);
        if (!matcher.matches()) {
            System.err.println(getClass().getSimpleName() + ": " + string + " does not match key pattern");
            return null;
        }
        return matcher.group(1).toUpperCase();
    }

    protected void parseFile(File file, List<FileType> fileTypes, List<MapType> mapTypes, List<ThemeType> themeTypes, File baseDirectory) throws IOException {
        String uri = relativizeUri(file, baseDirectory);
        List<FragmentType> fragmentTypes = new ArrayList<>();

        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(new FileInputStream(file));
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    String key = extractKey(entry.getName());
                    if (key != null) {
                        System.out.println(getClass().getSimpleName() + ": " + key + " maps to " + uri);
                        fragmentTypes.add(createFragmentType(key, entry, zipInputStream));

                        // do not close zip input stream
                        zipInputStream.closeEntry();
                    }
                }
                entry = zipInputStream.getNextEntry();
            }
        } finally {
            if (zipInputStream != null)
                closeQuietly(zipInputStream);
        }

        FileType fileType = createFileType(uri, file, null);
        fileType.getFragment().addAll(sortFragmentTypes(fragmentTypes));
        fileTypes.add(fileType);
    }

    public static void main(String[] args) throws Exception {
        new CreateHgtDataSourcesXml().run(args);
    }
}
