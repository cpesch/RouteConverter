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

package slash.navigation.kml;

import slash.navigation.*;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;
import slash.navigation.util.CompactCalendar;
import slash.navigation.simple.*;
import slash.navigation.klicktel.KlickTelRoute;
import slash.navigation.bcr.*;
import slash.navigation.copilot.CoPilot6Format;
import slash.navigation.copilot.CoPilot7Format;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.gopal.GoPalRoute;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.gpx.*;
import slash.navigation.itn.*;
import slash.navigation.mm.MagicMapsIktRoute;
import slash.navigation.mm.MagicMapsPthRoute;
import slash.navigation.mm.MagicMaps2GoFormat;
import slash.navigation.nmea.*;
import slash.navigation.nmn.*;
import slash.navigation.ovl.OvlRoute;
import slash.navigation.tour.TourPosition;
import slash.navigation.tour.TourRoute;
import slash.navigation.viamichelin.ViaMichelinRoute;

import java.util.ArrayList;
import java.util.List;

/**
 * A Google Earth (.kml) route.
 *
 * @author Christian Pesch
 */

public class KmlRoute extends BaseRoute<KmlPosition, BaseKmlFormat> {
    private String name;
    private List<String> description;
    private List<KmlPosition> positions;

    public KmlRoute(BaseKmlFormat format, RouteCharacteristics characteristics,
                    String name, List<String> description, List<KmlPosition> positions) {
        super(format, characteristics);
        this.name = name;
        this.description = description;
        this.positions = positions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDescription() {
        return description;
    }

    public List<KmlPosition> getPositions() {
        return positions;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, KmlPosition position) {
        positions.add(index, position);
    }


    public KmlPosition createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        return new KmlPosition(longitude, latitude, elevation, speed, time, comment);
    }

