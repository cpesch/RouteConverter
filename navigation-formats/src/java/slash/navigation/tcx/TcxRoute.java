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

package slash.navigation.tcx;

import slash.navigation.*;
import slash.navigation.bcr.*;
import slash.navigation.copilot.CoPilot6Format;
import slash.navigation.copilot.CoPilot7Format;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.gopal.GoPalRoute;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.gpx.*;
import slash.navigation.itn.ItnPosition;
import slash.navigation.itn.ItnRoute;
import slash.navigation.kml.*;
import slash.navigation.mm.MagicMapsIktRoute;
import slash.navigation.mm.MagicMapsPthRoute;
import slash.navigation.nmea.*;
import slash.navigation.nmn.*;
import slash.navigation.ovl.OvlRoute;
import slash.navigation.tcx.binding2.PositionT;
import slash.navigation.tcx.binding2.TrackpointT;
import slash.navigation.tour.TourPosition;
import slash.navigation.tour.TourRoute;
import slash.navigation.viamichelin.ViaMichelinRoute;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * A Training Center Database (.tcx) route.
 *
 * @author Christian Pesch
 */

public class TcxRoute extends BaseRoute<TcxPosition, TcxFormat> {
    private List<TcxPosition> positions;


    public TcxRoute(TcxFormat format, RouteCharacteristics characteristics, List<TcxPosition> positions) {
        super(format, characteristics);
        this.positions = positions;
    }

    public String getName() {
        return RouteComments.createRouteName(positions); // TODO
    }

    public void setName(String name) {
        // TODO
    }

    public List<String> getDescription() {
        return null; // TODO
    }

    public List<TcxPosition> getPositions() {
        return positions;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, TcxPosition position) {
        positions.add(index, position);
    }


    public TcxPosition createPosition(Double longitude, Double latitude, Calendar time, String comment) {
        // TODO COMMENT?
        PositionT positionT = new PositionT();
        positionT.setLatitudeDegrees(latitude); // TODO degrees?
        positionT.setLongitudeDegrees(latitude); // TODO degrees?
        TrackpointT trackpointT = new TrackpointT();
        trackpointT.setTime(TcxFormat.formatTime(time));
        trackpointT.setPosition(positionT);
        // TODO muﬂ noch in Struktur...
        return null; // TODO new TcxPosition(null, null, null, null, null);
    }

    private BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<BcrPosition>();
        for (TcxPosition position : positions) {
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
        for (TcxPosition position : positions) {
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


    public ItnRoute asItnFormat() {
        List<ItnPosition> itnPositions = new ArrayList<ItnPosition>();
        for (TcxPosition position : positions) {
            itnPositions.add(position.asItnPosition());
        }
        return new ItnRoute(getCharacteristics(), getName(), itnPositions);
    }


    private KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<KmlPosition>();
        for (TcxPosition position : positions) {
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

    public KmlRoute asKml22Format() {
        return asKmlFormat(new Kml22Format());
    }

    public KmlRoute asKmz20Format() {
        return asKmlFormat(new Kmz20Format());
    }

    public KmlRoute asKmz21Format() {
        return asKmlFormat(new Kmz21Format());
    }

    public KmlRoute asKmz22Format() {
        return asKmlFormat(new Kmz22Format());
    }

    
    public MagicMapsIktRoute asMagicMapsIktFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (TcxPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new MagicMapsIktRoute(getName(), getDescription(), wgs84Positions);
    }

    public MagicMapsPthRoute asMagicMapsPthFormat() {
        List<GkPosition> gkPositions = new ArrayList<GkPosition>();
        for (TcxPosition position : positions) {
            gkPositions.add(position.asGkPosition());
        }
        return new MagicMapsPthRoute(getCharacteristics(), gkPositions);
    }

    private NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> nmeaPositions = new ArrayList<NmeaPosition>();
        for (TcxPosition position : positions) {
            nmeaPositions.add(position.asNmeaPosition());
        }
        return new NmeaRoute(format, getCharacteristics(), nmeaPositions);
    }

    public NmeaRoute asMagellanExploristFormat() {
        return asNmeaFormat(new MagellanExploristFormat());
    }

    public NmeaRoute asNmeaFormat() {
        return asNmeaFormat(new NmeaFormat());
    }

    private NmnRoute asNmnFormat(NmnFormat format) {
        List<NmnPosition> nmnPositions = new ArrayList<NmnPosition>();
        for (TcxPosition Wgs84Position : positions) {
            nmnPositions.add(Wgs84Position.asNmnPosition());
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
        for (TcxPosition position : positions) {
            wgs84Positions.add(position.asOvlPosition());
        }
        return new OvlRoute(getCharacteristics(), getName(), wgs84Positions);
    }


    private SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> tcxPositions = new ArrayList<Wgs84Position>();
        for (TcxPosition position : positions) {
            tcxPositions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), tcxPositions);
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
        for (TcxPosition position : positions) {
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

    public SimpleRoute asNavigatingPoiWarnerFormat() {
        return asSimpleFormat(new NavigatingPoiWarnerFormat());
    }

    public SimpleRoute asRoute66Format() {
        return asSimpleFormat(new Route66Format());
    }

    public TourRoute asTourFormat() {
        List<TourPosition> tourPositions = new ArrayList<TourPosition>();
        for (TcxPosition position : positions) {
            tourPositions.add(position.asTourPosition());
        }
        return new TourRoute(getName(), tourPositions);
    }

    public ViaMichelinRoute asViaMichelinFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (TcxPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new ViaMichelinRoute(getName(), wgs84Positions);
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TcxRoute tcxRoute = (TcxRoute) o;

        return !(getName() != null ? !getName().equals(tcxRoute.getName()) : tcxRoute.getName() != null) &&
                !(positions != null ? !positions.equals(tcxRoute.positions) : tcxRoute.positions != null);
    }

    public int hashCode() {
        int result = (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (positions != null ? positions.hashCode() : 0);
        return result;
    }
}
