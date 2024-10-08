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
        return throwable.getLocalizedMessage() != null ? throwable.getLocalizedMessage() : throwable.toString();
    }

    public static String printStackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
