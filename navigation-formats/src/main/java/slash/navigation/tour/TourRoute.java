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

package slash.navigation.tour;

import slash.common.io.CompactCalendar;
import slash.navigation.base.*;
import slash.navigation.bcr.*;
import slash.navigation.copilot.CoPilot6Format;
import slash.navigation.copilot.CoPilot7Format;
import slash.navigation.copilot.CoPilot8Format;
import slash.navigation.gopal.GoPal3Route;
import slash.navigation.gopal.GoPal5Route;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.gpx.*;
import slash.navigation.itn.*;
import slash.navigation.klicktel.KlickTelRoute;
import slash.navigation.kml.*;
import slash.navigation.lmx.NokiaLandmarkExchangeFormat;
import slash.navigation.mm.MagicMaps2GoFormat;
import slash.navigation.mm.MagicMapsIktRoute;
import slash.navigation.mm.MagicMapsPthRoute;
import slash.navigation.nmea.*;
import slash.navigation.nmn.*;
import slash.navigation.ovl.OvlRoute;
import slash.navigation.simple.*;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;
import slash.navigation.util.RouteComments;
import slash.navigation.viamichelin.ViaMichelinRoute;
import slash.navigation.wbt.WintecWbt201Tk1Format;
import slash.navigation.wbt.WintecWbt201Tk2Format;
import slash.navigation.wbt.WintecWbt202TesFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * A Falk Navigator (.tour) route.
 *
 * @author Christian Pesch
 */

public class TourRoute extends BaseRoute<TourPosition, TourFormat> {
    private String name;
    private final List<TourPosition> positions;

    public TourRoute(String name, List<TourPosition> positions) {
        this(new TourFormat(), name, positions);
    }

    public TourRoute(TourFormat format, String name, List<TourPosition> positions) {
        super(format, RouteCharacteristics.Waypoints);
        this.positions = positions;
        setName(name);
    }

    public String getName() {
        return name != null ? name : RouteComments.createRouteName(positions);
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDescription() {
        return null;
    }

    public List<TourPosition> getPositions() {
        return positions;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, TourPosition position) {
        positions.add(index, position);
    }


    public TourPosition createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        return new TourPosition(longitude, latitude, elevation, speed, time, comment);
    }

    private BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<BcrPosition>();
        for (TourPosition position : positions) {
            bcrPositions.add(position.asMTPPosition());
        }
        return new BcrRoute(format, getName(), getDescription(), bcrPositions);
    }

    public BcrRoute asMTP0607Format() {
        return asBcrFormat(new MTP0607Format());
    }

    public BcrRoute asMTP0809Format() {
        return asBcrFormat(new MTP0809Format());
    }

    private GpxRoute asGpxFormat(GpxFormat format) {
        List<GpxPosition> gpxPositions = new ArrayList<GpxPosition>();
        for (TourPosition position : positions) {
            gpxPositions.add(position.asGpxPosition());
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

    public GpxRoute asNokiaLandmarkExchangeFormat() {
        return asGpxFormat(new NokiaLandmarkExchangeFormat());
    }

    private TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format) {
        List<TomTomPosition> tomTomPositions = new ArrayList<TomTomPosition>();
        for (TourPosition position : positions) {
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
        for (TourPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new KlickTelRoute(getName(), wgs84Positions);
    }

    private KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<KmlPosition>();
        for (TourPosition position : positions) {
            kmlPositions.add(position.asKmlPosition());
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

    public KmlRoute asIgo8RouteFormat() {
        return asKmlFormat(new Igo8RouteFormat());
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

    
    public MagicMapsIktRoute asMagicMapsIktFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (TourPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new MagicMapsIktRoute(getName(), getDescription(), wgs84Positions);
    }

    public MagicMapsPthRoute asMagicMapsPthFormat() {
        List<GkPosition> gkPositions = new ArrayList<GkPosition>();
        for (TourPosition position : positions) {
            gkPositions.add(position.asGkPosition());
        }
        return new MagicMapsPthRoute(getCharacteristics(), gkPositions);
    }

    private NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> nmeaPositions = new ArrayList<NmeaPosition>();
        for (TourPosition position : positions) {
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
        for (TourPosition Wgs84Position : positions) {
            nmnPositions.add(Wgs84Position.asNmnPosition());
        }
        return new NmnRoute(format, getCharacteristics(), name, nmnPositions);
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
        for (TourPosition position : positions) {
            wgs84Positions.add(position.asOvlPosition());
        }
        return new OvlRoute(getCharacteristics(), getName(), wgs84Positions);
    }

    public SimpleRoute asQstarzQ1000Format() {
        return asSimpleFormat(new QstarzQ1000Format());
    }

    private SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> tourPositions = new ArrayList<Wgs84Position>();
        for (TourPosition position : positions) {
            tourPositions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), tourPositions);
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

    public SimpleRoute asCoPilot8Format() {
        return asSimpleFormat(new CoPilot8Format());
    }

    public SimpleRoute asGlopusFormat() {
        return asSimpleFormat(new GlopusFormat());
    }

    public SimpleRoute asGoogleMapsUrlFormat() {
        return asSimpleFormat(new GoogleMapsUrlFormat());
    }

    public GoPal3Route asGoPal3RouteFormat() {
        List<GoPalPosition> gopalPositions = new ArrayList<GoPalPosition>();
        for (TourPosition position : positions) {
            gopalPositions.add(position.asGoPalRoutePosition());
        }
        return new GoPal3Route(getName(), gopalPositions);
    }

    public GoPal5Route asGoPal5RouteFormat() {
        List<GoPalPosition> gopalPositions = new ArrayList<GoPalPosition>();
        for (TourPosition position : positions) {
            gopalPositions.add(position.asGoPalRoutePosition());
        }
        return new GoPal5Route(getName(), gopalPositions);
    }

    public SimpleRoute asGoPalTrackFormat() {
        return asSimpleFormat(new GoPalTrackFormat());
    }

    public SimpleRoute asGpsTunerFormat() {
        return asSimpleFormat(new GpsTunerFormat());
    }

    public SimpleRoute asGroundTrackFormat() {
        return asSimpleFormat(new GroundTrackFormat());
    }

    public SimpleRoute asHaicomLoggerFormat() {
        return asSimpleFormat(new HaicomLoggerFormat());
    }

    public SimpleRoute asiBlue747Format() {
        return asSimpleFormat(new iBlue747Format());
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
    
    public SimpleRoute asWebPageFormat() {
        return asSimpleFormat(new WebPageFormat());
    }

    public SimpleRoute asWintecWbt201Tk1Format() {
        return asSimpleFormat(new WintecWbt201Tk1Format());
    }

    public SimpleRoute asWintecWbt201Tk2Format() {
        return asSimpleFormat(new WintecWbt201Tk2Format());
    }

    public SimpleRoute asWintecWbt202TesFormat() {
        return asSimpleFormat(new WintecWbt202TesFormat());
    }

    public TourRoute asTourFormat() {
        return this;
    }

    public ViaMichelinRoute asViaMichelinFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (TourPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new ViaMichelinRoute(getName(), wgs84Positions);
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TourRoute tourRoute = (TourRoute) o;

        return !(name != null ? !name.equals(tourRoute.name) : tourRoute.name != null) &&
                !(positions != null ? !positions.equals(tourRoute.positions) : tourRoute.positions != null);
    }

    public int hashCode() {
        int result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (positions != null ? positions.hashCode() : 0);
        return result;
    }
}
