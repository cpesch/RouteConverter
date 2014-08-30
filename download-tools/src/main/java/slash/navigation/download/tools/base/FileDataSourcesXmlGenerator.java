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
package slash.navigation.download.tools.base;

import slash.navigation.datasources.binding.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.io.File.separatorChar;
import static java.lang.System.currentTimeMillis;

/**
 * Base for generation of data sources XML from file system mirror.
 *
 * @author Christian Pesch
 */

public abstract class FileDataSourcesXmlGenerator extends BaseDataSourcesXmlGenerator {
    public void run(String[] args) throws Exception {
        if (args.length != 6) {
            System.err.println(getClass().getSimpleName() + ": <id> <name> <baseUrl> <directory> <scanDirectory> <writeXmlFile>");
            System.exit(20);
        }

        File scanDirectory = new File(args[4]);
        if (!scanDirectory.exists()) {
            System.err.println(getClass().getSimpleName() + ": " + scanDirectory + " does not exist");
            System.exit(10);
        }

        long start = currentTimeMillis();
        DatasourceType datasourceType = new ObjectFactory().createDatasourceType();
        datasourceType.setId(args[0]);
        datasourceType.setName(args[1]);
        datasourceType.setBaseUrl(args[2]);
        datasourceType.setDirectory(args[3]);

        Set<File> files = collectFiles(scanDirectory);

        List<FileType> fileTypes = new ArrayList<>();
        List<ThemeType> themeTypes = new ArrayList<>();
        List<MapType> mapTypes = new ArrayList<>();

        parseFiles(files, fileTypes, mapTypes, themeTypes, scanDirectory);

        datasourceType.getFile().addAll(sortFileTypes(fileTypes));
        datasourceType.getMap().addAll(sortMapTypes(mapTypes));
        datasourceType.getTheme().addAll(sortThemeTypes(themeTypes));

        File writeXmlFile = new File(args[5]);
        writeXml(datasourceType, writeXmlFile);

        long end = currentTimeMillis();
        System.out.println(getClass().getSimpleName() + ": Took " + ((end - start) / 1000) + " seconds to collect " +
                fileTypes.size() + " files, " + mapTypes.size() + " maps and " + themeTypes.size() + " themes");
        System.exit(0);
    }

    private void recursiveCollect(File directory, Set<File> files) {
        System.out.println(getClass().getSimpleName() + ": Collecting files in " + directory);
        //noinspection ConstantConditions
        for (File file : directory.listFiles()) {
            if (file.isDirectory())
                recursiveCollect(file, files);
            else
                files.add(file);
        }
    }

    private Set<File> collectFiles(File directory) {
        Set<File> files = new HashSet<>();
        recursiveCollect(directory, files);
        return files;
    }

    protected void parseFiles(Set<File> files, List<FileType> fileTypes, List<MapType> mapTypes, List<ThemeType> themeTypes, File baseDirectory) throws IOException {
        System.out.println(getClass().getSimpleName() + ": Parsing " + files.size() + " files in " + baseDirectory);
        for (File file : files)
            parseFile(file, fileTypes, mapTypes, themeTypes, baseDirectory);
    }

    protected abstract void parseFile(File file, List<FileType> fileTypes, List<MapType> mapTypes, List<ThemeType> themeTypes, File baseDirectory) throws IOException;

    protected String relativizeUri(File file, File baseDirectory) {
        return file.getAbsolutePath().substring(baseDirectory.getAbsolutePath().length() + 1).replace(separatorChar, '/');
    }
}
