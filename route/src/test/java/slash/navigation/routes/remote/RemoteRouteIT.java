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
package slash.navigation.routes.remote;

import org.junit.Test;
import slash.navigation.rest.SimpleCredentials;
import slash.navigation.rest.exception.DuplicateNameException;
import slash.navigation.rest.exception.ForbiddenException;
import slash.navigation.rest.exception.UnAuthorizedException;
import slash.navigation.routes.Category;
import slash.navigation.routes.NotFoundException;
import slash.navigation.routes.NotOwnerException;
import slash.navigation.routes.Route;

import java.io.IOException;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.*;
import static slash.navigation.routes.remote.RemoteCatalog.CATEGORY_URI;
import static slash.navigation.routes.remote.RemoteCatalog.ROUTE_URI;

public class RemoteRouteIT extends BaseRemoteCatalogTest {
    private static final String REMOTE_URL = "http://www.routeconverter.com/";

    private void createAndDeleteRoute(String description) throws IOException {
        Route route1 = test.createRoute(description, REMOTE_URL);
        assertTrue(route1.getHref().startsWith(API + ROUTE_URI));
        assertTrue(route1.getHref().endsWith("/"));

        Route route2 = catalog.getRoute(route1.getHref());
        assertNotNull(route2);
        assertEquals(route1.getHref(), route2.getHref());
        assertEquals(route2.getDescription(), route2.getName());
        assertEquals(description, route2.getDescription());
        assertEquals(USERNAME, route2.getCreator());
        assertNotNull(test.getCategories());
        assertNotNull(test.getRoutes());
        assertTrue(test.getRoutes().contains(route2));

        route2.delete();

        assertNull(catalog.getRoute(route2.getHref()));
        assertNotNull(test.getRoutes());
        // assertFalse(test.getRoutes().contains(route2));
    }

    @Test
    public void testCreateAndDeleteRoute() throws IOException {
        createAndDeleteRoute("New Route " + currentTimeMillis());
    }

    @Test
    public void testCreateRouteWithUmlauts() throws IOException {
        createAndDeleteRoute("Umlauts Route " + currentTimeMillis() + " " + UMLAUTS);
    }

    @Test
    public void testCreateRouteWithSpecialCharacters() throws IOException {
        createAndDeleteRoute("Special Route " + currentTimeMillis() + " " + SPECIAL_CHARACTERS);
    }

    @Test
    public void testCreateRouteWithPluses() throws IOException {
        createAndDeleteRoute("Plus Route " + currentTimeMillis() + " A + B + C");
    }

    @Test(expected = ForbiddenException.class)
    public void testCannotCreateRouteWithSlashes() throws IOException {
        createAndDeleteRoute("/Slashes/Route/" + currentTimeMillis() + "/");
    }

    @Test(expected = UnAuthorizedException.class)
    public void testCreateRouteForbidden() throws IOException {
        RemoteCatalog wrong = new RemoteCatalog(API, new SimpleCredentials(USERNAME, "wrong-password"));
        wrong.addRoute(API, "egal", null, REMOTE_URL);
    }

    @Test(expected = NotFoundException.class)
    public void testCannotCreateRouteWithInvalidURL() throws IOException {
        Category root = catalog.getRootCategory();
        String description = "NewRoute" + currentTimeMillis();
        catalog.addRoute(root.getHref(), description, null, "no-url");
    }

    @Test(expected = DuplicateNameException.class)
    public void testCannotCreateRouteWithSameName() throws IOException {
        Category root = catalog.getRootCategory();
        String description = "NewRoute" + currentTimeMillis();
        catalog.addRoute(root.getHref(), description, null,REMOTE_URL);
        catalog.addRoute(root.getHref(), description, null,REMOTE_URL);
    }

    @Test(expected = NotFoundException.class)
    public void testCannotCreateRouteWithNotExistingParent() throws IOException {
        catalog.addRoute(API + CATEGORY_URI + currentTimeMillis() + "/", "NewRoute" + currentTimeMillis(), null, REMOTE_URL);
    }

    @Test(expected = NotFoundException.class)
    public void testCannotDeleteNotExistingRoute() throws IOException {
        catalog.deleteRoute(API + ROUTE_URI + currentTimeMillis() + "/");
    }

    @Test(expected = NotOwnerException.class)
    public void testCannotDeleteRouteFromOtherUser() throws IOException {
        RemoteCatalog another = new RemoteCatalog(API, new SimpleCredentials(ANOTHER_USERNAME, PASSWORD));
        Category root = another.getRootCategory();
        String url = another.addRoute(root.getHref(), "MyRoute" + currentTimeMillis(), null, REMOTE_URL);
        catalog.deleteRoute(url);
    }

