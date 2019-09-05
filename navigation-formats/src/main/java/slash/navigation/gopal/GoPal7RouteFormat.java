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

import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;
import slash.navigation.gopal.binding5.ObjectFactory;
import slash.navigation.gopal.binding5.Tour;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static slash.navigation.common.NavigationConversion.formatPosition;
import static slash.navigation.common.NavigationConversion.formatSpeed;
import static slash.navigation.gopal.GoPalUtil.marshal5;
import static slash.navigation.gopal.GoPalUtil.unmarshal5;

/**
 * Reads and writes GoPal 7 Route (.xml) files.
 *
 * @author Christian Pesch
 */

public class GoPal7RouteFormat extends GoPalRouteFormat<GoPalRoute> {
    private static final String ROUTE_OPTIONS_SPEED_UNIT = "km_h";
    private static final String VERSION_PREFIX = "v7";

    private static final Map<String, Short> COUNTRY_TO_CODE = new HashMap<>();
    static {
        COUNTRY_TO_CODE.put("Deutschland", (short)49);
        COUNTRY_TO_CODE.put("Frankreich", (short)31);
        COUNTRY_TO_CODE.put("Polen", (short)48);
        COUNTRY_TO_CODE.put("Spanien", (short)34);
    }

    public String getName() {
        return "GoPal 7 Route (*" + getExtension() + ")";
    }

    protected String getVersion() {
        return VERSION_PREFIX;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> GoPalRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new GoPalRoute(this, name, (List<GoPalPosition>) positions);
    }

    private GoPalRoute process(Tour tour) {
        List<GoPalPosition> positions = new ArrayList<>();
        Tour.Start start = tour.getStart();
        if (start != null) {
            Short country = start.getCountry() != null ? start.getCountry().getCode() : null;
            if(country == null)
                return null;
            String state = start.getState() != null ? start.getState().getName() : null;
            String zip = start.getZip() != null ? start.getZip().getCode() : null;
            String city = start.getCity() != null ? start.getCity().getName() : null;
            String suburb = start.getCity() != null ? start.getCity().getSuburb() : null;
            String street = start.getStreet() != null ? start.getStreet().getName() : null;
            String sideStreet = start.getSideStreet() != null ? start.getSideStreet().getName() : null;
            Short houseNumber = start.getHouseNumber() != null && start.getHouseNumber().getValue() != null ?
                    start.getHouseNumber().getValue() : null;
            positions.add(new GoPalPosition(start.getCoordinates().getMercatorx(), start.getCoordinates().getMercatory(),
                    country, state, zip, city, suburb, street, sideStreet, houseNumber));
        }
        for (Tour.Destination destination : tour.getDestination()) {
            Short country = destination.getCountry() != null ? destination.getCountry().getCode() : null;
            if(country == null)
                return null;
            String state = destination.getState() != null ? destination.getState().getName() : null;
            String zip = destination.getZip() != null ? destination.getZip().getCode() : null;
            String city = destination.getCity() != null ? destination.getCity().getName() : null;
            String suburb = destination.getCity() != null ? destination.getCity().getSuburb() : null;
            String street = destination.getStreet() != null ? destination.getStreet().getName() : null;
            String sideStreet = destination.getSideStreet() != null ? destination.getSideStreet().getName() : null;
            Short houseNumber = destination.getHouseNumber() != null && destination.getHouseNumber().getValue() != null ?
                    destination.getHouseNumber().getValue() : null;
            positions.add(new GoPalPosition(destination.getCoordinates().getMercatorx(), destination.getCoordinates().getMercatory(),
                    country, state, zip, city, suburb, street, sideStreet, houseNumber));
        }
        return new GoPalRoute(this, null, tour.getRouteOptions(), positions);
    }

    public void read(InputStream source, ParserContext<GoPalRoute> context) throws IOException {
        Tour tour = unmarshal5(source);
        GoPalRoute process = process(tour);
        if (process != null)
            context.appendRoute(process);
    }

