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
package slash.navigation.nmn.bindingcruiser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Settings {
    public int VT = 1; // Vehicle Type
    public int BE = 0; // Bending
    public int FR = 0; // Ferries, values are: 0=allow, 1=avoid, 2=forbid (nur 0 und 2 nutzen!)
    public int ROUND = 0; // Round Trip
    public int RT = 3; // Route Type
    public int SR = 0; // Service Roads
    public int HOV = 0; // HOV Lanes
    public int HW = 0; // Highways
    public int TR = 0; // Tollroads, values are: 0=allow, 1=avoid, 2=forbid (nur 0 und 2 nutzen!)
    public int CU = 1; // Curvyness, values are: 0=less curvy ... 5=very curvy (nur so nutzen: 1=Autobahn erlaubt, 2=Autobahn verboten)
}
