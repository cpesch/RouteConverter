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

import slash.navigation.bcr.BcrFormat;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.bcr.BcrRoute;
import slash.navigation.bcr.MTP0607Format;
import slash.navigation.bcr.MTP0809Format;
import slash.navigation.copilot.CoPilot6Format;
import slash.navigation.copilot.CoPilot7Format;
import slash.navigation.copilot.CoPilot8Format;
import slash.navigation.copilot.CoPilot9Format;
import slash.navigation.fpl.GarminFlightPlanFormat;
import slash.navigation.gopal.GoPal3Route;
import slash.navigation.gopal.GoPal5Route;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.itn.TomTom5RouteFormat;
import slash.navigation.itn.TomTom8RouteFormat;
import slash.navigation.itn.TomTomPosition;
import slash.navigation.itn.TomTomRoute;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.klicktel.KlickTelRoute;
import slash.navigation.kml.BaseKmlFormat;
import slash.navigation.kml.Igo8RouteFormat;
import slash.navigation.kml.Kml20Format;
import slash.navigation.kml.Kml21Format;
import slash.navigation.kml.Kml22BetaFormat;
import slash.navigation.kml.Kml22Format;
import slash.navigation.kml.KmlPosition;
import slash.navigation.kml.KmlRoute;
import slash.navigation.kml.Kmz20Format;
import slash.navigation.kml.Kmz21Format;
import slash.navigation.kml.Kmz22BetaFormat;
import slash.navigation.kml.Kmz22Format;
import slash.navigation.lmx.NokiaLandmarkExchangeFormat;
import slash.navigation.mm.MagicMaps2GoFormat;
import slash.navigation.mm.MagicMapsIktRoute;
import slash.navigation.mm.MagicMapsPthRoute;
import slash.navigation.nmea.BaseNmeaFormat;
import slash.navigation.nmea.MagellanExploristFormat;
import slash.navigation.nmea.MagellanRouteFormat;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.nmea.NmeaPosition;
import slash.navigation.nmea.NmeaRoute;
import slash.navigation.nmn.NavigatingPoiWarnerFormat;
import slash.navigation.nmn.Nmn4Format;
import slash.navigation.nmn.Nmn5Format;
import slash.navigation.nmn.Nmn6FavoritesFormat;
import slash.navigation.nmn.Nmn6Format;
import slash.navigation.nmn.Nmn7Format;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.nmn.NmnPosition;
import slash.navigation.nmn.NmnRoute;
import slash.navigation.nmn.NmnRouteFormat;
import slash.navigation.nmn.NmnUrlFormat;
import slash.navigation.ovl.OvlRoute;
import slash.navigation.simple.ColumbusV900ProfessionalFormat;
import slash.navigation.simple.ColumbusV900StandardFormat;
import slash.navigation.simple.GlopusFormat;
import slash.navigation.simple.GoogleMapsUrlFormat;
import slash.navigation.simple.GpsTunerFormat;
import slash.navigation.simple.GroundTrackFormat;
import slash.navigation.simple.HaicomLoggerFormat;
import slash.navigation.simple.Iblue747Format;
import slash.navigation.simple.KienzleGpsFormat;
import slash.navigation.simple.KompassFormat;
import slash.navigation.simple.NavilinkFormat;
import slash.navigation.simple.OpelNaviFormat;
import slash.navigation.simple.QstarzQ1000Format;
import slash.navigation.simple.Route66Format;
import slash.navigation.simple.SygicAsciiFormat;
import slash.navigation.simple.SygicUnicodeFormat;
import slash.navigation.simple.WebPageFormat;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;
import slash.navigation.tour.TourPosition;
import slash.navigation.tour.TourRoute;
import slash.navigation.util.RouteComments;
import slash.navigation.viamichelin.ViaMichelinRoute;
import slash.navigation.wbt.WintecWbt201Tk1Format;
import slash.navigation.wbt.WintecWbt201Tk2Format;
import slash.navigation.wbt.WintecWbt202TesFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the simple most route.
 *
 * @author Christian Pesch
 */

public abstract class SimpleRoute<P extends BaseNavigationPosition, F extends SimpleFormat> extends BaseRoute<P, F> {
    protected String name;
    protected List<P> positions;

    public SimpleRoute(F format, RouteCharacteristics characteristics, List<P> positions) {
        this(format, characteristics, null, positions);
    }

