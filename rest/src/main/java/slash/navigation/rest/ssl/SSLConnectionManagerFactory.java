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

import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

import static slash.common.io.InputOutput.closeQuietly;

/**
 * A factory to create a {@link HttpClientConnectionManager} that supports the letsencrypt root certificate.
 * <p>
 * Based on http://blog.novoj.net/2016/02/29/how-to-make-apache-httpclient-trust-lets-encrypt-certificate-authority/
 *
 * @author Christian Pesch
 */

public class SSLConnectionManagerFactory {
    public HttpClientConnectionManager createConnectionManager() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        SSLContext sslContext = createSSLContext();
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);
        return new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", sslSocketFactory)
                        .build()
        );
    }

    private final SecureRandom secureRandom = new SecureRandom();

    private SSLContext createSSLContext() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, KeyManagementException, IOException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManagerFactory javaDefaultTrustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        javaDefaultTrustManager.init((KeyStore) null);
        TrustManagerFactory customCaTrustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        customCaTrustManager.init(getKeyStore());

        sslContext.init(null,
                new TrustManager[]{
                        new TrustManagerDelegate(
                                (X509TrustManager) customCaTrustManager.getTrustManagers()[0],
                                (X509TrustManager) javaDefaultTrustManager.getTrustManagers()[0]
                        )
                },
                secureRandom
        );
        return sslContext;
    }

    private KeyStore getKeyStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        InputStream inputStream = getClass().getResourceAsStream("letsencrypt.truststore");
        try {
            keyStore.load(inputStream, "letsencrypt".toCharArray());
        } finally {
            closeQuietly(inputStream);
        }
        return keyStore;
    }
}
