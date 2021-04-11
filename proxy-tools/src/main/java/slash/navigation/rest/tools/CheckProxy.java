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
package slash.navigation.rest.tools;

import org.apache.commons.cli.*;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.lang.System.exit;
import static java.net.Authenticator.RequestorType.PROXY;
import static java.net.Proxy.NO_PROXY;
import static java.net.Proxy.Type.DIRECT;
import static java.util.Collections.emptyList;
import static slash.common.helpers.ProxyHelper.setUseSystemProxies;

/**
 * Checks for proxy settings.
 *
 * @author Christian Pesch
 */

public class CheckProxy {
    private static final Logger log = Logger.getLogger(CheckProxy.class.getName());
    private static final String USERNAME_ARGUMENT = "username";
    private static final String PASSWORD_ARGUMENT = "password";

    private CommandLine parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption(Option.builder().argName(USERNAME_ARGUMENT).numberOfArgs(1).longOpt("username").
                desc("Username for the proxy authentication").build());
        options.addOption(Option.builder().argName(PASSWORD_ARGUMENT).numberOfArgs(1).longOpt("password").
                desc("Password for the proxy authentication").build());
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(getClass().getSimpleName(), options);
            throw e;
        }
    }

    private void run(String[] args) throws Exception {
        CommandLine line = parseCommandLine(args);
        String userName = line.getOptionValue(USERNAME_ARGUMENT);
        String password = line.getOptionValue(PASSWORD_ARGUMENT);

        setUseSystemProxies();

        ProxySelector selector = ProxySelector.getDefault();
        log.info(format("ProxySelector %s", selector));

        if(userName != null && password != null) {
            log.info(format("Using proxy authentication with user %s", userName));

            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    log.info("Authenticator#getPasswordAuthentication " + getRequestorType());
                    if (getRequestorType() == PROXY) {
                        return new PasswordAuthentication(userName, password.toCharArray());
                    } else {
                        return super.getPasswordAuthentication();
                    }
                }
            });
        }

        checkProxy("https://api.routeconverter.com/v1/mapservers/?format=xml", userName, password);
        checkProxy("https://static.routeconverter.com/maps/world.map", userName, password);
    }

    private void checkProxy(String url, String userName, String password) throws Exception {
        log.info(format("Checking proxy for %s", url));

        URI uri = new URI(url);
        List<Proxy> proxies = findProxies(uri);
        for(Proxy proxy : proxies) {
            apacheCommonsHttpRequest(uri, proxy, userName, password);
        }
        javaHttpClientRequest(uri);
    }

    private List<Proxy> findProxies(URI uri) {
        ProxySelector selector = ProxySelector.getDefault();
        if(selector == null) {
            log.info("Found no default proxy selector");
            return emptyList();
        }
        List<Proxy> proxyList = selector.select(uri);
        for (Proxy proxy : proxyList) {
            log.info(format("%d. proxy %s", proxyList.indexOf(proxy), proxy));
        }
        return proxyList;
    }

    private void apacheCommonsHttpRequest(URI uri, Proxy proxy, String userName, String password) throws IOException {
        CredentialsProvider credentialsProvider = null;

        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        if (proxy != NO_PROXY && !proxy.type().equals(DIRECT)) {

            SocketAddress address = proxy.address();
            log.info("SocketAddress " + address);
            if (address instanceof InetSocketAddress) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
                HttpHost host = new HttpHost(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
                requestConfigBuilder.setProxy(host);

                credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(new AuthScope(host), new UsernamePasswordCredentials(userName, password));
                log.info("CredentialsProvider#setCredentials " + host);
            }
        }

        log.info(format("Apache Commons HTTP request to %s with proxy %s", uri, proxy));
        RequestConfig requestConfig = requestConfigBuilder.build();

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setDefaultRequestConfig(requestConfig);
        if (credentialsProvider != null)
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);

        HttpClientContext context = HttpClientContext.create();
        try {
            HttpResponse response = clientBuilder.build().execute(new HttpGet(uri), context);
            log.info(format("Apache Commons HTTP response %s", response));
        } catch (SocketException e) {
            log.info(format("Failed to execute Apache Commons HTTP request %s with proxy %s: %s", uri, proxy, e));
        }
    }

    private void javaHttpClientRequest(URI uri) throws IOException {
        URL url = uri.toURL();

        log.info(format("Java HTTP Client request to %s", uri));
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int status = connection.getResponseCode();
            int length = connection.getContentLength();
            log.info(format("Java HTTP Client status %s length %s", status, length));

            connection.disconnect();
        } catch (Exception e) {
            log.info(format("Failed to execute Java HTTP Client request %s: %s", uri, e));
        }
    }

    public static void main(String[] args) throws Exception {
        new CheckProxy().run(args);
        exit(0);
    }
}
