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
package slash.navigation.hgt.helper;

import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.datasources.binding.DatasourcesType;
import slash.navigation.datasources.binding.MappingType;
import slash.navigation.datasources.binding.ObjectFactory;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.io.File.separatorChar;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static slash.navigation.datasources.DataSourcesUtil.marshal;

/**
 * Creates hgt data source xml from file system mirror.
 *
 * @author Christian Pesch
 */

public class CreateHgtDataSourcesXml {
    private List<DatasourceType> datasourceTypes = new ArrayList<DatasourceType>();

    public void run(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            DatasourceType datasourceType = new ObjectFactory().createDatasourceType();
            datasourceType.setName("name" + (i + 1));
            datasourceType.setBaseUrl("baseUrl" + (i + 1) + "/");
            datasourceType.setDirectory(arg);

            List<File> files = new ArrayList<File>();
            File baseDirectory = new File(arg);
            collectFiles(baseDirectory, files);

            List<MappingType> mappingTypes = new ArrayList<MappingType>();
            parseFiles(files, mappingTypes, baseDirectory);

            datasourceType.getMapping().addAll(sortMappingTypes(mappingTypes));
            datasourceTypes.add(datasourceType);
        }

        printXml();
        System.exit(0);
    }

    private List<MappingType> sortMappingTypes(List<MappingType> mappingTypes) {
        MappingType[] mappingTypesArray = mappingTypes.toArray(new MappingType[mappingTypes.size()]);
        sort(mappingTypesArray, new Comparator<MappingType>() {
            public int compare(MappingType mt1, MappingType mt2) {
                return mt1.getKey().compareTo(mt2.getKey());
            }
        });
        return asList(mappingTypesArray);
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

    private void parseFiles(List<File> files, List<MappingType> mappingTypes, File baseDirectory) throws IOException {
        System.out.println("Parsing " + files.size() + " files in " + baseDirectory);
        for (File file : files)
            parseFile(file, mappingTypes, baseDirectory);
    }

    private static final Pattern KEY_PATTERN = Pattern.compile(".*([N|S]\\d{2}[E|W]\\d{3}).*", CASE_INSENSITIVE);

    private String extractKey(String string) {
        Matcher matcher = KEY_PATTERN.matcher(string);
        if (!matcher.matches()) {
            System.err.println(string + " does not match key pattern");
            return null;
        }
        return matcher.group(1).toUpperCase();
    }

    private String relativizeUri(File file, File baseDirectory) {
        return file.getAbsolutePath().substring(baseDirectory.getAbsolutePath().length() + 1).replace(separatorChar, '/');
    }

    private void parseFile(File file, List<MappingType> mappingTypes, File baseDirectory) throws IOException {
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(new FileInputStream(file));
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    String key = extractKey(entry.getName());
                    if (key != null) {
                        String uri = relativizeUri(file, baseDirectory);
                        System.out.println(key + " maps to " + uri);

                        MappingType mappingType = new ObjectFactory().createMappingType();
                        mappingType.setKey(key);
                        mappingType.setUri(uri);
                        mappingTypes.add(mappingType);
                    }
                }
                entry = zipInputStream.getNextEntry();
            }
        } finally {
            if (zipInputStream != null)
                closeQuietly(zipInputStream);
        }
    }

    private void printXml() throws JAXBException, FileNotFoundException {
        DatasourcesType datasourcesType = new ObjectFactory().createDatasourcesType();
        datasourcesType.getDatasource().addAll(datasourceTypes);
        FileOutputStream out = new FileOutputStream("hgt-datasources.xml");
        marshal(datasourcesType, out);
        closeQuietly(out);
    }

    public static void main(String[] args) throws Exception {
        new CreateHgtDataSourcesXml().run(args);
    }
}
