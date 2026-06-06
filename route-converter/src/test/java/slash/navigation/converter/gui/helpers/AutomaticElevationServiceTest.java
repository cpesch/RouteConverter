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
package slash.navigation.converter.gui.helpers;

import org.junit.Test;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.MapDescriptor;
import slash.navigation.elevation.ElevationService;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for {@link AutomaticElevationService} ? accessor methods and priority ordering.
 *
 * @author Christian Pesch
 */
public class AutomaticElevationServiceTest {

    private ElevationServiceFacade newFacade() {
        return new ElevationServiceFacade();
    }

    private AutomaticElevationService newAutomatic(ElevationServiceFacade facade) {
        return new AutomaticElevationService(facade);
    }

    // ---- simple accessors ----

    @Test
    public void testGetNameReturnsAutomatic() {
        assertEquals("Automatic", newAutomatic(newFacade()).getName());
    }

    @Test
    public void testIsDownloadReturnsTrue() {
        assertTrue(newAutomatic(newFacade()).isDownload());
    }

    @Test
    public void testIsOverQueryLimitReturnsFalse() {
        assertFalse(newAutomatic(newFacade()).isOverQueryLimit());
    }

    @Test
    public void testGetPathReturnsEmptyString() {
        assertEquals("", newAutomatic(newFacade()).getPath());
    }

    @Test
    public void testSetPathDoesNotThrow() {
        // should silently accept any value
        newAutomatic(newFacade()).setPath("anything");
    }

    @Test
    public void testGetPreferredDownloadNameIsJonathanDeFerranti() {
        assertEquals("Jonathan de Ferranti DEM 3", newAutomatic(newFacade()).getPreferredDownloadName());
    }

    @Test
    public void testGetDirectoryReturnsNullWhenNoServicesRegistered() {
        // no services -> no directory
        assertNull(newAutomatic(newFacade()).getDirectory());
    }

    // ---- priority ordering via getElevationFor ----

    @Test
    public void testGetElevationForReturnsNullWhenNoServicesRegistered() throws IOException {
        assertNull(newAutomatic(newFacade()).getElevationFor(10.0, 50.0));
    }

    @Test
    public void testGetElevationForPrefersHigherPriorityService() throws IOException {
        ElevationServiceFacade facade = newFacade();
        AutomaticElevationService automatic = newAutomatic(facade);

        // Lower priority number = preferred first
        StubElevationService lidarService  = new StubElevationService("Sonny LiDAR DTM 0.5", 100.0);
        StubElevationService srtmService   = new StubElevationService("NASA SRTM 3", 999.0);
        StubElevationService googleService = new StubElevationService("Google Maps", 50.0);

        facade.addElevationService(automatic);
        facade.addElevationService(srtmService);
        facade.addElevationService(googleService);
        facade.addElevationService(lidarService);

        // LiDAR should win (priority 1)
        Double result = automatic.getElevationFor(10.0, 50.0);
        assertEquals(100.0, result, 0.001);
    }

    @Test
    public void testGetElevationForSkipsNullResults() throws IOException {
        ElevationServiceFacade facade = newFacade();
        AutomaticElevationService automatic = newAutomatic(facade);

        // First service returns null, second returns a value
        StubElevationService nullService  = new StubElevationService("Unknown XYZ", null);
        StubElevationService validService = new StubElevationService("GeoNames", 42.0);

        facade.addElevationService(automatic);
        facade.addElevationService(nullService);
        facade.addElevationService(validService);

        Double result = automatic.getElevationFor(0.0, 0.0);
        assertEquals(42.0, result, 0.001);
    }

    @Test
    public void testGetElevationForSkipsOverQueryLimitServices() throws IOException {
        ElevationServiceFacade facade = newFacade();
        AutomaticElevationService automatic = newAutomatic(facade);

        StubElevationService limited = new StubElevationService("Google Maps", 99.0) {
            @Override public boolean isOverQueryLimit() { return true; }
        };
        StubElevationService ok = new StubElevationService("GeoNames", 55.0);

        facade.addElevationService(automatic);
        facade.addElevationService(limited);
        facade.addElevationService(ok);

        Double result = automatic.getElevationFor(0.0, 0.0);
        assertEquals(55.0, result, 0.001);
    }

    // ---- simple stub implementation ----

    private static class StubElevationService implements ElevationService {
        private final String name;
        private final Double elevation;

        StubElevationService(String name, Double elevation) {
            this.name = name;
            this.elevation = elevation;
        }

        public String getName()              { return name; }
        public boolean isDownload()          { return false; }
        public boolean isOverQueryLimit()    { return false; }
        public String getPath()              { return ""; }
        public void setPath(String path)     {}
        public File getDirectory()           { return null; }

        public Double getElevationFor(double lon, double lat) { return elevation; }

        public void downloadElevationDataFor(List<LongitudeAndLatitude> ll, boolean wait) {}
        public long calculateRemainingDownloadSize(List<MapDescriptor> m) { return 0; }
        public void downloadElevationData(List<MapDescriptor> m) {}
    }
}

