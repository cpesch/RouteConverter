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

package slash.navigation.mapview.browser;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.transform.Scale;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import slash.navigation.common.NavigationPosition;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Math.min;
import static java.lang.System.currentTimeMillis;
import static javafx.application.Platform.isFxApplicationThread;
import static javafx.application.Platform.runLater;
import static javafx.application.Platform.setImplicitExit;
import static javafx.concurrent.Worker.State;
import static javafx.concurrent.Worker.State.SUCCEEDED;
import static slash.common.io.Transfer.parseDouble;
import static slash.navigation.rest.HttpRequest.USER_AGENT;

/**
 * Implementation for a component that displays the positions of a position list on a map
 * using the JavaFX WebView.
 *
 * @author Christian Pesch
 */

public class JavaFX8WebViewMapView extends BrowserMapView {
    private static final Logger log = Logger.getLogger(JavaFX8WebViewMapView.class.getName());

    private JFXPanel panel;
    private WebView webView;

    static {
        setImplicitExit(false);
    }

    public Component getComponent() {
        return panel;
    }

    protected JFXPanel getPanel() {
        return panel;
    }

    protected WebView getWebView() {
        return webView;
    }

    // initialization

    protected WebView createWebView() {
        try {
            final WebView webView = new WebView();
            double browserScaleFactor = getBrowserScaleFactor();
            if (browserScaleFactor != 1.0) {
                // allow to compile code with Java 7; with Java 8 this would simply be
                // webView.setZoom(browserScaleFactor);
                try {
                    Method method = WebView.class.getDeclaredMethod("setZoom", double.class);
                    method.invoke(webView, browserScaleFactor);
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    // intentionally do nothing
                }
            }
            Group group = new Group();
            group.getChildren().add(webView);
            panel.setScene(new Scene(group));
            panel.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    setWebViewSizeToPanelSize();
                }
            });
            webView.getEngine().setCreatePopupHandler(new Callback<PopupFeatures, WebEngine>() {
                public WebEngine call(PopupFeatures config) {
                    // grab the last hyperlink that has :hover pseudoclass
                    String url = executeScriptWithResult("extractPopupHrefs()");
                    if (url != null && isUrl(url)) {
                        mapViewCallback.startBrowser(url);
                    } else {
                        log.warning("No result from popup uri detector");
                    }

                    // prevent from opening in WebView
                    return null;
                }

                private boolean isUrl(String url) {
                    try {
                        new URL(url);
                        return true;
                    } catch (MalformedURLException e) {
                        return false;
                    }
                }
            });
            webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
                private int startCount;

                public void changed(ObservableValue<? extends State> observableValue, State oldState, State newState) {
                    log.info("WebView changed observableValue " + observableValue + " oldState " + oldState + " newState " + newState + " thread " + Thread.currentThread());
                    if (newState == SUCCEEDED) {
                        // get out of the listener callback
                        new Thread(new Runnable() {
                            public void run() {
                                tryToInitialize(startCount++, currentTimeMillis());
                            }
                        }, "MapViewInitializer").start();
                    }
                }
            });

            // allow to compile code with Java 7; with Java 8 this would simply be
            // webView.getEngine().setUserAgent(USER_AGENT);
            try {
                Method method = WebEngine.class.getDeclaredMethod("setUserAgent", String.class);
                method.invoke(webView.getEngine(), USER_AGENT);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                // intentionally do nothing
            }
            return webView;
        } catch (Throwable t) {
            log.severe("Cannot create WebView: " + t);
            setInitializationCause(t);
            return null;
        }
    }

    /*
    protected void runBrowserInteractionCallbacksAndTests(long start) {
        executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}");
        super.runBrowserInteractionCallbacksAndTests(start);
    }
    */

    private boolean loadWebPage() {
        try {
            String url = prepareWebPage();
            webView.getEngine().load(url);
            return true;
        } catch (Throwable t) {
            log.severe("Cannot load web page: " + t);
            setInitializationCause(t);
            return false;
        }
    }

    protected void initializeBrowser() {
        panel = new JFXPanel();

        runLater(new Runnable() {
            public void run() {
                webView = createWebView();
                if (webView == null)
                    return;

                log.info("Using JavaFX WebView to create map view: " + JavaFX8WebViewMapView.this.getClass().getName());
                initializeWebPage();
            }
        });
    }

    protected void initializeWebPage() {
        log.info("Loading Google Maps API from " + getGoogleMapsServerApiUrl());
        runLater(new Runnable() {
            public void run() {
                if (!loadWebPage())
                    dispose();
            }
        });
    }

    protected boolean isMapInitialized() {
        String result = executeScriptWithResult("isInitialized();");
        return parseBoolean(result);
    }

    public void resize() {
        super.resize();
        setWebViewSizeToPanelSize();
    }

    private void setWebViewSizeToPanelSize() {
        Dimension size = panel.getSize();
        if (webView != null) {
            webView.setMinSize(size.getWidth(), size.getHeight());
            webView.setMaxSize(size.getWidth(), size.getHeight());
        }
    }

    // bounds and center

    protected NavigationPosition getNorthEastBounds() {
        return parsePosition("getNorthEastBounds();");
    }
    protected NavigationPosition getSouthWestBounds() {
        return parsePosition("getSouthWestBounds();");
    }

    protected NavigationPosition getCurrentMapCenter() {
        return parsePosition("getCenter();");
    }

    protected Integer getCurrentZoom() {
        try {
            Double zoom = parseDouble(executeScriptWithResult("getZoom();"));
            if (zoom != null)
                return zoom.intValue();
        } catch (NumberFormatException e) {
            // intentionally left empty
        }
        return null;
    }

    protected String getCallbacks() {
        return executeScriptWithResult("getCallbacks();");
    }

    // print

    public void print(final String title) {
        runLater(new Runnable() {
            public void run() {
                PrinterJob job = PrinterJob.createPrinterJob();
                if (job != null && job.showPrintDialog(null)) {
                    WebView webView = getWebView();
                    PageLayout pageLayout = job.getPrinter().getDefaultPageLayout();
                    double scaleX = pageLayout.getPrintableWidth() / webView.getBoundsInParent().getWidth();
                    double scaleY = pageLayout.getPrintableHeight() / webView.getBoundsInParent().getHeight();
                    double minimumScale = min(scaleX, scaleY);
                    Scale scale = new Scale(minimumScale, minimumScale);

                    try {
                        webView.getTransforms().add(scale);

                        boolean success = job.printPage(webView);
                        if (success)
                            job.endJob();

                    } finally {
                        webView.getTransforms().remove(scale);

                        Group group = new Group();
                        group.getChildren().add(webView);
                        getPanel().setScene(new Scene(group));
                    }
                }
            }
        });
    }

    // script execution

    protected void executeScript(final String script) {
        if (webView == null || script.length() == 0)
            return;

        boolean debug = preferences.getBoolean(DEBUG_PREFERENCE, false);
        if (debug)
            log.info("Before executeScript " + script);
        if (!isFxApplicationThread()) {
            runLater(new Runnable() {
                public void run() {
                    try {
                        webView.getEngine().executeScript(script);
                    } catch (Throwable t) {
                        log.info("Exception during runLater executeScript of " + script + ": " + t);
                    }

                    logJavaScript(script, null);
                }
            });
        } else {
            try {
                webView.getEngine().executeScript(script);
            } catch (Throwable t) {
                log.info("Exception during executeScript of " + script + ": " + t);
            }

            logJavaScript(script, null);
        }
    }

    private static final Object LOCK = new Object();

    protected synchronized String executeScriptWithResult(final String script) {
        if (script.length() == 0)
            return null;

        final boolean debug = preferences.getBoolean(DEBUG_PREFERENCE, false);
        final boolean pollingCallback = !script.contains("getCallbacks");
        final Object[] result = new Object[1];

        if (!isFxApplicationThread()) {
            final boolean[] haveResult = new boolean[]{false};

            runLater(new Runnable() {
                public void run() {
                    Object r = null;
                    try {
                        r = webView.getEngine().executeScript(script);
                    }
                    catch (Throwable t) {
                        log.info("Exception during runLater executeScript with result of " + script + ": " + t);
                    }

                    if (debug && pollingCallback) {
                        log.info("After runLater, executeScript with result " + r);
                    }

                    synchronized (LOCK) {
                        result[0] = r;
                        haveResult[0] = true;
                        LOCK.notifyAll();
                    }
                }
            });

            synchronized (LOCK) {
                while (!haveResult[0]) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        // intentionally left empty
                    }
                }
            }
        } else {
            try {
                result[0] = webView.getEngine().executeScript(script);
            }
            catch (Throwable t) {
                log.info("Exception during executeScript with result of " + script + ": " + t);
            }

            if (debug && pollingCallback) {
                log.info("After executeScript with result " + result[0]);
            }
        }

        if (pollingCallback) {
            logJavaScript(script, result[0]);
        }
        return result[0] != null ? result[0].toString() : null;
    }
}
