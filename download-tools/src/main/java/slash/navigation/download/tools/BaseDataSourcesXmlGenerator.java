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

import slash.navigation.common.BoundingBox;
import slash.navigation.datasources.binding.*;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.io.File.separatorChar;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static slash.common.io.Files.generateChecksum;
import static slash.common.io.Transfer.formatTime;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.datasources.DataSourcesUtil.asBoundingBoxType;
import static slash.navigation.datasources.DataSourcesUtil.marshal;

/**
 * Base for generator of data sources XML from file system mirror.
 *
 * @author Christian Pesch
 */

public abstract class BaseDataSourcesXmlGenerator {

    public void run(String[] args) throws Exception {
        if (args.length != 6) {
            System.err.println(getClass().getSimpleName() + ": <id> <name> <baseUrl> <directory> <scanDirectory> <writeXmlFile>");
            System.exit(20);
        }

        long start = currentTimeMillis();

        DatasourceType datasourceType = new ObjectFactory().createDatasourceType();
        datasourceType.setId(args[0]);
        datasourceType.setName(args[1]);
        datasourceType.setBaseUrl(args[2]);
        datasourceType.setDirectory(args[3]);

        List<File> files = new ArrayList<>();
        File scanDirectory = new File(args[4]);
        if (!scanDirectory.exists()) {
            System.err.println(getClass().getSimpleName() + ": " + scanDirectory + " does not exist");
            System.exit(10);
        }
        collectFiles(scanDirectory, files);

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

    private void collectFiles(File directory, List<File> files) {
        //noinspection ConstantConditions
        for (File file : directory.listFiles()) {
            if (file.isDirectory())
                collectFiles(file, files);
            else
                files.add(file);
        }
    }

    protected void parseFiles(List<File> files, List<FileType> fileTypes, List<MapType> mapTypes, List<ThemeType> themeTypes, File baseDirectory) throws IOException {
        System.out.println(getClass().getSimpleName() + ": Parsing " + files.size() + " files in " + baseDirectory);
        for (File file : files)
            parseFile(file, fileTypes, mapTypes, themeTypes, baseDirectory);
    }

    protected abstract void parseFile(File file, List<FileType> fileTypes, List<MapType> mapTypes, List<ThemeType> themeTypes, File baseDirectory) throws IOException;

    protected String relativizeUri(File file, File baseDirectory) {
        return file.getAbsolutePath().substring(baseDirectory.getAbsolutePath().length() + 1).replace(separatorChar, '/');
    }

    private List<FileType> sortFileTypes(List<FileType> fileTypes) {
        FileType[] fileTypesArray = fileTypes.toArray(new FileType[fileTypes.size()]);
        sort(fileTypesArray, new Comparator<FileType>() {
            public int compare(FileType ft1, FileType ft2) {
                return ft1.getUri().compareTo(ft2.getUri());
            }
        });
        return asList(fileTypesArray);
    }

    private List<MapType> sortMapTypes(List<MapType> mapTypes) {
        MapType[] mapTypesArray = mapTypes.toArray(new MapType[mapTypes.size()]);
        sort(mapTypesArray, new Comparator<MapType>() {
            public int compare(MapType mt1, MapType mt2) {
                return mt1.getUri().compareTo(mt2.getUri());
            }
        });
        return asList(mapTypesArray);
    }

    private List<ThemeType> sortThemeTypes(List<ThemeType> themeTypes) {
        ThemeType[] themeTypesArray = themeTypes.toArray(new ThemeType[themeTypes.size()]);
        sort(themeTypesArray, new Comparator<ThemeType>() {
            public int compare(ThemeType tt1, ThemeType tt2) {
                return tt1.getUri().compareTo(tt2.getUri());
            }
        });
        return asList(themeTypesArray);
    }

    protected List<FragmentType> sortFragmentTypes(List<FragmentType> fragmentTypes) {
        FragmentType[] fragmentTypesArray = fragmentTypes.toArray(new FragmentType[fragmentTypes.size()]);
        sort(fragmentTypesArray, new Comparator<FragmentType>() {
            public int compare(FragmentType ft1, FragmentType ft2) {
                return ft1.getKey().compareTo(ft2.getKey());
            }
        });
        return asList(fragmentTypesArray);
    }

    protected FileType createFileType(String uri, File file, BoundingBox boundingBox) throws IOException {
        FileType fileType = new ObjectFactory().createFileType();
        fileType.setUri(uri);
        fileType.setBoundingBox(asBoundingBoxType(boundingBox));
        fileType.getChecksum().add(createChecksumType(file));
        return fileType;
    }

    protected MapType createMapType(String uri, File file, BoundingBox boundingBox) throws IOException {
        MapType mapType = new ObjectFactory().createMapType();
        mapType.setUri(uri);
        mapType.setBoundingBox(asBoundingBoxType(boundingBox));
        mapType.getChecksum().add(createChecksumType(file));
        return mapType;
    }

    protected ThemeType createThemeType(String uri, File file, String imageUrl) throws IOException {
        ThemeType themeType = new ObjectFactory().createThemeType();
        themeType.setUri(uri);
        themeType.setImageUrl(imageUrl);
        themeType.getChecksum().add(createChecksumType(file));
        return themeType;
    }

    protected FragmentType createFragmentType(String key, String uri, ZipEntry entry, ZipInputStream inputStream) throws IOException {
        FragmentType fragmentType = new ObjectFactory().createFragmentType();
        fragmentType.setKey(key);
        fragmentType.getChecksum().add(createChecksumType(entry.getTime(), entry.getSize(), inputStream));
        return fragmentType;
    }

    private ChecksumType createChecksumType(Long lastModified, Long contentLength, InputStream inputStream) throws IOException {
        ChecksumType result = new ChecksumType();
        result.setLastModified(formatTime(fromMillis(lastModified), true));
        result.setContentLength(contentLength);
        result.setSha1(generateChecksum(inputStream));
        return result;
    }

    private ChecksumType createChecksumType(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        try {
            return createChecksumType(file.lastModified(), file.length(), inputStream);
        } finally {
            closeQuietly(inputStream);
        }
    }

    private void writeXml(DatasourceType datasourceType, File file) throws JAXBException, FileNotFoundException {
        DatasourcesType datasourcesType = new ObjectFactory().createDatasourcesType();
        datasourcesType.getDatasource().add(datasourceType);
        FileOutputStream out = new FileOutputStream(file);
        marshal(datasourcesType, out);
        closeQuietly(out);
    }
}
