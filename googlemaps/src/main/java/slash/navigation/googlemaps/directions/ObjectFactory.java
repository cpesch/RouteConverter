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

package slash.navigation.googlemaps.directions;

import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * Factory methods for JAXB content in the slash.navigation.googlemaps.directions package.
 *
 * @author Christian Pesch
 */
@XmlRegistry
public class ObjectFactory {

    public ObjectFactory() {
    }

    public DirectionsResponse createDirectionsResponse() {
        return new DirectionsResponse();
    }

    public DirectionsResponse.Route createDirectionsResponseRoute() {
        return new DirectionsResponse.Route();
    }

    public DirectionsResponse.Route.Leg createDirectionsResponseRouteLeg() {
        return new DirectionsResponse.Route.Leg();
    }

    public DirectionsResponse.Route.Leg.Distance createDirectionsResponseRouteLegDistance() {
        return new DirectionsResponse.Route.Leg.Distance();
    }

    public DirectionsResponse.Route.Leg.Duration createDirectionsResponseRouteLegDuration() {
        return new DirectionsResponse.Route.Leg.Duration();
    }

    public DirectionsResponse.Route.Leg.Step createDirectionsResponseRouteLegStep() {
        return new DirectionsResponse.Route.Leg.Step();
    }

    public DirectionsResponse.Route.Leg.Step.Location createDirectionsResponseRouteLegStepLocation() {
        return new DirectionsResponse.Route.Leg.Step.Location();
    }
}
