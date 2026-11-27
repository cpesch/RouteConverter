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
package slash.navigation.base;

import java.io.IOException;

/**
 * Thrown by a format that recognized a file as its own - by a header, a signature or a magic
 * number - but cannot read its content, because it is damaged, truncated or of a version the
 * format does not support.
 * <p>
 * A format that does not recognize a file declines it by throwing any other exception or by
 * reading no routes. A format that recognized the file throws this exception instead, so that
 * {@link NavigationFormatParser} reports a failure with this message rather than a successful
 * read of an empty route. Another format that reads routes from the file still wins.
 *
 * @author Christian Pesch
 */

public class UnreadableFileException extends IOException {
    public UnreadableFileException(String message) {
        super(message);
    }

    public UnreadableFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