    public SimpleRoute(F format, RouteCharacteristics characteristics, String name, List<P> positions) {
        super(format, characteristics);
        this.name = name;
        this.positions = positions;
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

    public List<P> getPositions() {
        return positions;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, P position) {
        positions.add(index, position);
    }


    private BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<BcrPosition>();
        for (P position : positions) {
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

    private TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format) {
        List<TomTomPosition> tomTomPositions = new ArrayList<TomTomPosition>();
        for (P position : positions) {
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

    public SimpleRoute asKienzleGpsFormat() {
        if (getFormat() instanceof KienzleGpsFormat)
            return this;
        return asSimpleFormat(new KienzleGpsFormat());
    }

    public KlickTelRoute asKlickTelRouteFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (P position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new KlickTelRoute(getName(), wgs84Positions);
    }

    private KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<KmlPosition>();
        for (P position : positions) {
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
        for (P position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new MagicMapsIktRoute(getName(), getDescription(), wgs84Positions);
    }

    public MagicMapsPthRoute asMagicMapsPthFormat() {
        List<GkPosition> gkPositions = new ArrayList<GkPosition>();
        for (P position : positions) {
            gkPositions.add(position.asGkPosition());
        }
        return new MagicMapsPthRoute(getCharacteristics(), gkPositions);
    }


    protected abstract SimpleRoute asSimpleFormat(SimpleFormat format);

    public SimpleRoute asColumbusV900StandardFormat() {
        if (getFormat() instanceof ColumbusV900StandardFormat)
            return this;
        return asSimpleFormat(new ColumbusV900StandardFormat());
    }

    public SimpleRoute asColumbusV900ProfessionalFormat() {
        if (getFormat() instanceof ColumbusV900ProfessionalFormat)
            return this;
        return asSimpleFormat(new ColumbusV900ProfessionalFormat());
    }

    public SimpleRoute asCoPilot6Format() {
        if (getFormat() instanceof CoPilot6Format)
            return this;
        return asSimpleFormat(new CoPilot6Format());
    }

    public SimpleRoute asCoPilot7Format() {
        if (getFormat() instanceof CoPilot7Format)
            return this;
        return asSimpleFormat(new CoPilot7Format());
    }

    public SimpleRoute asCoPilot8Format() {
        if (getFormat() instanceof CoPilot8Format)
            return this;
        return asSimpleFormat(new CoPilot8Format());
    }

    public SimpleRoute asCoPilot9Format() {
        if (getFormat() instanceof CoPilot9Format)
            return this;
        return asSimpleFormat(new CoPilot9Format());
    }

    public SimpleRoute asGlopusFormat() {
        if (getFormat() instanceof GlopusFormat)
            return this;
        return asSimpleFormat(new GlopusFormat());
    }

    public SimpleRoute asGoogleMapsUrlFormat() {
        if (getFormat() instanceof GoogleMapsUrlFormat)
            return this;
        return asSimpleFormat(new GoogleMapsUrlFormat());
    }

    public GoPal3Route asGoPal3RouteFormat() {
        List<GoPalPosition> gopalPositions = new ArrayList<GoPalPosition>();
        for (P position : positions) {
            gopalPositions.add(position.asGoPalRoutePosition());
        }
        return new GoPal3Route(getName(), gopalPositions);
    }

    public GoPal5Route asGoPal5RouteFormat() {
        List<GoPalPosition> gopalPositions = new ArrayList<GoPalPosition>();
        for (P position : positions) {
            gopalPositions.add(position.asGoPalRoutePosition());
        }
        return new GoPal5Route(getName(), gopalPositions);
    }

    public SimpleRoute asGoPalTrackFormat() {
        if (getFormat() instanceof GoPalTrackFormat)
            return this;
        return asSimpleFormat(new GoPalTrackFormat());
    }

    public SimpleRoute asGpsTunerFormat() {
        if (getFormat() instanceof GpsTunerFormat)
            return this;
        return asSimpleFormat(new GpsTunerFormat());
    }

    public SimpleRoute asGroundTrackFormat() {
        if (getFormat() instanceof GroundTrackFormat)
            return this;
        return asSimpleFormat(new GroundTrackFormat());
    }

    public SimpleRoute asHaicomLoggerFormat() {
        if (getFormat() instanceof HaicomLoggerFormat)
            return this;
        return asSimpleFormat(new HaicomLoggerFormat());
    }

    public SimpleRoute asIblue747Format() {
        if (getFormat() instanceof Iblue747Format)
            return this;
        return asSimpleFormat(new Iblue747Format());
    }

    public SimpleRoute asKompassFormat() {
        if (getFormat() instanceof KompassFormat)
            return this;
        return asSimpleFormat(new KompassFormat());
    }

    public SimpleRoute asMagicMaps2GoFormat() {
        if (getFormat() instanceof MagicMaps2GoFormat)
            return this;
        return asSimpleFormat(new MagicMaps2GoFormat());
    }

    public SimpleRoute asNavigatingPoiWarnerFormat() {
        if (getFormat() instanceof NavigatingPoiWarnerFormat)
            return this;
        return asSimpleFormat(new NavigatingPoiWarnerFormat());
    }

    public SimpleRoute asNavilinkFormat() {
        if (getFormat() instanceof NavilinkFormat)
            return this;
        return asSimpleFormat(new NavilinkFormat());
    }

    public SimpleRoute asRoute66Format() {
        if (getFormat() instanceof Route66Format)
            return this;
        return asSimpleFormat(new Route66Format());
    }

    public SimpleRoute asSygicAsciiFormat() {
        if (getFormat() instanceof SygicAsciiFormat)
            return this;
        return asSimpleFormat(new SygicAsciiFormat());
    }

    public SimpleRoute asSygicUnicodeFormat() {
        if (getFormat() instanceof SygicUnicodeFormat)
            return this;
        return asSimpleFormat(new SygicUnicodeFormat());
    }

    public SimpleRoute asWebPageFormat() {
        if (getFormat() instanceof WebPageFormat)
            return this;
        return asSimpleFormat(new WebPageFormat());
    }

    public SimpleRoute asWintecWbt201Tk1Format() {
        if (getFormat() instanceof WintecWbt201Tk1Format)
            return this;
        return asSimpleFormat(new WintecWbt201Tk1Format());
    }

    public SimpleRoute asWintecWbt201Tk2Format() {
        if (getFormat() instanceof WintecWbt201Tk2Format)
            return this;
        return asSimpleFormat(new WintecWbt201Tk2Format());
    }

    public SimpleRoute asWintecWbt202TesFormat() {
        if (getFormat() instanceof WintecWbt202TesFormat)
            return this;
        return asSimpleFormat(new WintecWbt202TesFormat());
    }

    private GpxRoute asGpxFormat(GpxFormat format) {
        List<GpxPosition> gpxPositions = new ArrayList<GpxPosition>();
        for (P position : positions) {
            gpxPositions.add(position.asGpxPosition());
        }
        return new GpxRoute(format, getCharacteristics(), getName(), getDescription(), gpxPositions);
    }

    public GpxRoute asGarminFlightPlanFormat() {
        return asGpxFormat(new GarminFlightPlanFormat());
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

    private NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> nmeaPositions = new ArrayList<NmeaPosition>();
        for (P position : positions) {
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
        for (P position : positions) {
            nmnPositions.add(position.asNmnPosition());
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
        if (getFormat() instanceof NmnRouteFormat)
            return this;
        return asSimpleFormat(new NmnRouteFormat());
    }

    public SimpleRoute asNmnUrlFormat() {
        if (getFormat() instanceof NmnUrlFormat)
            return this;
        return asSimpleFormat(new NmnUrlFormat());
    }

    public SimpleRoute asOpelNaviFormat() {
        if (getFormat() instanceof OpelNaviFormat)
            return this;
        return asSimpleFormat(new OpelNaviFormat());
    }

    public OvlRoute asOvlFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (P position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new OvlRoute(getCharacteristics(), getName(), wgs84Positions);
    }

    public SimpleRoute asQstarzQ1000Format() {
        if (getFormat() instanceof QstarzQ1000Format)
            return this;
        return asSimpleFormat(new QstarzQ1000Format());
    }

    public TourRoute asTourFormat() {
        List<TourPosition> tourPositions = new ArrayList<TourPosition>();
        for (P position : positions) {
            tourPositions.add(position.asTourPosition());
        }
        return new TourRoute(getName(), tourPositions);
    }

    public ViaMichelinRoute asViaMichelinFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (P position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new ViaMichelinRoute(getName(), wgs84Positions);
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleRoute route = (SimpleRoute) o;

        return !(name != null ? !name.equals(route.name) : route.name != null) &&
                characteristics.equals(route.characteristics) &&
                positions.equals(route.positions);
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + characteristics.hashCode();
        result = 29 * result + positions.hashCode();
        return result;
    }
}
