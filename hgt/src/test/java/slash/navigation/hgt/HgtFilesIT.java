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
package slash.navigation.hgt;

import org.junit.Test;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.binding.ActionType;
import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.datasources.binding.FileType;
import slash.navigation.datasources.binding.FragmentType;
import slash.navigation.datasources.impl.DataSourceImpl;
import slash.navigation.download.DownloadManager;

import java.io.IOException;

import static java.io.File.createTempFile;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static slash.common.TestCase.calendar;
import static slash.navigation.datasources.helpers.DataSourcesUtil.createChecksumType;

public class HgtFilesIT {

    private DataSource createDataSource(String id, String name, String baseUrl, String directory) {
        DatasourceType datasourceType = new DatasourceType();
        datasourceType.setId(id);
        datasourceType.setName(name);
        datasourceType.setBaseUrl(baseUrl);
        datasourceType.setDirectory(directory);
        datasourceType.setAction(ActionType.fromValue("Flatten"));

        FileType fileType1 = new FileType();
        fileType1.setUri("Eurasia/N59E011.hgt.zip");
        FragmentType fragmentType1 = new FragmentType();
        fragmentType1.setKey("N59E011.hgt");
        fileType1.getFragment().add(fragmentType1);
        datasourceType.getFile().add(fileType1);

        FileType fileType2 = new FileType();
        fileType2.setUri("Eurasia/N60E012.hgt.zip");
        FragmentType fragmentType2 = new FragmentType();
        fragmentType2.setKey("N60E012.hgt");
        fragmentType2.getChecksum().add(createChecksumType(calendar(2009, 1, 15, 11, 6, 16), 2884802L, "395C9F5202BC8ECF0CCAAE567772FA7955774FEA"));
        fileType2.getFragment().add(fragmentType2);
        datasourceType.getFile().add(fileType2);

        FileType fileType3 = new FileType();
        fileType3.setUri("I36.zip");
        fileType3.getChecksum().add(createChecksumType(calendar(2013, 1, 20, 17, 42, 36), 4987465L, "99982D1554A9F2B9CA49642E78BCD8192FC9DEF3"));
        FragmentType fragmentType3 = new FragmentType();
        fragmentType3.setKey("N32E034.hgt");
        fragmentType3.getChecksum().add(createChecksumType(calendar(2012, 5, 14, 20, 51, 32), 2884802L, "AD36AA9709ECAE64718308EBB1659C5BB4327A74"));
        fileType3.getFragment().add(fragmentType3);
        FragmentType fragmentType4 = new FragmentType();
        fragmentType4.setKey("N32E035.hgt");
        fragmentType4.getChecksum().add(createChecksumType(calendar(2011, 1, 20, 17, 42, 36), 287465L, "B9982D1554A9F2B9CA49642E78BCD8192FC9DEF3"));
        fileType3.getFragment().add(fragmentType4);
        datasourceType.getFile().add(fileType3);

        DataSourceImpl dataSource = new DataSourceImpl(datasourceType);
        // make sure to initialize
        dataSource.getDownloadable(fileType1.getUri());
        return dataSource;
    }

    @Test
    public void testElevationFor() throws IOException {
        HgtFiles files = new HgtFiles(createDataSource("test id", "test plain", "http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/", "test"),
                new DownloadManager(createTempFile("queueFile", ".xml")));
        files.downloadElevationDataFor(asList(new LongitudeAndLatitude(11.2, 59.0), new LongitudeAndLatitude(12.0, 60.2)), true);

        Double elevation1 = files.getElevationFor(11.2, 59.0);
        assertNotNull(elevation1);
        assertEquals(40, elevation1.intValue());
        Double elevation2 = files.getElevationFor(12.0, 60.2);
        assertNotNull(elevation2);
        assertEquals(211, elevation2.intValue());
        assertNull(files.getElevationFor(11.2, 61.0));
    }

    @Test
    public void testDownloadElevationDataInZipFile() throws IOException {
        HgtFiles files = new HgtFiles(createDataSource("test id", "test zip", "http://www.viewfinderpanoramas.org/dem3/", "test"),
                new DownloadManager(createTempFile("queueFile", ".xml")));
        files.downloadElevationDataFor(singletonList(new LongitudeAndLatitude(35.71, 32.51)), true);

        Double elevation1 = files.getElevationFor(35.71, 32.51);
        assertNotNull(elevation1);
        assertEquals(274, elevation1.intValue());
    }
}
