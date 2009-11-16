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

package slash.navigation.gpx;

import slash.navigation.*;
import slash.navigation.bcr.*;
import slash.navigation.copilot.CoPilot6Format;
import slash.navigation.copilot.CoPilot7Format;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.gopal.GoPalRoute;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.itn.*;
import slash.navigation.klicktel.KlickTelRoute;
import slash.navigation.kml.*;
import slash.navigation.mm.MagicMaps2GoFormat;
import slash.navigation.mm.MagicMapsIktRoute;
import slash.navigation.mm.MagicMapsPthRoute;
import slash.navigation.nmea.*;
import slash.navigation.nmn.*;
import slash.navigation.ovl.OvlRoute;
import slash.navigation.simple.*;
import slash.navigation.tcx.Crs1Format;
import slash.navigation.tcx.Tcx2Format;
import slash.navigation.tour.TourPosition;
import slash.navigation.tour.TourRoute;
import slash.common.io.CompactCalendar;
import slash.navigation.viamichelin.ViaMichelinRoute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A GPS Exchange Format (.gpx) route.
 *
 * @author Christian Pesch
 */

public class GpxRoute extends BaseRoute<GpxPosition, GpxFormat> {
    private String name;
    private final List<String> description;
    private final List<GpxPosition> positions;
    private final List<Object> origins;

    GpxRoute(GpxFormat format, RouteCharacteristics characteristics,
             String name, List<String> description, List<GpxPosition> positions,
             Object... origins) {
        super(format, characteristics);
        this.name = name;
        this.description = description;
        this.positions = positions;
        this.origins = Arrays.asList(origins);
    }

    public GpxRoute(GpxFormat format, RouteCharacteristics characteristics,
                    String name, List<String> description, List<GpxPosition> positions) {
        this(format, characteristics, name, description, positions, new Object[0]);
    }

    public GpxRoute(GpxFormat format) {
        this(format, RouteCharacteristics.Waypoints, "?", null, new ArrayList<GpxPosition>());
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

    public List<GpxPosition> getPositions() {
        return positions;
    }

    List<Object> getOrigins() {
        return origins;
    }

    @SuppressWarnings({"unchecked"})
    <T> T getOrigin(Class<T> resultClass) {
        for (Object origin : origins) {
            if (resultClass.isInstance(origin))
                return (T) origin;
        }
        return null;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, GpxPosition position) {
        positions.add(index, position);
    }

    public GpxPosition createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        return new GpxPosition(longitude, latitude, elevation, speed, time, comment);
    }


    private BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<BcrPosition>();
        for (GpxPosition gpxPosition : positions) {
            BcrPosition bcrPosition = gpxPosition.asMTPPosition();
            // shortens comment to better fit to Map&Guide Tourenplaner station list
            String location = gpxPosition.getCity();
            if (location != null)
                bcrPosition.setComment(location);
            bcrPositions.add(bcrPosition);
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
        for (GpxPosition position : positions) {
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
        for (GpxPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new KlickTelRoute(getName(), wgs84Positions);
    }

    private KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<KmlPosition>();
        for (GpxPosition gpxPosition : positions) {
            kmlPositions.add(gpxPosition.asKmlPosition());
        }
        return new KmlRoute(format, getCharacteristics(), getName(), getDescription(), kmlPositions);
    }

    public KmlRoute asKml20Format() {
        return asKmlFormat(new Kml20Format());
    }

    public KmlRoute asKml21Format() {
        return asKmlFormat(new Kml21Format());
    }

    public KmlRoute asKml22BetaFormat() {
        return asKmlFormat(new Kml22BetaFormat());
    }

    public KmlRoute asKml22Format() {
        return asKmlFormat(new Kml22Format());
    }

    public KmlRoute asKmz20Format() {
        return asKmlFormat(new Kmz20Format());
    }

    public KmlRoute asKmz21Format() {
        return asKmlFormat(new Kmz21Format());
    }

    public KmlRoute asKmz22BetaFormat() {
        return asKmlFormat(new Kmz22BetaFormat());
    }

    public KmlRoute asKmz22Format() {
        return asKmlFormat(new Kmz22Format());
    }


    private GpxRoute asGpxFormat(GpxFormat format) {
        List<GpxPosition> gpxPositions = new ArrayList<GpxPosition>(getPositions());
        return new GpxRoute(format, getCharacteristics(), getName(), getDescription(), gpxPositions);
    }

    public GpxRoute asGpx10Format() {
        if (getFormat() instanceof Gpx10Format)
            return this;
        return asGpxFormat(new Gpx10Format());
    }

    public GpxRoute asGpx11Format() {
        if (getFormat() instanceof Gpx11Format)
            return this;
        return asGpxFormat(new Gpx11Format());
    }

    public GpxRoute asCrs1Format() {
        if (getFormat() instanceof Crs1Format)
            return this;
        return asGpxFormat(new Crs1Format());
    }

    public GpxRoute asTcx2Format() {
        if (getFormat() instanceof Tcx2Format)
            return this;
        return asGpxFormat(new Tcx2Format());
    }

    public MagicMapsIktRoute asMagicMapsIktFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (GpxPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new MagicMapsIktRoute(getName(), getDescription(), wgs84Positions);
    }

    public MagicMapsPthRoute asMagicMapsPthFormat() {
        List<GkPosition> gkPositions = new ArrayList<GkPosition>();
        for (GpxPosition position : positions) {
            gkPositions.add(position.asGkPosition());
        }
        return new MagicMapsPthRoute(getCharacteristics(), gkPositions);
    }

    private NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> nmeaPositions = new ArrayList<NmeaPosition>();
        for (GpxPosition position : positions) {
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
        for (GpxPosition gpxPosition : positions) {
            nmnPositions.add(gpxPosition.asNmnPosition());
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
        for (GpxPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new OvlRoute(getCharacteristics(), getName(), wgs84Positions);
    }
    

    private SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (GpxPosition gpxPosition : positions) {
            wgs84Positions.add(gpxPosition.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), wgs84Positions);
    }

    public SimpleRoute asColumbusV900StandardFormat() {
        return asSimpleFormat(new ColumbusV900StandardFormat());
    }

    public SimpleRoute asColumbusV900ProfessionalFormat() {
        return asSimpleFormat(new ColumbusV900ProfessionalFormat());
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
        for (GpxPosition position : positions) {
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

    public SimpleRoute asKompassFormat() {
        return asSimpleFormat(new KompassFormat());
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

    public SimpleRoute asSygicUnicodeFormat() {
        return asSimpleFormat(new SygicUnicodeFormat());
    }

    public TourRoute asTourFormat() {
        List<TourPosition> tourPositions = new ArrayList<TourPosition>();
        for (GpxPosition position : positions) {
            tourPositions.add(position.asTourPosition());
        }
        return new TourRoute(getName(), tourPositions);
    }

    public ViaMichelinRoute asViaMichelinFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (GpxPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new ViaMichelinRoute(getName(), wgs84Positions);
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final GpxRoute gpxRoute = (GpxRoute) o;

        return !(description != null ? !description.equals(gpxRoute.description) : gpxRoute.description != null) &&
                !(name != null ? !name.equals(gpxRoute.name) : gpxRoute.name != null) &&
                characteristics.equals(gpxRoute.characteristics) &&
                positions.equals(gpxRoute.positions);
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