    private Tour.RouteOptions createRouteOptions(GoPalRoute route) {
        Tour.RouteOptions options = route.getOptions(Tour.RouteOptions.class);
        if (options == null) {
            ObjectFactory objectFactory = new ObjectFactory();
            options = objectFactory.createTourRouteOptions();

            Tour.RouteOptions.NaviMode naviMode = objectFactory.createTourRouteOptionsNaviMode();
            naviMode.setValue(preferences.get(VERSION_PREFIX + "naviMode", "motorbike")); // bicycle, car, motorbike, pedestrian
            options.setNaviMode(naviMode);
            Tour.RouteOptions.OptimizationMode optimizationMode = objectFactory.createTourRouteOptionsOptimizationMode();
            optimizationMode.setValue(preferences.get(VERSION_PREFIX + "optimizationMode", "short")); // fast, short
            options.setOptimizationMode(optimizationMode);
            Tour.RouteOptions.TTIBypass bypass = objectFactory.createTourRouteOptionsTTIBypass();
            bypass.setCalculation(preferences.get(VERSION_PREFIX + "ttiBypass", "automatic")); // automatic, disabled, manual
            bypass.setMode("avoid");
            options.setTTIBypass(bypass);

            Tour.RouteOptions.RoadUsageTypes usageTypes = objectFactory.createTourRouteOptionsRoadUsageTypes();
            Tour.RouteOptions.RoadUsageTypes.CarTrains carTrains = objectFactory.createTourRouteOptionsRoadUsageTypesCarTrains();
            carTrains.setMode(preferences.get(VERSION_PREFIX + "carTrains", "use")); // avoid, use, prohibit
            usageTypes.setCarTrains(carTrains);
            Tour.RouteOptions.RoadUsageTypes.Ferries ferries = objectFactory.createTourRouteOptionsRoadUsageTypesFerries();
            ferries.setMode(preferences.get(VERSION_PREFIX + "ferries", "avoid")); // avoid, use, prohibit
            usageTypes.setFerries(ferries);
            Tour.RouteOptions.RoadUsageTypes.IPDRoads ipdRoads = objectFactory.createTourRouteOptionsRoadUsageTypesIPDRoads();
            ipdRoads.setMode(preferences.get(VERSION_PREFIX + "ipdRoads", "use")); // avoid, use, prohibit
            usageTypes.setIPDRoads(ipdRoads);
            Tour.RouteOptions.RoadUsageTypes.MotorWays motorWays = objectFactory.createTourRouteOptionsRoadUsageTypesMotorWays();
            motorWays.setMode(preferences.get(VERSION_PREFIX + "motorWays", "avoid")); // avoid, use, prohibit
            usageTypes.setMotorWays(motorWays);
            Tour.RouteOptions.RoadUsageTypes.SeasonalRestrictedRoads seasonalRestrictedRoads = objectFactory.createTourRouteOptionsRoadUsageTypesSeasonalRestrictedRoads();
            seasonalRestrictedRoads.setMode(preferences.get(VERSION_PREFIX + "seasonalRestrictedRoads", "use")); // avoid, use, prohibit
            usageTypes.setSeasonalRestrictedRoads(seasonalRestrictedRoads);
            Tour.RouteOptions.RoadUsageTypes.SpecialChargeRoads specialChargeRoads = objectFactory.createTourRouteOptionsRoadUsageTypesSpecialChargeRoads();
            specialChargeRoads.setMode(preferences.get(VERSION_PREFIX + "specialChargeRoads", "avoid")); // avoid, use, prohibit
            usageTypes.setSpecialChargeRoads(specialChargeRoads);
            Tour.RouteOptions.RoadUsageTypes.TimeRestrictedRoads timeRestrictedRoads = objectFactory.createTourRouteOptionsRoadUsageTypesTimeRestrictedRoads();
            timeRestrictedRoads.setMode(preferences.get(VERSION_PREFIX + "timeRestrictedRoads", "use")); // avoid, use, prohibit
            usageTypes.setTimeRestrictedRoads(timeRestrictedRoads);
            Tour.RouteOptions.RoadUsageTypes.TollRoads tollRoads = objectFactory.createTourRouteOptionsRoadUsageTypesTollRoads();
            tollRoads.setMode(preferences.get(VERSION_PREFIX + "tollRoads", "avoid")); // avoid, use, prohibit
            usageTypes.setTollRoads(tollRoads);
            Tour.RouteOptions.RoadUsageTypes.TrafficFlowInfo trafficFlowInfo = objectFactory.createTourRouteOptionsRoadUsageTypesTrafficFlowInfo();
            trafficFlowInfo.setMode(preferences.get(VERSION_PREFIX + "trafficFlowInfo", "use")); // avoid, use, prohibit
            usageTypes.setTrafficFlowInfo(trafficFlowInfo);
            Tour.RouteOptions.RoadUsageTypes.Tunnels tunnels = objectFactory.createTourRouteOptionsRoadUsageTypesTunnels();
            tunnels.setMode(preferences.get(VERSION_PREFIX + "tunnels", "use")); // avoid, use, prohibit
            usageTypes.setTunnels(tunnels);
            Tour.RouteOptions.RoadUsageTypes.UnpavedRoads unpavedRoads = objectFactory.createTourRouteOptionsRoadUsageTypesUnpavedRoads();
            unpavedRoads.setMode(preferences.get(VERSION_PREFIX + "unpavedRoads", "avoid")); // avoid, use, prohibit
            usageTypes.setUnpavedRoads(unpavedRoads);
            options.setRoadUsageTypes(usageTypes);

            Tour.RouteOptions.TravelSpeeds travelSpeeds = objectFactory.createTourRouteOptionsTravelSpeeds();
            Tour.RouteOptions.TravelSpeeds.Bicycle bicycle = objectFactory.createTourRouteOptionsTravelSpeedsBicycle();
            bicycle.setSpeed(formatSpeed(preferences.getDouble(VERSION_PREFIX + "bicycleSpeed", 14.4))); // Km/h
            bicycle.setUnit(ROUTE_OPTIONS_SPEED_UNIT);
            travelSpeeds.setBicycle(bicycle);
            Tour.RouteOptions.TravelSpeeds.Pedestrian pedestrian = objectFactory.createTourRouteOptionsTravelSpeedsPedestrian();
            pedestrian.setSpeed(formatSpeed(preferences.getDouble(VERSION_PREFIX + "pedestrianSpeed", 3.6))); // Km/h
            pedestrian.setUnit(ROUTE_OPTIONS_SPEED_UNIT);
            travelSpeeds.setPedestrian(pedestrian);
            Tour.RouteOptions.TravelSpeeds.Vehicle vehicle = objectFactory.createTourRouteOptionsTravelSpeedsVehicle();
            vehicle.setSpeed(formatSpeed(preferences.getDouble(VERSION_PREFIX + "vehicleSpeed", 80.0))); // Km/h
            vehicle.setUnit(ROUTE_OPTIONS_SPEED_UNIT);
            Tour.RouteOptions.TravelSpeeds.Vehicle.MotorWay motorWay = objectFactory.createTourRouteOptionsTravelSpeedsVehicleMotorWay();
            motorWay.setSpeed(formatSpeed(preferences.getDouble(VERSION_PREFIX + "vehicleSpeedMotorWay", 120.0))); // Km/h
            motorWay.setUnit(ROUTE_OPTIONS_SPEED_UNIT);
            vehicle.setMotorWay(motorWay);
            Tour.RouteOptions.TravelSpeeds.Vehicle.PedestrianArea pedestrianArea = objectFactory.createTourRouteOptionsTravelSpeedsVehiclePedestrianArea();
            pedestrianArea.setSpeed(formatSpeed(preferences.getDouble(VERSION_PREFIX + "vehicleSpeedPedestrianArea", 7.2))); // Km/h
            pedestrianArea.setUnit(ROUTE_OPTIONS_SPEED_UNIT);
            vehicle.setPedestrianArea(pedestrianArea);
            travelSpeeds.setVehicle(vehicle);
            options.setTravelSpeeds(travelSpeeds);
        }
        return options;
    }

