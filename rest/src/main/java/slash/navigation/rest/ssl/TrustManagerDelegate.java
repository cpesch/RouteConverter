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
package slash.navigation.rest.ssl;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A {@link X509TrustManager}
 * <p>
 * Based on http://blog.novoj.net/2016/02/29/how-to-make-apache-httpclient-trust-lets-encrypt-certificate-authority/
 *
 * @author Otec Fura
 */

class TrustManagerDelegate implements X509TrustManager {
    private final X509TrustManager mainTrustManager;
    private final X509TrustManager fallbackTrustManager;

    public TrustManagerDelegate(X509TrustManager mainTrustManager, X509TrustManager fallbackTrustManager) {
        this.mainTrustManager = mainTrustManager;
        this.fallbackTrustManager = fallbackTrustManager;
    }

    public void checkClientTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
        try {
            mainTrustManager.checkClientTrusted(x509Certificates, authType);
        } catch(CertificateException ignored) {
            fallbackTrustManager.checkClientTrusted(x509Certificates, authType);
        }
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
        try {
            mainTrustManager.checkServerTrusted(x509Certificates, authType);
        } catch(CertificateException ignored) {
            fallbackTrustManager.checkServerTrusted(x509Certificates, authType);
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        return fallbackTrustManager.getAcceptedIssuers();
    }
}