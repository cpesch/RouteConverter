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

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Hand-trimmed mapping of the Google Directions API XML response --
 * only the fields RouteConverter actually reads (status, and each leg's
 * distance/duration/step start+end locations); unmapped elements
 * (copyrights, polylines, html_instructions, bounds, ...) are ignored
 * by the schema-validation-free unmarshaller.
 *
 * @author Christian Pesch
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "status",
    "route"
})
@XmlRootElement(name = "DirectionsResponse")
public class DirectionsResponse {

    @XmlElement(required = true)
    protected String status;
    protected List<DirectionsResponse.Route> route;

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    public List<DirectionsResponse.Route> getRoute() {
        if (route == null) {
            route = new ArrayList<>();
        }
        return this.route;
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "leg"
    })
    public static class Route {

        protected List<DirectionsResponse.Route.Leg> leg;

        public List<DirectionsResponse.Route.Leg> getLeg() {
            if (leg == null) {
                leg = new ArrayList<>();
            }
            return this.leg;
        }


        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "distance",
            "duration",
            "step"
        })
        public static class Leg {

            protected DirectionsResponse.Route.Leg.Distance distance;
            protected DirectionsResponse.Route.Leg.Duration duration;
            protected List<DirectionsResponse.Route.Leg.Step> step;

            public DirectionsResponse.Route.Leg.Distance getDistance() {
                return distance;
            }

            public void setDistance(DirectionsResponse.Route.Leg.Distance value) {
                this.distance = value;
            }

            public DirectionsResponse.Route.Leg.Duration getDuration() {
                return duration;
            }

            public void setDuration(DirectionsResponse.Route.Leg.Duration value) {
                this.duration = value;
            }

            public List<DirectionsResponse.Route.Leg.Step> getStep() {
                if (step == null) {
                    step = new ArrayList<>();
                }
                return this.step;
            }


            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "value",
                "text"
            })
            public static class Distance {
                protected BigDecimal value;
                protected String text;

                public BigDecimal getValue() {
                    return value;
                }

                public void setValue(BigDecimal value) {
                    this.value = value;
                }

                public String getText() {
                    return text;
                }

                public void setText(String text) {
                    this.text = text;
                }
            }


            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "value",
                "text"
            })
            public static class Duration {
                protected BigDecimal value;
                protected String text;

                public BigDecimal getValue() {
                    return value;
                }

                public void setValue(BigDecimal value) {
                    this.value = value;
                }

                public String getText() {
                    return text;
                }

                public void setText(String text) {
                    this.text = text;
                }
            }


            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "startLocation",
                "endLocation"
            })
            public static class Step {

                @XmlElement(name = "start_location", required = true)
                protected DirectionsResponse.Route.Leg.Step.Location startLocation;
                @XmlElement(name = "end_location", required = true)
                protected DirectionsResponse.Route.Leg.Step.Location endLocation;

                public DirectionsResponse.Route.Leg.Step.Location getStartLocation() {
                    return startLocation;
                }

                public void setStartLocation(DirectionsResponse.Route.Leg.Step.Location value) {
                    this.startLocation = value;
                }

                public DirectionsResponse.Route.Leg.Step.Location getEndLocation() {
                    return endLocation;
                }

                public void setEndLocation(DirectionsResponse.Route.Leg.Step.Location value) {
                    this.endLocation = value;
                }


                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "lat",
                    "lng"
                })
                public static class Location {
                    @XmlElement(required = true)
                    protected BigDecimal lat;
                    @XmlElement(required = true)
                    protected BigDecimal lng;

                    public BigDecimal getLat() {
                        return lat;
                    }

                    public void setLat(BigDecimal value) {
                        this.lat = value;
                    }

                    public BigDecimal getLng() {
                        return lng;
                    }

                    public void setLng(BigDecimal value) {
                        this.lng = value;
                    }
                }
            }
        }
    }
}
