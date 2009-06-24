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

package slash.navigation.gopal;

import slash.navigation.*;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;
import slash.navigation.simple.*;
import slash.navigation.util.RouteComments;
import slash.navigation.util.CompactCalendar;
import slash.navigation.klicktel.KlickTelRoute;
import slash.navigation.bcr.*;
import slash.navigation.copilot.CoPilot6Format;
import slash.navigation.copilot.CoPilot7Format;
import slash.navigation.gopal.binding3.Tour;
import slash.navigation.gpx.*;
import slash.navigation.itn.*;
import slash.navigation.kml.*;
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
 * A GoPal Route (.xml) route.
 *
 * @author Christian Pesch
 */

public class GoPalRoute extends BaseRoute<GoPalPosition, GoPalRouteFormat> {
    private String name;
    private Tour.Options options;
    private List<GoPalPosition> positions;


    public GoPalRoute(String name, List<GoPalPosition> positions) {
        this(name, defaultOptions(), positions);
    }

    private static Tour.Options defaultOptions() {
        Tour.Options options = new Tour.Options();
        /*
            Fahrzeugtype:       Type="3"        => 0=PKW	1=Fußgänger 2=Fahrrad   3=Motorrad
            Art der Route:		Mode="2"        => 0=kurz   1=schnell   2=Ökonomisch
            Mautstrassen:		TollRoad="1"    => 0= meiden    1=verwenden
            Fähren:				Ferries="1"     => 0= meiden    1=verwenden
            Tunnel:				Tunnels="1"     => 0= meiden    1=verwenden
            Stauumfahrung:		TTIMode="0"     => 0= automatisch 1= manuell 2=keine
            Autobahn:			MotorWays="0"   => 0= meiden    1=verwenden
         */
        options.setType((short) 3);
        options.setMode((short) 2);
        options.setTollRoad((short) 1);
        options.setFerries((short) 1);
        options.setTunnels((short) 1);
        options.setTTIMode((short) 0);
        options.setMotorWays((short) 0);
        options.setVehicleSpeedMotorway((short) 33);
        options.setVehicleSpeedNonMotorway((short) 27);
        options.setPedestrianSpeed((short) 1);
        options.setVehicleSpeedInPedestrianArea((short) 2);
        options.setCyclistSpeed((short) 4);
        return options;
    }

    public GoPalRoute(String name, Tour.Options options, List<GoPalPosition> positions) {
        super(new GoPalRouteFormat(), RouteCharacteristics.Route);
        this.options = options;
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

    public Tour.Options getOptions() {
        return options;
    }

    public List<GoPalPosition> getPositions() {
        return positions;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, GoPalPosition position) {
        positions.add(index, position);
    }


    public GoPalPosition createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        return new GoPalPosition(longitude, latitude, elevation, speed, time, comment);
    }

    private BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<BcrPosition>();
        for (GoPalPosition position : positions) {
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
        for (GoPalPosition position : positions) {
            gpxPositions.add(position.asGpxPosition());
        }
        return new GpxRoute(format, RouteCharacteristics.Route, getName(), getDescription(), gpxPositions);
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

    private TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format) {
        List<TomTomPosition> tomTomPositions = new ArrayList<TomTomPosition>();
        for (GoPalPosition position : positions) {
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
        for (GoPalPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new KlickTelRoute(getName(), wgs84Positions);
    }

    private KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<KmlPosition>();
        for (GoPalPosition position : positions) {
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
        for (GoPalPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new MagicMapsIktRoute(getName(), getDescription(), wgs84Positions);
    }

    public MagicMapsPthRoute asMagicMapsPthFormat() {
        List<GkPosition> gkPositions = new ArrayList<GkPosition>();
        for (GoPalPosition position : positions) {
            gkPositions.add(position.asGkPosition());
        }
        return new MagicMapsPthRoute(getCharacteristics(), gkPositions);
    }

    private NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> nmeaPositions = new ArrayList<NmeaPosition>();
        for (GoPalPosition position : positions) {
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
        for (GoPalPosition Wgs84Position : positions) {
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


    public OvlRoute asOvlFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (GoPalPosition position : positions) {
            wgs84Positions.add(position.asOvlPosition());
        }
        return new OvlRoute(getCharacteristics(), getName(), wgs84Positions);
    }


    private SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> gopalPositions = new ArrayList<Wgs84Position>();
        for (GoPalPosition position : positions) {
            gopalPositions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), gopalPositions);
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
        return this;
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
        for (GoPalPosition position : positions) {
            tourPositions.add(position.asTourPosition());
        }
        return new TourRoute(getName(), tourPositions);
    }

    public ViaMichelinRoute asViaMichelinFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (GoPalPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new ViaMichelinRoute(getName(), wgs84Positions);
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GoPalRoute gopalRoute = (GoPalRoute) o;

        return !(name != null ? !name.equals(gopalRoute.name) : gopalRoute.name != null) &&
                !(positions != null ? !positions.equals(gopalRoute.positions) : gopalRoute.positions != null);
    }

    public int hashCode() {
        int result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (positions != null ? positions.hashCode() : 0);
        return result;
    }
}