    private Short formatCountry(GoPalPosition position) {
        if(position.getCountry() != null)
            return position.getCountry();
        for(Map.Entry<String, Short> entry : COUNTRY_TO_CODE.entrySet()) {
            if(position.getCity().contains(entry.getKey()))
                return entry.getValue();
        }
        return (short)49;
    }

    private Tour.Start createStart(GoPalPosition position) {
        ObjectFactory objectFactory = new ObjectFactory();
        Tour.Start start = objectFactory.createTourStart();
        Tour.Start.Coordinates coordinates = objectFactory.createTourStartCoordinates();
        if (position.getX() != null)
            coordinates.setMercatorx(position.getX());
        if (position.getY() != null)
            coordinates.setMercatory(position.getY());
        if (position.getLongitude() != null)
            coordinates.setLongitude(formatPosition(position.getLongitude()));
        if (position.getLatitude() != null)
            coordinates.setLatitude(formatPosition(position.getLatitude()));
        start.setCoordinates(coordinates);

        Tour.Start.City city = objectFactory.createTourStartCity();
        city.setName(position.getCity());
        city.setSuburb(position.getSuburb());
        start.setCity(city);
        Tour.Start.Country country = objectFactory.createTourStartCountry();
        country.setCode(formatCountry(position));
        start.setCountry(country);
        Tour.Start.State state = objectFactory.createTourStartState();
        if (position.getState() != null)
            state.setName(position.getState());
        start.setState(state);
        Tour.Start.HouseNumber houseNumber = objectFactory.createTourStartHouseNumber();
        if (position.getHouseNumber() != null) {
            houseNumber.setValue(position.getHouseNumber());
            houseNumber.setType("middle_of_street");
        }
        start.setHouseNumber(houseNumber);
        Tour.Start.Street street = objectFactory.createTourStartStreet();
        street.setName(position.getStreet());
        if (position.getHouseNumber() != null)
            street.setHouseNumberAvailable("no");
        start.setStreet(street);
        Tour.Start.SideStreet sideStreet = objectFactory.createTourStartSideStreet();
        sideStreet.setName(position.getSideStreet());
        start.setSideStreet(sideStreet);
        Tour.Start.Zip zip = objectFactory.createTourStartZip();
        zip.setCode(position.getZipCode());
        start.setZip(zip);
        return start;
    }

