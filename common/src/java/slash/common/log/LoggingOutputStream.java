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

package slash.common.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An OutputStream that writes contents to a Logger upon each call to {@link #flush()}.
 * <p>
 * Based on http://blogs.sun.com/nickstephen/entry/java_redirecting_system_out_and
 *
 * @author Christian Pesch
 */

public class LoggingOutputStream extends ByteArrayOutputStream {
    private String lineSeparator = System.getProperty("line.separator");
    private Logger logger;
    private Level level;

    public LoggingOutputStream(Logger logger, Level level) {
        super();
        this.logger = logger;
        this.level = level;
    }

    public void flush() throws IOException {
        synchronized (this) {
            super.flush();

            String record = this.toString();
            super.reset();

            // avoid empty records
            if (record.length() == 0 || record.equals(lineSeparator)) {
                return;
            }

            logger.logp(level, "", "", record);
        }
    }
}