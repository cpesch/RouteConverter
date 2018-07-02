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

import org.junit.Before;
import slash.navigation.rest.SimpleCredentials;

public abstract class RouteFeedbackServiceBase {
    protected static final String FEEDBACK = System.getProperty("feedback", "http://localhost:8000/feedback/");
    protected static final String API = System.getProperty("api", "http://localhost:8000/");
    protected static final String USERNAME = "test";
    protected static final String PASSWORD = "test";
    protected static final String UMLAUTS = "\u00E4\u00F6\u00FC\u00DF\u00C4\u00D6\u00DC";

    protected RouteFeedback routeFeedback;

    @Before
    public void setUp() {
        routeFeedback = new RouteFeedback(FEEDBACK, API, new SimpleCredentials(USERNAME, PASSWORD));
    }
}
