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
package slash.navigation.simple;

import slash.navigation.base.Wgs84Position;

import java.io.PrintWriter;

/**
 * Reads Sygic POI ASCII (.txt) files.
 *
 * Standard Header:
 * ; Created from User Poi file sample.upi
 * ; longitude    latitude    name    phone
 * Standard Format: 2.324360	48.826760	Rue Antoine Chantin(14eme Arrondissement Paris), Paris	+123456789
 *
 * @author Christian Pesch
 */

public class SygicAsciiFormat extends SygicFormat {

    public String getName() {
        return "Sygic POI ASCII (*" + getExtension() + ")";
    }

    public boolean isSupportsWriting() {
        return false;
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        throw new UnsupportedOperationException();
    }
}