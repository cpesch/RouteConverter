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

import slash.navigation.download.datasources.binding.FileType;
import slash.navigation.download.datasources.binding.FragmentType;
import slash.navigation.download.datasources.binding.ObjectFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static slash.common.io.Files.generateChecksum;
import static slash.common.io.Transfer.formatTime;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Creates a HGT data sources XML from file system mirror.
 *
 * @author Christian Pesch
 */

public class CreateHgtDataSourcesXml extends BaseDataSourcesXmlGenerator {
    private static final Pattern KEY_PATTERN = Pattern.compile(".*([N|S]\\d{2}[E|W]\\d{3}).*", CASE_INSENSITIVE);

    private String extractKey(String string) {
        Matcher matcher = KEY_PATTERN.matcher(string);
        if (!matcher.matches()) {
            System.err.println(getClass().getSimpleName() + ": " + string + " does not match key pattern");
            return null;
        }
        return matcher.group(1).toUpperCase();
    }

    protected void parseFile(File file, List<FragmentType> fragmentTypes, List<FileType> fileTypes, File baseDirectory) throws IOException {
        String uri = relativizeUri(file, baseDirectory);
        fileTypes.add(createFileType(uri, file));

        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(new FileInputStream(file));
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    String key = extractKey(entry.getName());
                    if (key != null) {
                        System.out.println(getClass().getSimpleName() + ": " + key + " maps to " + uri);

                        String checksum = generateChecksum(zipInputStream);

                        FragmentType fragmentType = new ObjectFactory().createFragmentType();
                        fragmentType.setKey(key);
                        fragmentType.setUri(uri);
                        fragmentType.setSize(entry.getSize());
                        fragmentType.setChecksum(checksum);
                        fragmentType.setTimestamp(formatTime(fromMillis(entry.getTime()), true));
                        fragmentTypes.add(fragmentType);

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
    }

    public static void main(String[] args) throws Exception {
        new CreateHgtDataSourcesXml().run(args);
    }
}
