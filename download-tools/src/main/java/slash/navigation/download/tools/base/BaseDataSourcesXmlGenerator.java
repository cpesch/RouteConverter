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

import slash.navigation.common.BoundingBox;
import slash.navigation.datasources.binding.*;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static slash.common.io.Files.generateChecksum;
import static slash.common.io.Transfer.formatTime;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.datasources.DataSourcesUtil.asBoundingBoxType;
import static slash.navigation.datasources.DataSourcesUtil.marshal;

/**
 * Base for generation of data sources XML.
 *
 * @author Christian Pesch
 */

public abstract class BaseDataSourcesXmlGenerator {

    protected List<FileType> sortFileTypes(List<FileType> fileTypes) {
        FileType[] fileTypesArray = fileTypes.toArray(new FileType[fileTypes.size()]);
        sort(fileTypesArray, new Comparator<FileType>() {
            public int compare(FileType ft1, FileType ft2) {
                return ft1.getUri().compareTo(ft2.getUri());
            }
        });
        return asList(fileTypesArray);
    }

    protected List<MapType> sortMapTypes(List<MapType> mapTypes) {
        MapType[] mapTypesArray = mapTypes.toArray(new MapType[mapTypes.size()]);
        sort(mapTypesArray, new Comparator<MapType>() {
            public int compare(MapType mt1, MapType mt2) {
                return mt1.getUri().compareTo(mt2.getUri());
            }
        });
        return asList(mapTypesArray);
    }

    protected List<ThemeType> sortThemeTypes(List<ThemeType> themeTypes) {
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

    private ChecksumType createChecksumType(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        try {
            return createChecksumType(file.lastModified(), file.length(), inputStream);
        } finally {
            closeQuietly(inputStream);
        }
    }

    protected FileType createFileType(String uri, File file, BoundingBox boundingBox) throws IOException {
        FileType fileType = new ObjectFactory().createFileType();
        fileType.setUri(uri);
        fileType.setBoundingBox(asBoundingBoxType(boundingBox));
        fileType.getChecksum().add(createChecksumType(file));
        return fileType;
    }

    protected FileType createFileType(String uri, Long lastModified, Long contentLength, BoundingBox boundingBox) throws IOException {
        FileType fileType = new ObjectFactory().createFileType();
        fileType.setUri(uri);
        fileType.setBoundingBox(asBoundingBoxType(boundingBox));
        fileType.getChecksum().add(createChecksumType(lastModified, contentLength));
        return fileType;
    }

    protected MapType createMapType(String uri, Long lastModified, Long contentLength, BoundingBox boundingBox) throws IOException {
        MapType mapType = new ObjectFactory().createMapType();
        mapType.setUri(uri);
        mapType.setBoundingBox(asBoundingBoxType(boundingBox));
        mapType.getChecksum().add(createChecksumType(lastModified, contentLength));
        return mapType;
    }

    protected ThemeType createThemeType(String uri, Long lastModified, Long contentLength, InputStream inputStream, String imageUrl) throws IOException {
        ThemeType themeType = new ObjectFactory().createThemeType();
        themeType.setUri(uri);
        themeType.setImageUrl(imageUrl);
        themeType.getChecksum().add(createChecksumType(lastModified, contentLength, inputStream));
        return themeType;
    }

    protected FragmentType createFragmentType(String key, ZipEntry entry, InputStream inputStream) throws IOException {
        FragmentType fragmentType = new ObjectFactory().createFragmentType();
        fragmentType.setKey(key);
        fragmentType.getChecksum().add(createChecksumType(entry.getTime(), entry.getSize(), inputStream));
        return fragmentType;
    }

    protected FragmentType createFragmentType(String key, Long lastModified, Long contentLength) throws IOException {
        FragmentType fragmentType = new ObjectFactory().createFragmentType();
        fragmentType.setKey(key);
        fragmentType.getChecksum().add(createChecksumType(lastModified, contentLength));
        return fragmentType;
    }

    protected ChecksumType createChecksumType(Long lastModified, Long contentLength, InputStream inputStream) throws IOException {
        ChecksumType result = new ChecksumType();
        result.setLastModified(formatTime(fromMillis(lastModified), true));
        result.setContentLength(contentLength);
        if (inputStream != null)
            result.setSha1(generateChecksum(inputStream));
        return result;
    }

    protected ChecksumType createChecksumType(Long lastModified, Long contentLength) throws IOException {
        return createChecksumType(lastModified, contentLength, null);
    }

    protected void writeXml(DatasourceType datasourceType, File file) throws JAXBException, FileNotFoundException {
        DatasourcesType datasourcesType = new ObjectFactory().createDatasourcesType();
        datasourcesType.getDatasource().add(datasourceType);
        FileOutputStream out = new FileOutputStream(file);
        marshal(datasourcesType, out);
        closeQuietly(out);
    }
}
