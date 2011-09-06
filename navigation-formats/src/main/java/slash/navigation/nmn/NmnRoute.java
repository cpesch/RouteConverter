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

import slash.common.io.CompactCalendar;
import slash.navigation.base.*;
import slash.navigation.ovl.OvlRoute;
import slash.navigation.simple.OpelNaviFormat;
import slash.navigation.simple.QstarzQ1000Format;

import java.util.ArrayList;
import java.util.List;

/**
 * A Navigon Mobile Navigator (.rte) route.
 *
 * @author Christian Pesch
 */

public class NmnRoute extends SimpleRoute<NmnPosition, NmnFormat> {

    public NmnRoute(NmnFormat format, RouteCharacteristics characteristics, String name, List<NmnPosition> positions) {
        super(format, characteristics, name, positions);
    }

    private NmnRoute asNmnFormat(NmnFormat format) {
        List<NmnPosition> nmnPositions = new ArrayList<NmnPosition>(getPositions());
        return new NmnRoute(format, getCharacteristics(), name, nmnPositions);
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

    public NmnRoute asNmn7Format() {
        if (getFormat() instanceof Nmn7Format)
            return this;
        return asNmnFormat(new Nmn7Format());
    }
    
    public SimpleRoute asNmnRouteFormat() {
        return asSimpleFormat(new NmnRouteFormat());
    }

    public SimpleRoute asNmnUrlFormat() {
        return asSimpleFormat(new NmnUrlFormat());
    }

    public SimpleRoute asOpelNaviFormat() {
        return asSimpleFormat(new OpelNaviFormat());
    }

    public OvlRoute asOvlFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (NmnPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new OvlRoute(getCharacteristics(), getName(), wgs84Positions);
    }

    public SimpleRoute asQstarzQ1000Format() {
        return asSimpleFormat(new QstarzQ1000Format());
    }

    protected SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> wgs84positions = new ArrayList<Wgs84Position>();
        for (NmnPosition position : positions) {
            wgs84positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), wgs84positions);
    }


    public NmnPosition createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        return new NmnPosition(longitude, latitude, elevation, speed, time, comment);
    }
}
