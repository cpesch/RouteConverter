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

package slash.navigation.datasources.helpers;

import slash.common.helpers.JAXBHelper;
import slash.common.type.CompactCalendar;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Fragment;
import slash.navigation.datasources.binding.ActionType;
import slash.navigation.datasources.binding.BoundingBoxType;
import slash.navigation.datasources.binding.CatalogType;
import slash.navigation.datasources.binding.ChecksumType;
import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.datasources.binding.FileType;
import slash.navigation.datasources.binding.FragmentType;
import slash.navigation.datasources.binding.MapType;
import slash.navigation.datasources.binding.ObjectFactory;
import slash.navigation.datasources.binding.PositionType;
import slash.navigation.datasources.binding.ThemeType;
import slash.navigation.download.Checksum;
import slash.navigation.download.FileAndChecksum;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;

import static java.io.File.separator;
import static slash.common.helpers.JAXBHelper.newContext;
import static slash.common.io.Files.generateChecksum;
import static slash.common.io.Transfer.formatXMLTime;
import static slash.common.io.Transfer.parseXMLTime;
import static slash.common.type.CompactCalendar.fromMillis;

public class DataSourcesUtil {
    private static Unmarshaller newUnmarshaller() {
        return JAXBHelper.newUnmarshaller(newContext(ObjectFactory.class));
    }

    private static Marshaller newMarshaller() {
        return JAXBHelper.newMarshaller(newContext(ObjectFactory.class));
    }

