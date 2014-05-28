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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

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


    public static DatasourceType asDatasourceType(DataSource dataSource) {
        ObjectFactory objectFactory = new ObjectFactory();

        DatasourceType datasourceType = objectFactory.createDatasourceType();
        datasourceType.setId(dataSource.getId());
        datasourceType.setName(dataSource.getName());
        datasourceType.setBaseUrl(dataSource.getBaseUrl());
        datasourceType.setDirectory(dataSource.getDirectory());

        for (File file : dataSource.getFiles()) {
            FileType fileType = objectFactory.createFileType();
            fileType.setBoundingBox(asBoundingBoxType(file.getBoundingBox()));
            fileType.setUri(file.getUri());
            replaceChecksumTypes(fileType.getChecksum(), file.getChecksums());
            replaceFragmentTypes(fileType.getFragment(), file.getFragments());
            datasourceType.getFile().add(fileType);
        }

        for (Map map : dataSource.getMaps()) {
            MapType mapType = objectFactory.createMapType();
            mapType.setBoundingBox(asBoundingBoxType(map.getBoundingBox()));
            mapType.setUri(map.getUri());
            replaceChecksumTypes(mapType.getChecksum(), map.getChecksums());
            replaceFragmentTypes(mapType.getFragment(), map.getFragments());
            datasourceType.getMap().add(mapType);
        }

        for (Theme theme : dataSource.getThemes()) {
            ThemeType themeType = objectFactory.createThemeType();
            themeType.setImageUrl(theme.getImageUrl());
            themeType.setUri(theme.getUri());
            replaceChecksumTypes(themeType.getChecksum(), theme.getChecksums());
            replaceFragmentTypes(themeType.getFragment(), theme.getFragments());
            datasourceType.getTheme().add(themeType);
        }

        return datasourceType;
    }

    public static BoundingBoxType asBoundingBoxType(BoundingBox boundingBox) {
        if(boundingBox == null)
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

    public static void replaceChecksumTypes(List<ChecksumType> previousChecksumTypes, List<Checksum> nextChecksums) {
        previousChecksumTypes.clear();
        if(nextChecksums != null)
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

    public static void replaceFragmentTypes(List<FragmentType> previousFragmentTypes, List<Fragment> nextFragments) {
        previousFragmentTypes.clear();
        if(nextFragments != null)
            previousFragmentTypes.addAll(asFragmentTypes(nextFragments));
    }

    private static List<FragmentType> asFragmentTypes(List<Fragment> fragments) {
        if (fragments == null)
            return null;

        List<FragmentType> fragmentTypes = new ArrayList<>();
        for (Fragment fragment : fragments)
            fragmentTypes.add(asFragmentType(fragment));
        return fragmentTypes;
    }

    private static FragmentType asFragmentType(Fragment fragment) {
        FragmentType fragmentType = new ObjectFactory().createFragmentType();
        fragmentType.setKey(fragment.getKey());
        replaceChecksumTypes(fragmentType.getChecksum(), fragment.getChecksums());
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
