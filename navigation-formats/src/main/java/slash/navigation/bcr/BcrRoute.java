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

package slash.navigation.bcr;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.GkPosition;
import slash.navigation.base.SimpleFormat;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.fpl.GarminFlightPlanPosition;
import slash.navigation.fpl.GarminFlightPlanRoute;
import slash.navigation.gopal.GoPal3Route;
import slash.navigation.gopal.GoPal5Route;
import slash.navigation.gopal.GoPalPosition;
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
import slash.navigation.kml.KmlPosition;
import slash.navigation.kml.KmlRoute;
import slash.navigation.lmx.NokiaLandmarkExchangeFormat;
import slash.navigation.mm.MagicMapsIktRoute;
import slash.navigation.mm.MagicMapsPthRoute;
import slash.navigation.nmea.BaseNmeaFormat;
import slash.navigation.nmea.MagellanExploristFormat;
import slash.navigation.nmea.MagellanRouteFormat;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.nmea.NmeaPosition;
import slash.navigation.nmea.NmeaRoute;
import slash.navigation.nmn.Nmn4Format;
import slash.navigation.nmn.Nmn5Format;
import slash.navigation.nmn.Nmn6FavoritesFormat;
import slash.navigation.nmn.Nmn6Format;
import slash.navigation.nmn.Nmn7Format;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.nmn.NmnPosition;
import slash.navigation.nmn.NmnRoute;
import slash.navigation.ovl.OvlRoute;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;
import slash.navigation.tour.TourPosition;
import slash.navigation.tour.TourRoute;
import slash.navigation.viamichelin.ViaMichelinRoute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.bcr.BcrFormat.CLIENT_TITLE;
import static slash.navigation.bcr.BcrFormat.COORDINATES_TITLE;
import static slash.navigation.bcr.BcrFormat.DESCRIPTION;
import static slash.navigation.bcr.BcrFormat.DESCRIPTION_LINE_COUNT;
import static slash.navigation.bcr.BcrFormat.DESCRIPTION_TITLE;
import static slash.navigation.bcr.BcrFormat.ROUTE_NAME;
import static slash.navigation.bcr.BcrFormat.ROUTE_TITLE;

/**
 * A Map&Guide Tourenplaner Route (.bcr) route.
 *
 * @author Christian Pesch
 */

public class BcrRoute extends BaseRoute<BcrPosition, BcrFormat> {
    private final List<BcrSection> sections;
    private final List<BcrPosition> positions;


    public BcrRoute(BcrFormat format, List<BcrSection> sections, List<BcrPosition> positions) {
        super(format, Route);
        this.sections = sections;
        this.positions = positions;
    }

    public BcrRoute(BcrFormat format, String name, List<String> description, List<BcrPosition> positions) {
        this(format, new ArrayList<BcrSection>(), positions);
        sections.add(new BcrSection(CLIENT_TITLE));
        sections.add(new BcrSection(COORDINATES_TITLE));
        sections.add(new BcrSection(DESCRIPTION_TITLE));
        sections.add(new BcrSection(ROUTE_TITLE));
        setName(name);
        setDescription(description);
        findSection(CLIENT_TITLE).put("REQUEST", "TRUE");
    }


    List<BcrSection> getSections() {
        return sections;
    }

    BcrSection findSection(String title) {
        for (BcrSection section : getSections()) {
            if (title.equals(section.getTitle()))
                return section;
        }
        return null;
    }

    public String getName() {
        BcrSection section = findSection(CLIENT_TITLE);
        return section.get(ROUTE_NAME);
    }

    public void setName(String name) {
        BcrSection section = findSection(CLIENT_TITLE);
        section.put(ROUTE_NAME, name);
    }

    private int getDescriptionCount() {
        BcrSection client = findSection(CLIENT_TITLE);
        String countStr = client.get(DESCRIPTION_LINE_COUNT);
        if (countStr == null)
            return 0;
        return Integer.parseInt(countStr);
    }

    public List<String> getDescription() {
        List<String> descriptions = new ArrayList<String>();
        BcrSection client = findSection(CLIENT_TITLE);
        int count = getDescriptionCount();
        for (int i = 0; i < count; i++) {
            descriptions.add(client.get(DESCRIPTION + (i + 1)));
        }
        return descriptions;
    }

    private void setDescription(List<String> description) {
        BcrSection client = findSection(CLIENT_TITLE);
        client.remove(DESCRIPTION_LINE_COUNT);

        Set<String> removeNames = new HashSet<String>();
        for (String name : client.keySet()) {
            if (name.startsWith(DESCRIPTION))
                removeNames.add(name);
        }
        for (String name : removeNames) {
            client.remove(name);
        }

        if (description != null) {
            client.put(DESCRIPTION_LINE_COUNT, Integer.toString(description.size()));
            for (int i = 0; i < description.size(); i++) {
                client.put(DESCRIPTION + (i + 1), description.get(i));
            }
        }
    }