    private Tour.Destination createDestination(GoPalPosition position) {
        ObjectFactory objectFactory = new ObjectFactory();
        Tour.Destination destination = objectFactory.createTourDestination();
        Tour.Destination.Coordinates coordinates = objectFactory.createTourDestinationCoordinates();
        if (position.getX() != null)
            coordinates.setMercatorx(position.getX());
        if (position.getY() != null)
            coordinates.setMercatory(position.getY());
        if (position.getLongitude() != null)
            coordinates.setLongitude(formatPosition(position.getLongitude()));
        if (position.getLatitude() != null)
            coordinates.setLatitude(formatPosition(position.getLatitude()));
        destination.setCoordinates(coordinates);

        Tour.Destination.City city = objectFactory.createTourDestinationCity();
        city.setName(position.getCity());
        city.setSuburb(position.getSuburb());
        destination.setCity(city);
        Tour.Destination.Country country = objectFactory.createTourDestinationCountry();
        country.setCode(formatCountry(position));
        destination.setCountry(country);
        Tour.Destination.State state = objectFactory.createTourDestinationState();
        if (position.getState() != null)
            state.setName(position.getState());
        destination.setState(state);
        Tour.Destination.HouseNumber houseNumber = objectFactory.createTourDestinationHouseNumber();
        if (position.getHouseNumber() != null) {
            houseNumber.setValue(position.getHouseNumber());
            houseNumber.setType("middle_of_street");
        }
        destination.setHouseNumber(houseNumber);
        Tour.Destination.Street street = objectFactory.createTourDestinationStreet();
        street.setName(position.getStreet());
        if (position.getHouseNumber() != null)
            street.setHouseNumberAvailable("no");
        destination.setStreet(street);
        Tour.Destination.SideStreet sideStreet = objectFactory.createTourDestinationSideStreet();
        sideStreet.setName(position.getSideStreet());
        destination.setSideStreet(sideStreet);
        Tour.Destination.Zip zip = objectFactory.createTourDestinationZip();
        zip.setCode(position.getZipCode());
        destination.setZip(zip);
        return destination;
    }

    private Tour createGoPal(GoPalRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        Tour tour = objectFactory.createTour();
        tour.setRouteOptions(createRouteOptions(route));

        for (int i = startIndex; i < endIndex; i++) {
            GoPalPosition position = route.getPosition(i);

            if (i == startIndex) {
                tour.setStart(createStart(position));
            } else {
                tour.getDestination().add(createDestination(position));
            }
        }
        return tour;
    }

    public void write(GoPalRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal5(createGoPal(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }
}
