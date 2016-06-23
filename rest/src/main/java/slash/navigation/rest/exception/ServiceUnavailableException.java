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
package slash.navigation.rest.exception;

import java.io.IOException;

/**
 * A RouteConverter service cannot be accessed.
 *
 * @author Christian Pesch
 */

public class ServiceUnavailableException extends IOException {
    private String serviceName, serviceUrl;

    public ServiceUnavailableException(String serviceName, String serviceUrl, String result) {
        super("Service " + serviceName + " is unavailable, overloaded or beyond usage quota\n" +
                "URL: " + serviceUrl + "\n" +
                "Result: " + result);
        this.serviceName = serviceName;
        this.serviceUrl = serviceUrl;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public String toString() {
        return super.toString() + "[serviceName=" + getServiceName() + ", serviceUrl=" + getServiceUrl() + "]";
    }
}