    private BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<BcrPosition>();
        for (KmlPosition kmlPosition : positions) {
            bcrPositions.add(kmlPosition.asMTPPosition());
        }
        return new BcrRoute(format, getName(), getDescription(), bcrPositions);
    }

    public BcrRoute asMTP0607Format() {
        return asBcrFormat(new MTP0607Format());
    }

    public BcrRoute asMTP0809Format() {
        return asBcrFormat(new MTP0809Format());
    }

    private TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format) {
        List<TomTomPosition> tomTomPositions = new ArrayList<TomTomPosition>();
        for (KmlPosition position : positions) {
            tomTomPositions.add(position.asTomTomRoutePosition());
        }
        return new TomTomRoute(format, getCharacteristics(), getName(), tomTomPositions);
    }

    public TomTomRoute asTomTom5RouteFormat() {
        return asTomTomRouteFormat(new TomTom5RouteFormat());
    }

    public TomTomRoute asTomTom8RouteFormat() {
        return asTomTomRouteFormat(new TomTom8RouteFormat());
    }

    public KlickTelRoute asKlickTelRouteFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (KmlPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new KlickTelRoute(getName(), wgs84Positions);
    }

    private KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<KmlPosition>(getPositions());
        return new KmlRoute(format, getCharacteristics(), getName(), getDescription(), kmlPositions);
    }

    public KmlRoute asKml20Format() {
        if (getFormat() instanceof Kml20Format)
            return this;
        return asKmlFormat(new Kml20Format());
    }

    public KmlRoute asKml21Format() {
        if (getFormat() instanceof Kml21Format)
            return this;
        return asKmlFormat(new Kml21Format());
    }

    public KmlRoute asKml22BetaFormat() {
        if (getFormat() instanceof Kml22BetaFormat)
            return this;
        return asKmlFormat(new Kml22BetaFormat());
    }

    public KmlRoute asKml22Format() {
        if (getFormat() instanceof Kml22Format)
            return this;
        return asKmlFormat(new Kml22Format());
    }

    public KmlRoute asKmz20Format() {
        if (getFormat() instanceof Kmz20Format)
            return this;
        return asKmlFormat(new Kmz20Format());
    }

    public KmlRoute asKmz21Format() {
        if (getFormat() instanceof Kmz21Format)
            return this;
        return asKmlFormat(new Kmz21Format());
    }

    public KmlRoute asKmz22BetaFormat() {
        if (getFormat() instanceof Kmz22BetaFormat)
            return this;
        return asKmlFormat(new Kmz22BetaFormat());
    }

    public KmlRoute asKmz22Format() {
        if (getFormat() instanceof Kmz22Format)
            return this;
        return asKmlFormat(new Kmz22Format());
    }

    private GpxRoute asGpxFormat(GpxFormat format) {
        List<GpxPosition> gpxPositions = new ArrayList<GpxPosition>();
        for (KmlPosition kmlPosition : positions) {
            gpxPositions.add(kmlPosition.asGpxPosition());
        }
        return new GpxRoute(format, getCharacteristics(), getName(), getDescription(), gpxPositions);
    }

    public GpxRoute asGpx10Format() {
        return asGpxFormat(new Gpx10Format());
    }

    public GpxRoute asGpx11Format() {
        return asGpxFormat(new Gpx11Format());
    }

    public GpxRoute asTcx1Format() {
        return asGpxFormat(new Tcx1Format());
    }

    public GpxRoute asTcx2Format() {
        return asGpxFormat(new Tcx2Format());
    }

    public MagicMapsIktRoute asMagicMapsIktFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (KmlPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new MagicMapsIktRoute(getName(), getDescription(), wgs84Positions);
    }

    public MagicMapsPthRoute asMagicMapsPthFormat() {
        List<GkPosition> gkPositions = new ArrayList<GkPosition>();
        for (KmlPosition position : positions) {
            gkPositions.add(position.asGkPosition());
        }
        return new MagicMapsPthRoute(getCharacteristics(), gkPositions);
    }

    private NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> nmeaPositions = new ArrayList<NmeaPosition>();
        for (KmlPosition position : positions) {
            nmeaPositions.add(position.asNmeaPosition());
        }
        return new NmeaRoute(format, getCharacteristics(), nmeaPositions);
    }

    public NmeaRoute asMagellanExploristFormat() {
        return asNmeaFormat(new MagellanExploristFormat());
    }

    public NmeaRoute asMagellanRouteFormat() {
        return asNmeaFormat(new MagellanRouteFormat());
    }

    public NmeaRoute asNmeaFormat() {
        return asNmeaFormat(new NmeaFormat());
    }

    private NmnRoute asNmnFormat(NmnFormat format) {
        List<NmnPosition> nmnPositions = new ArrayList<NmnPosition>();
        for (KmlPosition kmlPosition : positions) {
            nmnPositions.add(kmlPosition.asNmnPosition());
        }
        return new NmnRoute(format, getCharacteristics(), getName(), nmnPositions);
    }

    public NmnRoute asNmn4Format() {
        return asNmnFormat(new Nmn4Format());
    }

    public NmnRoute asNmn5Format() {
        return asNmnFormat(new Nmn5Format());
    }

    public NmnRoute asNmn6Format() {
        return asNmnFormat(new Nmn6Format());
    }

    public NmnRoute asNmn6FavoritesFormat() {
        return asNmnFormat(new Nmn6FavoritesFormat());
    }

    public NmnRoute asNmn7Format() {
        return asNmnFormat(new Nmn7Format());
    }


    public OvlRoute asOvlFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (KmlPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new OvlRoute(getCharacteristics(), getName(), wgs84Positions);
    }


    private SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (KmlPosition kmlPosition : positions) {
            wgs84Positions.add(kmlPosition.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), wgs84Positions);
    }

    public SimpleRoute asColumbusV900Format() {
        return asSimpleFormat(new ColumbusV900Format());
    }

    public SimpleRoute asCoPilot6Format() {
        return asSimpleFormat(new CoPilot6Format());
    }

    public SimpleRoute asCoPilot7Format() {
        return asSimpleFormat(new CoPilot7Format());
    }

    public SimpleRoute asGlopusFormat() {
        return asSimpleFormat(new GlopusFormat());
    }

    public SimpleRoute asGoogleMapsFormat() {
        return asSimpleFormat(new GoogleMapsFormat());
    }

    public GoPalRoute asGoPalRouteFormat() {
        List<GoPalPosition> gopalPositions = new ArrayList<GoPalPosition>();
        for (KmlPosition position : positions) {
            gopalPositions.add(position.asGoPalRoutePosition());
        }
        return new GoPalRoute(getName(), gopalPositions);
    }

    public SimpleRoute asGoPalTrackFormat() {
        return asSimpleFormat(new GoPalTrackFormat());
    }

    public SimpleRoute asGpsTunerFormat() {
        return asSimpleFormat(new GpsTunerFormat());
    }

    public SimpleRoute asHaicomLoggerFormat() {
        return asSimpleFormat(new HaicomLoggerFormat());
    }

    public SimpleRoute asMagicMaps2GoFormat() {
        return asSimpleFormat(new MagicMaps2GoFormat());
    }

    public SimpleRoute asNavigatingPoiWarnerFormat() {
        return asSimpleFormat(new NavigatingPoiWarnerFormat());
    }

    public SimpleRoute asRoute66Format() {
        return asSimpleFormat(new Route66Format());
    }

    public TourRoute asTourFormat() {
        List<TourPosition> tourPositions = new ArrayList<TourPosition>();
        for (KmlPosition position : positions) {
            tourPositions.add(position.asTourPosition());
        }
        return new TourRoute(getName(), tourPositions);
    }

    public ViaMichelinRoute asViaMichelinFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (KmlPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new ViaMichelinRoute(getName(), wgs84Positions);
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final KmlRoute kmlRoute = (KmlRoute) o;

        return !(description != null ? !description.equals(kmlRoute.description) : kmlRoute.description != null) &&
                !(name != null ? !name.equals(kmlRoute.name) : kmlRoute.name != null) &&
                characteristics.equals(kmlRoute.characteristics) &&
                positions.equals(kmlRoute.positions);
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + (description != null ? description.hashCode() : 0);
        result = 29 * result + characteristics.hashCode();
        result = 29 * result + positions.hashCode();
        return result;
    }
}
