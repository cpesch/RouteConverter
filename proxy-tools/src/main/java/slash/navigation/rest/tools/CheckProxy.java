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

import com.github.markusbernhardt.proxy.ProxySearch;
import org.apache.commons.cli.*;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
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

/**
 * Checks for proxy settings.
 *
 * @author Christian Pesch
 */

public class CheckProxy {
    private static final Logger log = Logger.getLogger(CheckProxy.class.getName());
    private static final String USERNAME_ARGUMENT = "username";
    private static final String PASSWORD_ARGUMENT = "password";

    @SuppressWarnings("AccessStaticViaInstance")
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
        System.setProperty("java.net.useSystemProxies","true");

        CommandLine line = parseCommandLine(args);
        String userName = line.getOptionValue(USERNAME_ARGUMENT);
        String password = line.getOptionValue(PASSWORD_ARGUMENT);

        ProxySearch proxySearch = ProxySearch.getDefaultProxySearch();
        ProxySelector proxySelector = proxySearch.getProxySelector();
        ProxySelector.setDefault(proxySelector);

        ProxySelector selector = ProxySelector.getDefault();
        log.info(format("ProxySelector %s", selector));

        if(userName != null && password != null) {
            log.info(format("Using proxy authentication with user %s", userName));

            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestorType() == PROXY) {
                        return new PasswordAuthentication(userName, password.toCharArray());
                    } else {
                        return super.getPasswordAuthentication();
                    }
                }
            });
        }

        checkProxy("https://api.routeconverter.com/v1/mapservers/?format=xml");
        checkProxy("https://static.routeconverter.com/maps/world.map");
    }

    private void checkProxy(String url) throws Exception {
        log.info(format("Checking proxy for %s", url));

        URI uri = new URI(url);
        List<Proxy> proxies = findProxies(uri);
        for(Proxy proxy : proxies) {
            request(uri, proxy);
        }
    }

    private List<Proxy> findProxies(URI uri) {
        List<Proxy> proxyList = ProxySelector.getDefault().select(uri);
        for (Proxy proxy : proxyList) {
            log.info(format("%d. proxy %s", proxyList.indexOf(proxy), proxy));
        }
        return proxyList;
    }

    private void request(URI uri, Proxy proxy) throws IOException {
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        if (proxy != NO_PROXY && !proxy.type().equals(DIRECT)) {
            SocketAddress address = proxy.address();
            if (address instanceof InetSocketAddress) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
                requestConfigBuilder.setProxy(new HttpHost(inetSocketAddress.getHostName(), inetSocketAddress.getPort()));
            }
        }

        log.info(format("Request to %s with proxy %s", uri, proxy));
        RequestConfig requestConfig = requestConfigBuilder.build();

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setDefaultRequestConfig(requestConfig);
        HttpClientContext context = HttpClientContext.create();
        try {
            HttpResponse response = clientBuilder.build().execute(new HttpGet(uri), context);
            log.info(format("Response %s", response));
        } catch (SocketException e) {
            log.info(format("Failed to request %s with proxy %s: %s", uri, proxy, e));
        }
    }

    public static void main(String[] args) throws Exception {
        new CheckProxy().run(args);
        exit(0);
    }
}
