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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertTrue;
import static slash.common.io.InputOutput.copyAndClose;

public class RouteFeedbackIT extends RouteFeedbackServiceBase {
    private static final String SUPER_USERNAME = "super";
    private static final String SPECIAL_CHARACTERS = "@!\u00A7$%&()=";
    private static final String UMLAUTS = "\u00E4\u00F6\u00FC\u00DF\u00C4\u00D6\u00DC";

    private RouteFeedback routeFeedback;
    private String url;

    @Before
    public void setUp() {
        routeFeedback = new RouteFeedback(API, new SimpleCredentials("routeconverter", "pmcs123$".toCharArray()));
    }

    @After
    public void tearDown() throws IOException {
        if(url != null)
            deleteAsSuperuser(url);
    }

    private void deleteAsSuperuser(String url) throws IOException {
        RouteFeedback superUser = new RouteFeedback(API, new SimpleCredentials(SUPER_USERNAME, PASSWORD));
        superUser.deleteUser(API + "v1/" + url);
    }

    @Test
    public void testCanAddUserWithUmlauts() throws IOException {
        String userName = "Umlauts" + UMLAUTS + currentTimeMillis();
        url = routeFeedback.addUser(userName, userName, "egal", "egal", "egal@egal.egal");
    }

    @Test(expected = ForbiddenException.class)
    public void testCannotAddUserWithSpecialCharacters() throws IOException {
        String userName = "Specials" + SPECIAL_CHARACTERS + currentTimeMillis();
        url = routeFeedback.addUser(userName, userName, "egal", "egal", "egal@egal.egal");
    }

    @Test
    public void testSuperuserCanDeleteOtherUser() throws IOException {
        String userName = "OtherUser" + currentTimeMillis();
        url = routeFeedback.addUser(userName, userName, "Other", "User", "Test@User.Mail");
        assertTrue(url.contains("users"));

        deleteAsSuperuser(url);

        url = null;
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
        routeFeedback.deleteUser(API + "v1/" + url);
    }

    @Test
    public void testCanSendErrorReport() throws IOException {
        InputStream input = getClass().getResourceAsStream("errorreporttest.txt");
        File file = File.createTempFile("errorreport", ".txt");
        file.deleteOnExit();
        copyAndClose(input, new FileOutputStream(file));

        String url = routeFeedback.sendErrorReport("log output", "description", file);
        assertTrue(url.contains("error-report"));
    }

    @Test
    public void testCanCheckForUpdateAnonymous() throws IOException {
        RouteFeedback anonymous = new RouteFeedback(API, null);
        String result = anonymous.checkForUpdate("1",
                "2", 3, "4", "5", "6",
                "7", "8", "9", 10);
        assertTrue(result.contains("version"));
        assertTrue(result.contains("feature"));
    }

    @Test
    public void testCanCheckForUpdate() throws IOException {
        String result = routeFeedback.checkForUpdate("2",
                "3", 4, "5", "6", "7",
                "8", "9", "10", 11);
        assertTrue(result.contains("feature"));
    }

    @Test
    public void testCanCheckForUpdateWrongCredentials() throws IOException {
        RouteFeedback anonymous = new RouteFeedback(API, new SimpleCredentials("UnknownUser" + System.currentTimeMillis(), WRONG_PASSWORD));
        String result = anonymous.checkForUpdate("1",
                "2", 3, "4", "5", "6",
                "7", "8", "9", 10);
        assertTrue(result.contains("version"));
        assertTrue(result.contains("feature"));
    }
}