    @Test
    public void testSuperuserCanDeleteRouteFromOtherUser() throws IOException {
        Category root = catalog.getRootCategory();
        String description = "MyRoute" + currentTimeMillis();
        String url = catalog.addRoute(root.getHref(), description, null, REMOTE_URL);

        RemoteCatalog superUser = new RemoteCatalog(API, new SimpleCredentials(SUPER_USERNAME, PASSWORD));
        superUser.deleteRoute(url);

        for (Route route : catalog.getRootCategory().getRoutes()) {
            assertEquals("Route " + description + " still exists", route.getHref(), url);
        }
    }

    @Test
    public void testUpdateRoute() throws IOException {
        Category root = catalog.getRootCategory();

        String firstUrl = catalog.addCategory(root.getHref(), "First Category" + currentTimeMillis());
        String secondUrl = catalog.addCategory(root.getHref(), "Second Category" + currentTimeMillis());

        String description = "New Route" + currentTimeMillis();
        String url = catalog.addRoute(firstUrl, description, null,REMOTE_URL);
        Route route1 = catalog.getRoute(url);
        assertTrue(catalog.getCategory(firstUrl).getRoutes().contains(route1));
        assertFalse(catalog.getCategory(secondUrl).getRoutes().contains(route1));

        catalog.updateRoute(url, secondUrl, description, null, REMOTE_URL);
        Route route2 = catalog.getRoute(url);
        assertNotNull(route2);
        assertEquals(description, route2.getDescription());
        assertEquals(REMOTE_URL, route2.getUrl());
        assertEquals(secondUrl, ((RemoteRoute) route2).getCategory().getHref());
        assertFalse(catalog.getCategory(firstUrl).getRoutes().contains(route2));
        assertTrue(catalog.getCategory(secondUrl).getRoutes().contains(route2));

        String description3 = "UpdatedRoute" + currentTimeMillis();
        catalog.updateRoute(url, secondUrl, description3, null, REMOTE_URL);
        Route route3 = catalog.getRoute(url);
        assertNotNull(route3);
        assertEquals(description3, route3.getDescription());
        assertEquals(REMOTE_URL, route3.getUrl());
        assertEquals(secondUrl, ((RemoteRoute) route3).getCategory().getHref());

        String description4 = "UpdatedAndMovedRoute" + currentTimeMillis();
        catalog.updateRoute(url, firstUrl, description4, null, REMOTE_URL);
        Route route4 = catalog.getRoute(url);
        assertNotNull(route4);
        assertEquals(description4, route4.getDescription());
        assertEquals(REMOTE_URL, route4.getUrl());
        assertEquals(firstUrl, ((RemoteRoute) route4).getCategory().getHref());
        assertTrue(catalog.getCategory(firstUrl).getRoutes().contains(route4));
        assertFalse(catalog.getCategory(secondUrl).getRoutes().contains(route4));

        String url2 = REMOTE_URL + "updated";
        catalog.updateRoute(url, firstUrl, description4, null, url2);
        Route route5 = catalog.getRoute(url);
        assertNotNull(route5);
        assertEquals(description4, route5.getDescription());
        assertEquals(url2, route5.getUrl());
        assertEquals(firstUrl, ((RemoteRoute) route5).getCategory().getHref());
    }

    @Test(expected = NotFoundException.class)
    public void testCannotUpdateNotExistingRouteUrl() throws IOException {
        catalog.updateRoute(API + ROUTE_URI + currentTimeMillis() + "/", API, "egal", null, REMOTE_URL);
    }

    @Test(expected = NotFoundException.class)
    public void testCannotUpdateNotExistingRouteFile() throws IOException {
        catalog.updateRoute(API + ROUTE_URI + currentTimeMillis() + "/", API, "egal", REMOTE_URL, null);
    }

    @Test(expected = DuplicateNameException.class)
    public void testCannotUpdateRouteWithSameName() throws IOException {
        Category root = catalog.getRootCategory();
        String name = "FirstRoute" + currentTimeMillis();
        catalog.addRoute(root.getHref(), name, null, REMOTE_URL);
        String url = catalog.addRoute(root.getHref(), "SecondRoute" + currentTimeMillis(), null, REMOTE_URL);

        catalog.updateRoute(url, root.getHref(), name, null, REMOTE_URL);
    }

    @Test(expected = ForbiddenException.class)
    public void testCannotUpdateRouteWithSlashes() throws IOException {
        Category root = catalog.getRootCategory();
        String url = catalog.addRoute(root.getHref(), "A Route " + currentTimeMillis(), null, REMOTE_URL);

        catalog.updateRoute(url, root.getHref(), "/Slashes/Route/" + currentTimeMillis() + "/", null, REMOTE_URL);
    }
}
