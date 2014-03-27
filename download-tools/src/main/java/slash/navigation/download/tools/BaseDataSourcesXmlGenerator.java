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

import slash.navigation.download.datasources.binding.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.io.File.separatorChar;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static slash.common.io.Files.generateChecksum;
import static slash.common.io.Transfer.formatTime;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.download.datasources.DataSourcesUtil.marshal;

/**
 * Base for generator of data sources XML from file system mirror.
 *
 * @author Christian Pesch
 */

public abstract class BaseDataSourcesXmlGenerator {
    private List<DatasourceType> datasourceTypes = new ArrayList<DatasourceType>();

    public void run(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println(getClass().getSimpleName() + ": <name> <baseUrl> <directory> <scanDirectory> <writeXmlFile>");
            System.exit(20);
        }

        long start = currentTimeMillis();

        DatasourceType datasourceType = new ObjectFactory().createDatasourceType();
        datasourceType.setName(args[0]);
        datasourceType.setBaseUrl(args[1]);
        datasourceType.setDirectory(args[2]);
        datasourceTypes.add(datasourceType);

        List<File> files = new ArrayList<File>();
        File scanDirectory = new File(args[3]);
        if (!scanDirectory.exists()) {
            System.err.println(getClass().getSimpleName() + ": " + scanDirectory + " does not exist");
            System.exit(10);
        }
        collectFiles(scanDirectory, files);

        List<FragmentType> fragmentTypes = new ArrayList<FragmentType>();
        List<FileType> fileTypes = new ArrayList<FileType>();

        parseFiles(files, fragmentTypes, fileTypes, scanDirectory);

        datasourceType.getFragment().addAll(sortFragmentTypes(fragmentTypes));
        datasourceType.getFile().addAll(sortFileTypes(fileTypes));

        File writeXmlFile = new File(args[4]);
        writeXml(writeXmlFile);

        long end = currentTimeMillis();
        System.out.println(getClass().getSimpleName() + ": Took " + (end - start) + " ms to collect " + fragmentTypes.size() + " fragments and " + fileTypes.size() + " files");
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

    protected void parseFiles(List<File> files, List<FragmentType> fragmentTypes, List<FileType> fileTypes, File baseDirectory) throws IOException {
        System.out.println(getClass().getSimpleName() + ": Parsing " + files.size() + " files in " + baseDirectory);
        for (File file : files)
            parseFile(file, fragmentTypes, fileTypes, baseDirectory);
    }

    protected abstract void parseFile(File file, List<FragmentType> fragmentTypes, List<FileType> fileTypes, File baseDirectory) throws IOException;

    protected String relativizeUri(File file, File baseDirectory) {
        return file.getAbsolutePath().substring(baseDirectory.getAbsolutePath().length() + 1).replace(separatorChar, '/');
    }

    private List<FragmentType> sortFragmentTypes(List<FragmentType> fragmentTypes) {
        FragmentType[] fragmentTypesArray = fragmentTypes.toArray(new FragmentType[fragmentTypes.size()]);
        sort(fragmentTypesArray, new Comparator<FragmentType>() {
            public int compare(FragmentType ft1, FragmentType ft2) {
                return ft1.getKey().compareTo(ft2.getKey());
            }
        });
        return asList(fragmentTypesArray);
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

    protected FileType createFileType(String uri, File file) throws IOException {
        String fileChecksum = generateChecksum(file);

        FileType fileType = new ObjectFactory().createFileType();
        fileType.setUri(uri);
        fileType.setSize(file.length());
        fileType.setChecksum(fileChecksum);
        fileType.setTimestamp(formatTime(fromMillis(file.lastModified()), true));

        return fileType;
    }

    private void writeXml(File file) throws JAXBException, FileNotFoundException {
        DatasourcesType datasourcesType = new ObjectFactory().createDatasourcesType();
        datasourcesType.getDatasource().addAll(datasourceTypes);
        FileOutputStream out = new FileOutputStream(file);
        marshal(datasourcesType, out);
        closeQuietly(out);
    }
}
