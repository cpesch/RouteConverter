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

package slash.navigation.datasources;

import slash.common.helpers.JAXBHelper;
import slash.common.type.CompactCalendar;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.binding.*;
import slash.navigation.download.Checksum;
import slash.navigation.download.FileAndChecksum;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static slash.common.helpers.JAXBHelper.newContext;
import static slash.common.io.Transfer.formatTime;
import static slash.common.io.Transfer.parseTime;

public class DataSourcesUtil {
    private static Unmarshaller newUnmarshaller() {
        return JAXBHelper.newUnmarshaller(newContext(ObjectFactory.class));
    }

    private static Marshaller newMarshaller() {
        return JAXBHelper.newMarshaller(newContext(ObjectFactory.class));
    }

    public static DatasourcesType unmarshal(InputStream in) throws JAXBException {
        DatasourcesType result;
        try {
            JAXBElement<DatasourcesType> element = (JAXBElement<DatasourcesType>) newUnmarshaller().unmarshal(in);
            result = element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e, e);
        }
        return result;
    }

    public static void marshal(DatasourcesType datasourcesType, OutputStream out) throws JAXBException {
        try {
            try {
                newMarshaller().marshal(new ObjectFactory().createDatasources(datasourcesType), out);
            } finally {
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e, e);
        }
    }

    public static void marshal(DatasourcesType datasourcesType, Writer writer) throws JAXBException {
        newMarshaller().marshal(new ObjectFactory().createDatasources(datasourcesType), writer);
    }


    public static Checksum asChecksum(ChecksumType checksumType) {
        return new Checksum(parseTime(checksumType.getLastModified()), checksumType.getContentLength(), checksumType.getSha1());
    }

    public static BoundingBox asBoundingBox(BoundingBoxType boundingBox) {
        if (boundingBox == null || boundingBox.getNorthEast() == null || boundingBox.getSouthWest() == null)
            return null;
        return new BoundingBox(asPosition(boundingBox.getNorthEast()), asPosition(boundingBox.getSouthWest()));
    }

    private static NavigationPosition asPosition(PositionType positionType) {
        return new SimpleNavigationPosition(positionType.getLongitude(), positionType.getLatitude());
    }

    private static boolean contains(String[] array, String name) {
        for (String anArray : array) {
            if (name.equals(anArray))
                return true;

        }
        return false;
    }

    public static DatasourceType asDatasourceType(DataSource dataSource, java.util.Map<FileAndChecksum, List<FileAndChecksum>> fileAndChecksums, String... filterUrls) {
        ObjectFactory objectFactory = new ObjectFactory();

        DatasourceType datasourceType = objectFactory.createDatasourceType();
        datasourceType.setId(dataSource.getId());
        datasourceType.setName(dataSource.getName());
        datasourceType.setBaseUrl(dataSource.getBaseUrl());
        datasourceType.setDirectory(dataSource.getDirectory());

        for (File aFile : dataSource.getFiles()) {
            if (!contains(filterUrls, dataSource.getBaseUrl() + aFile.getUri()))
                continue;

            FileType fileType = objectFactory.createFileType();
            fileType.setBoundingBox(asBoundingBoxType(aFile.getBoundingBox()));
            fileType.setUri(aFile.getUri());
            replaceChecksumTypes(fileType.getChecksum(), filterChecksums(aFile.getChecksums(), fileAndChecksums.keySet()));
            replaceFragmentTypes(fileType.getFragment(), aFile.getFragments(), fileAndChecksums);
            datasourceType.getFile().add(fileType);
        }

        for (Map map : dataSource.getMaps()) {
            if (!contains(filterUrls, dataSource.getBaseUrl() + map.getUri()))
                continue;

            MapType mapType = objectFactory.createMapType();
            mapType.setBoundingBox(asBoundingBoxType(map.getBoundingBox()));
            mapType.setUri(map.getUri());
            replaceChecksumTypes(mapType.getChecksum(), filterChecksums(map.getChecksums(), fileAndChecksums.keySet()));
            replaceFragmentTypes(mapType.getFragment(), map.getFragments(), fileAndChecksums);
            datasourceType.getMap().add(mapType);
        }

        for (Theme theme : dataSource.getThemes()) {
            if (!contains(filterUrls, dataSource.getBaseUrl() + theme.getUri()))
                continue;

            ThemeType themeType = objectFactory.createThemeType();
            themeType.setImageUrl(theme.getImageUrl());
            themeType.setUri(theme.getUri());
            replaceChecksumTypes(themeType.getChecksum(), filterChecksums(theme.getChecksums(), fileAndChecksums.keySet()));
            replaceFragmentTypes(themeType.getFragment(), theme.getFragments(), fileAndChecksums);
            datasourceType.getTheme().add(themeType);
        }

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

    private static List<Checksum> filterChecksums(List<Checksum> expectedChecksums, Set<FileAndChecksum> fileAndChecksums) {
        List<Checksum> result = new ArrayList<>();
        for (Checksum expectedChecksum : expectedChecksums) {
            for (FileAndChecksum fileAndChecksum : fileAndChecksums) {
                if (fileAndChecksum.getExpectedChecksum().equals(expectedChecksum))
                    result.add(fileAndChecksum.getActualChecksum());
            }
        }
        return result;
    }

    private static void replaceChecksumTypes(List<ChecksumType> previousChecksumTypes, List<Checksum> nextChecksums) {
        previousChecksumTypes.clear();
        if (nextChecksums != null)
            previousChecksumTypes.addAll(asChecksumTypes(nextChecksums));
    }

    private static List<ChecksumType> asChecksumTypes(List<Checksum> checksums) {
        if (checksums == null)
            return null;

        List<ChecksumType> checksumTypes = new ArrayList<>();
        for (Checksum checksum : checksums)
            checksumTypes.add(asChecksumType(checksum));
        return checksumTypes;
    }

    private static ChecksumType asChecksumType(Checksum checksum) {
        return createChecksumType(checksum.getContentLength(), checksum.getLastModified(), checksum.getSHA1());
    }

    public static ChecksumType createChecksumType(Long contentLength, CompactCalendar lastModified, String sha1) {
        ChecksumType checksumType = new ObjectFactory().createChecksumType();
        checksumType.setContentLength(contentLength);
        checksumType.setLastModified(formatTime(lastModified, true));
        checksumType.setSha1(sha1);
        return checksumType;
    }

    public static void replaceFragmentTypes(List<FragmentType> previousFragmentTypes, List<Fragment> nextFragments, java.util.Map<FileAndChecksum, List<FileAndChecksum>> fragments) {
        previousFragmentTypes.clear();
        if (nextFragments != null)
            previousFragmentTypes.addAll(asFragmentTypes(nextFragments, fragments));
    }

    private static Set<FileAndChecksum> findFile(Fragment fragment, java.util.Map<FileAndChecksum, List<FileAndChecksum>> fileAndChecksumsMap) {
        Set<FileAndChecksum> result = new HashSet<>();
        for (FileAndChecksum fileAndChecksum : fileAndChecksumsMap.keySet()) {
            String path = fileAndChecksum.getFile().getAbsolutePath();
            String uri = fragment.getDownloadable().getUri();
            if (path.endsWith(uri)) {
                List<FileAndChecksum> fragmentAndChecksums = fileAndChecksumsMap.get(fileAndChecksum);
                if (fragmentAndChecksums != null)
                    for (FileAndChecksum fragmentChecksum : fragmentAndChecksums) {
                        String pathf = fragmentChecksum.getFile().getAbsolutePath();
                        String key = fragment.getKey();
                        if (pathf.endsWith(key))
                            result.add(fragmentChecksum);
                    }
            }
        }
        return result;
    }

    private static List<FragmentType> asFragmentTypes(List<Fragment> fragments, java.util.Map<FileAndChecksum, List<FileAndChecksum>> fileAndChecksums) {
        if (fragments == null)
            return null;

        List<FragmentType> fragmentTypes = new ArrayList<>();
        for (Fragment fragment : fragments)
            fragmentTypes.add(asFragmentType(fragment, findFile(fragment, fileAndChecksums)));
        return fragmentTypes;
    }

    private static FragmentType asFragmentType(Fragment fragment, Set<FileAndChecksum> fileAndChecksums) {
        FragmentType fragmentType = new ObjectFactory().createFragmentType();
        fragmentType.setKey(fragment.getKey());
        replaceChecksumTypes(fragmentType.getChecksum(), filterChecksums(fragment.getChecksums(), fileAndChecksums));
        return fragmentType;
    }

    public static String toXml(DatasourcesType datasourcesType) throws IOException {
        StringWriter writer = new StringWriter();
        try {
            marshal(datasourcesType, writer);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + datasourcesType + ": " + e, e);
        }
        return writer.toString();
    }
}
