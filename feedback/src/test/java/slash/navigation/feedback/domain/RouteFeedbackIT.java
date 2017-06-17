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
package slash.navigation.feedback.domain;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.rest.SimpleCredentials;
import slash.navigation.rest.exception.ForbiddenException;

import java.io.IOException;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertTrue;
import static slash.navigation.feedback.domain.RouteFeedback.USER_URI;

public class RouteFeedbackIT {
    private static final String API = System.getProperty("api", "http://localhost:8000/");
    private static final String SUPER_USERNAME = "super";
    private static final String PASSWORD = "test";
    private static final String UMLAUTS = "\u00E4\u00F6\u00FC\u00DF\u00C4\u00D6\u00DC";
    private static final String SPECIAL_CHARACTERS = "@!§$%&()=";

    private RouteFeedback routeFeedback;
    private String url;

    @Before
    public void setUp() {
        routeFeedback = new RouteFeedback(null, API, null);
    }

    @After
    public void tearDown() throws IOException {
        if(url != null)
            deleteAsSuperuser(url);
    }

    private void deleteAsSuperuser(String url) throws IOException {
        RouteFeedback superUser = new RouteFeedback(null, API, new SimpleCredentials(SUPER_USERNAME, PASSWORD));
        superUser.deleteUser(url);
    }

    @Test
    public void testCanAddUserWithUmlauts() throws IOException {
        String userName = "Umlauts" + UMLAUTS + currentTimeMillis();
        url = routeFeedback.addUser(userName, userName, "egal", "egal", "egal@egal.egal");
    }

    @Test
    public void testCanAddUserWithSpecialCharacters() throws IOException {
        String userName = "Specials" + SPECIAL_CHARACTERS + currentTimeMillis();
        url = routeFeedback.addUser(userName, userName, "egal", "egal", "egal@egal.egal");
    }

    @Test
    public void testSuperuserCanDeleteOtherUser() throws IOException {
        String userName = "OtherUser" + currentTimeMillis();
        String url = routeFeedback.addUser(userName, userName, "Other", "User", "Test@User.Mail");
        assertTrue(url.startsWith(API + USER_URI));

        deleteAsSuperuser(url);
    }

    @Test(expected = ForbiddenException.class)
    public void testCannotAddUserWithShortUserName() throws IOException {
        url = routeFeedback.addUser("a", "egal", "egal", "egal", "egal@egal.egal");
    }

    @Test(expected = ForbiddenException.class)
    public void testCannotAddUserWithShortPassword() throws IOException {
        String userName = "ShortPassword" + currentTimeMillis();
        url = routeFeedback.addUser(userName, "b", "egal", "egal", "egal@egal.egal");
    }

    @Test(expected = ForbiddenException.class)
    public void testCannotAddUserWithWrongEmail() throws IOException {
        String userName = "WrongEmail" + currentTimeMillis();
        url = routeFeedback.addUser(userName, userName, "egal", "egal", "egal");
    }

    @Test(expected = ForbiddenException.class)
    public void testCannotAddUserWithSameName() throws IOException {
        String userName = "SameUser" + currentTimeMillis();
        url = routeFeedback.addUser(userName, userName, "First", "User", "First@User.Mail");

        routeFeedback.addUser(userName, userName, "Second", "User", "Second@User.Mail");
    }

    @Test(expected = ForbiddenException.class)
    public void testCannotDeleteOtherUser() throws IOException {
        String userName = "TestUser" + currentTimeMillis();
        url = routeFeedback.addUser(userName, userName, "Test", "User", "Test@User.Mail");
        routeFeedback.deleteUser(url);
    }
}
