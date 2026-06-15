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

package slash.common.helpers;

import org.junit.Test;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.ExceptionHelper.getMessageWithCauses;
import static slash.common.helpers.ExceptionHelper.getRootCause;
import static slash.common.helpers.ExceptionHelper.isComputerOffline;
import static slash.common.helpers.ExceptionHelper.printStackTrace;

public class ExceptionHelperTest {

    @Test
    public void testConnectExceptionIsOffline() {
        assertTrue(isComputerOffline(new ConnectException("refused")));
    }

    @Test
    public void testUnknownHostExceptionIsOffline() {
        assertTrue(isComputerOffline(new UnknownHostException("unknown")));
    }

    @Test
    public void testSSLExceptionIsOffline() {
        assertTrue(isComputerOffline(new SSLException("handshake")));
    }

    @Test
    public void testSocketTimeoutExceptionIsOffline() {
        assertTrue(isComputerOffline(new SocketTimeoutException("timeout")));
    }

    @Test
    public void testRuntimeExceptionIsNotOffline() {
        assertFalse(isComputerOffline(new RuntimeException("something else")));
    }

    @Test
    public void testIOExceptionIsNotOffline() {
        assertFalse(isComputerOffline(new java.io.IOException("generic io")));
    }

    @Test
    public void testGetLocalizedMessageForOfflineException() {
        ConnectException ex = new ConnectException("api.routeconverter.com");
        String msg = getLocalizedMessage(ex);
        assertTrue(msg.contains("not connected to the Internet"));
        assertTrue(msg.contains("api.routeconverter.com"));
    }

    @Test
    public void testGetLocalizedMessageForOnlineExceptionWithMessage() {
        RuntimeException ex = new RuntimeException("something failed");
        assertEquals("something failed", getLocalizedMessage(ex));
    }

    @Test
    public void testGetLocalizedMessageFallsBackToToString() {
        // An exception whose getLocalizedMessage() is null: use a custom subclass
        Throwable ex = new Throwable() {
            @Override
            public String getLocalizedMessage() {
                return null;
            }
            @Override
            public String toString() {
                return "CustomThrowable";
            }
        };
        assertEquals("CustomThrowable", getLocalizedMessage(ex));
    }

    @Test
    public void testPrintStackTraceReturnsNonEmpty() {
        RuntimeException ex = new RuntimeException("test exception");
        String trace = printStackTrace(ex);
        assertNotNull(trace);
        assertTrue(trace.contains("RuntimeException"));
        assertTrue(trace.contains("test exception"));
    }

    @Test
    public void testGetRootCauseUnwrapsToDeepestCause() {
        Throwable root = new NoClassDefFoundError("jdk/net/Sockets");
        Throwable wrapper = new ExceptionInInitializerError(root);
        assertEquals(root, getRootCause(wrapper));
    }

    @Test
    public void testGetRootCauseOfPlainThrowableIsItself() {
        Throwable ex = new RuntimeException("plain");
        assertEquals(ex, getRootCause(ex));
    }

    @Test
    public void testGetMessageWithCausesRendersWholeChain() {
        Throwable root = new NoClassDefFoundError("jdk/net/Sockets");
        Throwable wrapper = new ExceptionInInitializerError(root);
        String message = getMessageWithCauses(wrapper);
        assertTrue(message.contains("ExceptionInInitializerError"));
        assertTrue(message.contains("caused by"));
        assertTrue(message.contains("NoClassDefFoundError"));
        assertTrue(message.contains("jdk/net/Sockets"));
    }

    @Test
    public void testGetMessageWithCausesOfSingleThrowable() {
        String message = getMessageWithCauses(new IllegalStateException("boom"));
        assertEquals("IllegalStateException: boom", message);
    }

    @Test
    public void testGetLocalizedMessageKeepsTopLevelMessageWhenPresent() {
        Throwable root = new NoClassDefFoundError("jdk/net/Sockets");
        Throwable wrapper = new IllegalStateException("outer message", root);
        assertEquals("outer message", getLocalizedMessage(wrapper));
    }

    @Test
    public void testGetLocalizedMessageFallsBackToRootWhenTopLevelHasNoMessage() {
        Throwable root = new NoClassDefFoundError("jdk/net/Sockets");
        Throwable wrapper = new ExceptionInInitializerError(root);  // no own message
        assertEquals("jdk/net/Sockets", getLocalizedMessage(wrapper));
    }
}

