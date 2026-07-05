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

import javax.net.ssl.SSLException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static java.util.Collections.newSetFromMap;

/**
 * Provides exception helpers
 *
 * @author Christian Pesch
 */

public class ExceptionHelper {
    public static boolean isComputerOffline(Throwable throwable) {
        return throwable instanceof ConnectException || throwable instanceof UnknownHostException ||
                throwable instanceof SSLException || throwable instanceof SocketTimeoutException;
    }

    public static String getLocalizedMessage(Throwable throwable) {
        if (isComputerOffline(throwable))
            return "Your computer is not connected to the Internet and\n" +
                    "cannot access " + throwable.getMessage() + ".";
        if (throwable.getLocalizedMessage() != null)
            return throwable.getLocalizedMessage();
        // a wrapper without its own message (e.g. ExceptionInInitializerError)
        // would otherwise display just its class name; fall back to the root
        Throwable root = getRootCause(throwable);
        return root.getLocalizedMessage() != null ? root.getLocalizedMessage() : root.toString();
    }

    /**
     * Walks the {@link Throwable#getCause() cause} chain from {@code throwable} to the
     * deepest cause and returns every distinct throwable on the way, outermost first.
     * <p>
     * The walk is cycle-safe: a throwable is only visited once (identity-based), so a
     * self-referential ({@code A -> A}) or looping ({@code A -> B -> A}) chain, which a
     * naive {@code getCause()} loop would follow forever, terminates. This is the single
     * cause-chain traversal every other helper here (and {@code DiagnosticReport}) shares.
     */
    public static List<Throwable> getCauseChain(Throwable throwable) {
        List<Throwable> chain = new ArrayList<>();
        Set<Throwable> visited = newSetFromMap(new IdentityHashMap<>());
        Throwable cause = throwable;
        // visited.add() returns false once a throwable recurs, which ends the walk on a
        // self-referential (A -> A) or looping (A -> B -> A) chain
        while (cause != null && visited.add(cause)) {
            chain.add(cause);
            cause = cause.getCause();
        }
        return chain;
    }

    /**
     * Walks the {@link Throwable#getCause() cause} chain to the deepest cause.
     * A wrapper like ExceptionInInitializerError or InvocationTargetException
     * carries the real fault (e.g. a NoClassDefFoundError for a missing JDK
     * module) as its cause; this returns that fault instead of the wrapper.
     * Cycle-safe via {@link #getCauseChain}.
     */
    public static Throwable getRootCause(Throwable throwable) {
        List<Throwable> chain = getCauseChain(throwable);
        return chain.get(chain.size() - 1);
    }

    /**
     * Renders the throwable and its whole cause chain as one line, e.g.
     * "ExceptionInInitializerError caused by NoClassDefFoundError: jdk/net/Sockets".
     * Surfaces the underlying fault that a single getMessage() hides -- the
     * outermost message is often a generic "Could not initialize class ...".
     * Cycle-safe via {@link #getCauseChain}.
     */
    public static String getMessageWithCauses(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        for (Throwable cause : getCauseChain(throwable)) {
            if (builder.length() > 0)
                builder.append("\ncaused by ");
            builder.append(cause.getClass().getSimpleName());
            String message = cause.getLocalizedMessage();
            if (message != null)
                builder.append(": ").append(message);
        }
        return builder.toString();
    }

    public static String printStackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
