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

package slash.navigation.nmn;

import slash.navigation.*;
import slash.navigation.ovl.OvlRoute;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * A Navigon Mobile Navigator (.rte) route.
 *
 * @author Christian Pesch
 */

public class NmnRoute extends SimpleRoute<NmnPosition, NmnFormat> {
    public NmnRoute(NmnFormat format, RouteCharacteristics characteristics, List<NmnPosition> positions) {
        super(format, characteristics, positions);
    }

    private NmnRoute asNmnFormat(NmnFormat format) {
        List<NmnPosition> nmnPositions = new ArrayList<NmnPosition>(getPositions());
        return new NmnRoute(format, getCharacteristics(), nmnPositions);
    }

    public NmnRoute asNmn4Format() {
        if (getFormat() instanceof Nmn4Format)
            return this;
        return asNmnFormat(new Nmn4Format());
    }

    public NmnRoute asNmn5Format() {
        if (getFormat() instanceof Nmn5Format)
            return this;
        return asNmnFormat(new Nmn5Format());
    }

    public NmnRoute asNmn6Format() {
        if (getFormat() instanceof Nmn6Format)
            return this;
        return asNmnFormat(new Nmn6Format());
    }

    public NmnRoute asNmn6FavoritesFormat() {
        if (getFormat() instanceof Nmn6FavoritesFormat)
            return this;
        return asNmnFormat(new Nmn6FavoritesFormat());
    }

    public OvlRoute asOvlFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (NmnPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new OvlRoute(getCharacteristics(), getName(), wgs84Positions);
    }

    protected SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> wgs84positions = new ArrayList<Wgs84Position>();
        for (NmnPosition position : positions) {
            wgs84positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), wgs84positions);
    }


    public NmnPosition createPosition(Double longitude, Double latitude, Calendar time, String comment) {
        return new NmnPosition(longitude, latitude, null, time, comment);
    }
}
