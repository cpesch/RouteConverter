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

    Copyright (C) 2007 Per Borgentun, e4borgen(at)yahoo.com
    With lot of inspiration from wbt-200.c
       Copyright (C) 2006 Andy Armstrong
    Translated to Java from GPSBabel mtk_logger.cc (master 9c1e336) for RouteConverter, 2026; modified.
*/

package slash.navigation.holux;

/**
 * Reads MTK Logger Binary (.bin) files of iBlue 747, Qstarz BT-Q1000, Transystem and other MediaTek based loggers.
 * <p>
 * GPSBabel's {@code mtk-bin} format: 8 byte double coordinates, a 4 byte float height and a '*' before
 * the checksum. Dumps with a Holux marker string are left to the {@link HoluxM241BinaryFormat}.
 *
 * @author Christian Pesch
 */

public class MtkLoggerBinaryFormat extends MtkBinaryFormat {

    public String getName() {
        return "MTK Logger Binary (*" + getExtension() + ")";
    }

    boolean isHolux() {
        return false;
    }
}
