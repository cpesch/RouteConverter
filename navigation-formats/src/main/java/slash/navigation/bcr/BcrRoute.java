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

import slash.common.io.CompactCalendar;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.GkPosition;
import slash.navigation.base.SimpleFormat;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
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
import slash.navigation.simple.KompassFormat;
import slash.navigation.simple.OpelNaviFormat;
import slash.navigation.simple.QstarzQ1000Format;
import slash.navigation.simple.Route66Format;
import slash.navigation.simple.SygicUnicodeFormat;
import slash.navigation.simple.WebPageFormat;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;
import slash.navigation.tour.TourPosition;
import slash.navigation.tour.TourRoute;
import slash.navigation.viamichelin.ViaMichelinRoute;
import slash.navigation.wbt.WintecWbt201Tk1Format;
import slash.navigation.wbt.WintecWbt201Tk2Format;
import slash.navigation.wbt.WintecWbt202TesFormat;

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

    private KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<KmlPosition>();
        for (BcrPosition bcrPosition : positions) {
            kmlPositions.add(bcrPosition.asKmlPosition());
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


    private GpxRoute asGpxFormat(GpxFormat format) {
        List<GpxPosition> gpxPositions = new ArrayList<GpxPosition>();
        for (BcrPosition bcrPosition : positions) {
            gpxPositions.add(bcrPosition.asGpxPosition());
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
        for (BcrPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new OvlRoute(getCharacteristics(), getName(), wgs84Positions);
    }

    public SimpleRoute asQstarzQ1000Format() {
        return asSimpleFormat(new QstarzQ1000Format());
    }

    private SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (BcrPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
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

    public SimpleRoute asCoPilot8Format() {
        return asSimpleFormat(new CoPilot8Format());
    }

    public SimpleRoute asCoPilot9Format() {
        return asSimpleFormat(new CoPilot9Format());
    }

    public SimpleRoute asGlopusFormat() {
        return asSimpleFormat(new GlopusFormat());
    }

    public SimpleRoute asGoogleMapsUrlFormat() {
        return asSimpleFormat(new GoogleMapsUrlFormat());
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

    public SimpleRoute asIblue747Format() {
        return asSimpleFormat(new Iblue747Format());
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
