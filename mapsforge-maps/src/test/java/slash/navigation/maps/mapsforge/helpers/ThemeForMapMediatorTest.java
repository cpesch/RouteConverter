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
package slash.navigation.maps.mapsforge.helpers;

import org.junit.Test;
import slash.navigation.maps.item.ItemModel;
import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.LocalTheme;
import slash.navigation.maps.mapsforge.MapType;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ThemeForMapMediatorTest {
    @Test
    public void testFallsBackToCurrentThemeWhenProviderDirectoryHasNoThemes() throws Exception {
        Path themesDirectory = Files.createTempDirectory("theme-mediator-provider");
        try {
            String provider = "provider-" + UUID.randomUUID();
            Files.createDirectory(themesDirectory.resolve(provider));
            Path foreignProvider = Files.createDirectory(themesDirectory.resolve("other-provider"));
            Files.createFile(foreignProvider.resolve("other.xml"));

            LocalTheme currentTheme = createTheme("Current Theme", "theme-url");
            LocalMap map = createMap("map-" + UUID.randomUUID(), provider);
            ThemeForMapMediator mediator = new ThemeForMapMediator(createMapManager(themesDirectory, map, currentTheme));
            try {
                Method getFirstTheme = ThemeForMapMediator.class.getDeclaredMethod("getFirstTheme", LocalMap.class);
                getFirstTheme.setAccessible(true);

                String actual = (String) getFirstTheme.invoke(mediator, map);
                assertEquals(currentTheme.description(), actual);
            } finally {
                mediator.dispose();
            }
        } finally {
            deleteRecursively(themesDirectory);
        }
    }

    @Test
    public void testDoesNotReapplySameThemeWhenSwitchingMaps() throws IOException {
        Path themesDirectory = Files.createTempDirectory("theme-mediator-noop");
        try {
            LocalTheme currentTheme = createTheme("Current Theme", "theme-url");
            LocalMap map = createMap("map-" + UUID.randomUUID(), "provider-" + UUID.randomUUID());
            TestMapModel displayedMapModel = new TestMapModel(map);
            CountingThemeModel appliedThemeModel = new CountingThemeModel(currentTheme);
            ItemTableModel<LocalTheme> availableThemesModel = new ItemTableModel<>(1);
            availableThemesModel.addOrUpdateItem(currentTheme);

            MapsforgeMapManager manager = mock(MapsforgeMapManager.class);
            when(manager.getDisplayedMapModel()).thenReturn(displayedMapModel);
            when(manager.getAppliedThemeModel()).thenReturn(appliedThemeModel);
            when(manager.getAvailableThemesModel()).thenReturn(availableThemesModel);
            when(manager.getThemesDirectory()).thenReturn(themesDirectory.toFile());

            ThemeForMapMediator mediator = new ThemeForMapMediator(manager);
            try {
                displayedMapModel.fireChangedExternally();
                assertEquals("Switching maps should not reapply an identical theme", 0, appliedThemeModel.getSetInvocations());
            } finally {
                mediator.dispose();
            }
        } finally {
            deleteRecursively(themesDirectory);
        }
    }

    private static MapsforgeMapManager createMapManager(Path themesDirectory, LocalMap map, LocalTheme currentTheme) {
        TestMapModel displayedMapModel = new TestMapModel(map);
        CountingThemeModel appliedThemeModel = new CountingThemeModel(currentTheme);
        ItemTableModel<LocalTheme> availableThemesModel = new ItemTableModel<>(1);
        availableThemesModel.addOrUpdateItem(currentTheme);

        MapsforgeMapManager manager = mock(MapsforgeMapManager.class);
        when(manager.getDisplayedMapModel()).thenReturn(displayedMapModel);
        when(manager.getAppliedThemeModel()).thenReturn(appliedThemeModel);
        when(manager.getAvailableThemesModel()).thenReturn(availableThemesModel);
        when(manager.getThemesDirectory()).thenReturn(themesDirectory.toFile());
        return manager;
    }

    private static LocalMap createMap(String description, String provider) {
        LocalMap map = mock(LocalMap.class);
        when(map.description()).thenReturn(description);
        when(map.getProvider()).thenReturn(provider);
        when(map.getType()).thenReturn(MapType.Mapsforge);
        when(map.getUrl()).thenReturn("file:/" + description + ".map");
        return map;
    }

    private static LocalTheme createTheme(String description, String url) {
        LocalTheme theme = mock(LocalTheme.class);
        when(theme.description()).thenReturn(description);
        when(theme.getUrl()).thenReturn(url);
        return theme;
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root))
            return;

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null)
                    throw exc;
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static class TestMapModel extends ItemModel<LocalMap> {
        private LocalMap item;

        private TestMapModel(LocalMap item) {
            super("displayed-map-" + UUID.randomUUID(), null);
            this.item = item;
        }

        @Override
        public LocalMap getItem() {
            return item;
        }

        private void fireChangedExternally() {
            fireChanged();
        }

        @Override
        protected LocalMap stringToItem(String value) {
            return item;
        }

        @Override
        protected String itemToString(LocalMap item) {
            return item != null ? item.getUrl() : "";
        }
    }

    private static class CountingThemeModel extends ItemModel<LocalTheme> {
        private LocalTheme item;
        private int setInvocations;

        private CountingThemeModel(LocalTheme item) {
            super("applied-theme-" + UUID.randomUUID(), null);
            this.item = item;
        }

        @Override
        public LocalTheme getItem() {
            return item;
        }

        @Override
        public void setItem(LocalTheme item) {
            this.item = item;
            setInvocations++;
            fireChanged();
        }

        private int getSetInvocations() {
            return setInvocations;
        }

        @Override
        protected LocalTheme stringToItem(String value) {
            return item;
        }

        @Override
        protected String itemToString(LocalTheme item) {
            return item != null ? item.getUrl() : "";
        }
    }
}