    public List<BcrPosition> getPositions() {
        return positions;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, BcrPosition position) {
        positions.add(index, position);
    }


    public BcrPosition createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        return new BcrPosition(longitude, latitude, elevation, speed, time, comment);
    }

    private BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<BcrPosition>(getPositions());
        return new BcrRoute(format, getName(), getDescription(), bcrPositions);
    }

    public BcrRoute asMTP0607Format() {
        if (getFormat() instanceof MTP0607Format)
            return this;
        return asBcrFormat(new MTP0607Format());
    }

    public BcrRoute asMTP0809Format() {
        if (getFormat() instanceof MTP0809Format)
            return this;
        return asBcrFormat(new MTP0809Format());
    }

    private TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format) {
        List<TomTomPosition> tomTomPositions = new ArrayList<TomTomPosition>();
        for (BcrPosition bcrPosition : positions) {
            TomTomPosition tomTomPosition = bcrPosition.asTomTomRoutePosition();
            // shortens comment to better fit to Tom Tom Rider display
            String city = bcrPosition.getCity();
            String street = bcrPosition.getStreet();
            if (city != null)
                tomTomPosition.setComment(city + (street != null && !BcrPosition.STREET_DEFINES_CENTER_SYMBOL.equals(street) ? "," + street : ""));
            tomTomPositions.add(tomTomPosition);
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
        for (BcrPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new KlickTelRoute(getName(), wgs84Positions);
    }

    protected KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<KmlPosition>();
        for (BcrPosition bcrPosition : positions) {
            kmlPositions.add(bcrPosition.asKmlPosition());
        }
        return new KmlRoute(format, getCharacteristics(), getName(), getDescription(), kmlPositions);
    }

    public GarminFlightPlanRoute asGarminFlightPlanFormat() {
        List<GarminFlightPlanPosition> flightPlanPositions = new ArrayList<GarminFlightPlanPosition>();
        for (BcrPosition position : positions) {
            flightPlanPositions.add(position.asGarminFlightPlanPosition());
        }
        return new GarminFlightPlanRoute(getName(), getDescription(), flightPlanPositions);
    }

    private GpxRoute asGpxFormat(GpxFormat format) {
        List<GpxPosition> gpxPositions = new ArrayList<GpxPosition>();
        for (BcrPosition bcrPosition : positions) {
            gpxPositions.add(bcrPosition.asGpxPosition());
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

    public MagicMapsIktRoute asMagicMapsIktFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (BcrPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new MagicMapsIktRoute(getName(), getDescription(), wgs84Positions);
    }

    public MagicMapsPthRoute asMagicMapsPthFormat() {
        List<GkPosition> gkPositions = new ArrayList<GkPosition>();
        for (BcrPosition position : positions) {
            gkPositions.add(position.asGkPosition());
        }
        return new MagicMapsPthRoute(getCharacteristics(), gkPositions);
    }


    private NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> nmeaPositions = new ArrayList<NmeaPosition>();
        for (BcrPosition position : positions) {
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
        for (BcrPosition bcrPosition : positions) {
            nmnPositions.add(bcrPosition.asNmnPosition());
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
        for (BcrPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new OvlRoute(getCharacteristics(), getName(), wgs84Positions);
    }

    protected SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (BcrPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), wgs84Positions);
    }

    public GoPal3Route asGoPal3RouteFormat() {
        List<GoPalPosition> gopalPositions = new ArrayList<GoPalPosition>();
        for (BcrPosition position : positions) {
            gopalPositions.add(position.asGoPalRoutePosition());
        }
        return new GoPal3Route(getName(), gopalPositions);
    }

    public GoPal5Route asGoPal5RouteFormat() {
        List<GoPalPosition> gopalPositions = new ArrayList<GoPalPosition>();
        for (BcrPosition position : positions) {
            gopalPositions.add(position.asGoPalRoutePosition());
        }
        return new GoPal5Route(getName(), gopalPositions);
    }

    public TourRoute asTourFormat() {
        List<TourPosition> tourPositions = new ArrayList<TourPosition>();
        for (BcrPosition position : positions) {
            tourPositions.add(position.asTourPosition());
        }
        return new TourRoute(getName(), tourPositions);
    }

    public ViaMichelinRoute asViaMichelinFormat() {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (BcrPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new ViaMichelinRoute(getName(), wgs84Positions);
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BcrRoute route = (BcrRoute) o;

        return positions.equals(route.positions) && sections.equals(route.sections);
    }

    public int hashCode() {
        int result;
        result = sections.hashCode();
        result = 31 * result + positions.hashCode();
        return result;
    }
}