    public static CatalogType unmarshal(InputStream in) throws JAXBException {
        CatalogType result;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller().unmarshal(in);
            result = (CatalogType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e, e);
        }
        return result;
    }

    public static void marshal(CatalogType catalogType, Writer writer) throws JAXBException {
        newMarshaller().marshal(new ObjectFactory().createCatalog(catalogType), writer);
    }


    public static String toXml(CatalogType catalogType) throws IOException {
        StringWriter writer = new StringWriter();
        try {
            marshal(catalogType, writer);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + catalogType + ": " + e, e);
        }
        return writer.toString();
    }

    public static String toXml(DatasourceType datasourceType) throws IOException {
        CatalogType catalogType = new ObjectFactory().createCatalogType();
        catalogType.getDatasource().add(datasourceType);
        return toXml(catalogType);
    }

    public static Checksum asChecksum(ChecksumType checksumType) {
        return new Checksum(parseXMLTime(checksumType.getLastModified()), checksumType.getContentLength(), checksumType.getSha1());
    }

    public static List<Checksum> asChecksums(Set<FileAndChecksum> fileAndChecksums) {
        List<Checksum> result = new ArrayList<>();
        for (FileAndChecksum fileAndChecksum : fileAndChecksums)
            result.add(fileAndChecksum.getActualChecksum());
        return result;
    }

    public static BoundingBox asBoundingBox(BoundingBoxType boundingBox) {
        if (boundingBox == null || boundingBox.getNorthEast() == null || boundingBox.getSouthWest() == null)
            return null;
        return new BoundingBox(asPosition(boundingBox.getNorthEast()), asPosition(boundingBox.getSouthWest()));
    }

    private static NavigationPosition asPosition(PositionType positionType) {
        return new SimpleNavigationPosition(positionType.getLongitude(), positionType.getLatitude());
    }

    public static DatasourceType asDatasourceType(DataSource dataSource) {
        DatasourceType datasourceType = new ObjectFactory().createDatasourceType();
        datasourceType.setId(dataSource.getId());
        datasourceType.setName(dataSource.getName());
        datasourceType.setBaseUrl(dataSource.getBaseUrl());
        datasourceType.setDirectory(dataSource.getDirectory());
        datasourceType.setAction(ActionType.fromValue(dataSource.getAction()));
        return datasourceType;
    }


    public static BoundingBoxType asBoundingBoxType(BoundingBox boundingBox) {
        if (boundingBox == null)
            return null;

        BoundingBoxType boundingBoxType = new ObjectFactory().createBoundingBoxType();
        boundingBoxType.setNorthEast(asPositionType(boundingBox.getNorthEast()));
        boundingBoxType.setSouthWest(asPositionType(boundingBox.getSouthWest()));
        return boundingBoxType;
    }

    private static PositionType asPositionType(NavigationPosition position) {
        PositionType positionType = new ObjectFactory().createPositionType();
        positionType.setLongitude(position.getLongitude());
        positionType.setLatitude(position.getLatitude());
        return positionType;
    }

    private static ChecksumType asChecksumType(Checksum checksum) {
        return createChecksumType(checksum.getLastModified(), checksum.getContentLength(), checksum.getSHA1());
    }

    private static List<ChecksumType> asChecksumTypes(List<Checksum> checksums) {
        if (checksums == null)
            return null;

        List<ChecksumType> checksumTypes = new ArrayList<>();
        for (Checksum checksum : checksums)
            if (checksum != null)
                checksumTypes.add(asChecksumType(checksum));
        return checksumTypes;
    }

    public static FileType createFileType(String uri, List<Checksum> checksums, BoundingBox boundingBox) {
        FileType fileType = new ObjectFactory().createFileType();
        fileType.setUri(uri);
        fileType.setBoundingBox(asBoundingBoxType(boundingBox));
        List<ChecksumType> checksumTypes = asChecksumTypes(checksums);
        if (checksumTypes != null)
            fileType.getChecksum().addAll(checksumTypes);
        return fileType;
    }

    public static MapType createMapType(String uri, List<Checksum> checksums, BoundingBox boundingBox) {
        MapType mapType = new ObjectFactory().createMapType();
        mapType.setUri(uri);
        mapType.setBoundingBox(asBoundingBoxType(boundingBox));
        List<ChecksumType> checksumTypes = asChecksumTypes(checksums);
        if (checksumTypes != null)
            mapType.getChecksum().addAll(checksumTypes);
        return mapType;
    }

    public static ThemeType createThemeType(String uri, List<Checksum> checksums, String imageUrl) {
        ThemeType themeType = new ObjectFactory().createThemeType();
        themeType.setUri(uri);
        themeType.setImageUrl(imageUrl);
        List<ChecksumType> checksumTypes = asChecksumTypes(checksums);
        if (checksumTypes != null)
            themeType.getChecksum().addAll(checksumTypes);
        return themeType;
    }

    public static FragmentType createFragmentType(String key, Long lastModified, Long contentLength) throws IOException {
        FragmentType fragmentType = new ObjectFactory().createFragmentType();
        fragmentType.setKey(key);
        fragmentType.getChecksum().add(createChecksumType(lastModified, contentLength, null));
        return fragmentType;
    }

    public static FragmentType createFragmentType(String key, ZipEntry entry, InputStream inputStream) throws IOException {
        FragmentType fragmentType = new ObjectFactory().createFragmentType();
        fragmentType.setKey(key);
        fragmentType.getChecksum().add(createChecksumType(entry.getTime(), entry.getSize(), inputStream));
        return fragmentType;
    }

    public static FragmentType createFragmentType(Fragment fragment, Set<FileAndChecksum> fileAndChecksums) {
        FragmentType fragmentType = new ObjectFactory().createFragmentType();
        fragmentType.setKey(fragment.getKey());
        List<ChecksumType> checksumTypes = asChecksumTypes(asChecksums(fileAndChecksums));
        if (checksumTypes != null)
            fragmentType.getChecksum().addAll(checksumTypes);
        return fragmentType;
    }

    public static ChecksumType createChecksumType(CompactCalendar lastModified, Long contentLength, String sha1) {
        ChecksumType checksumType = new ObjectFactory().createChecksumType();
        checksumType.setContentLength(contentLength);
        checksumType.setLastModified(formatXMLTime(lastModified, true));
        checksumType.setSha1(sha1);
        return checksumType;
    }

    private static ChecksumType createChecksumType(Long lastModified, Long contentLength, InputStream inputStream) throws IOException {
        ChecksumType result = new ChecksumType();
        result.setLastModified(lastModified != null ? formatXMLTime(fromMillis(lastModified), true) : null);
        result.setContentLength(contentLength);
        if (inputStream != null)
            result.setSha1(generateChecksum(inputStream));
        return result;
    }

    public static String asMetaDataComparablePath(File file) throws IOException {
        return file.getCanonicalPath().replace(separator, "/");
    }
}
