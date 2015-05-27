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

package slash.navigation.converter.gui.mapview;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import slash.navigation.common.NavigationPosition;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.logging.Logger;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.currentTimeMillis;
import static javafx.application.Platform.isFxApplicationThread;
import static javafx.application.Platform.runLater;
import static javafx.concurrent.Worker.State;
import static javafx.concurrent.Worker.State.SUCCEEDED;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.io.Transfer.parseDouble;

/**
 * Implementation for a component that displays the positions of a position list on a map
 * using the JavaFX WebView.
 *
 * @author Christian Pesch
 */

public class JavaFXWebViewMapView extends BaseMapView {
    private static final Logger log = Logger.getLogger(JavaFXWebViewMapView.class.getName());

    private JFXPanel panel;
    private WebView webView;

    public Component getComponent() {
        return panel;
    }

    // initialization

    private WebView createWebView() {
        try {
            final WebView webView = new WebView();
            Group group = new Group();
            group.getChildren().add(webView);
            panel.setScene(new Scene(group));
            panel.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    Dimension size = panel.getSize();
                    webView.setMinSize(size.getWidth(), size.getHeight());
                }
            });
            return webView;
        } catch (Throwable t) {
            log.severe("Cannot create WebView: " + t);
            setInitializationCause(t);
            return null;
        }
    }

    private boolean loadWebPage() {
        try {
            final String url = prepareWebPage();
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

                log.info("Using JavaFX WebView to create map view");

                webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
                    private int startCount = 0;

                    public void changed(ObservableValue<? extends State> observableValue, State oldState, State newState) {
                        log.info("WebView changed observableValue " + observableValue + " oldState " + oldState + " newState " + newState + " thread " + Thread.currentThread());
                        if (newState == SUCCEEDED) {
                            invokeLater(new Runnable() {
                                public void run() {
                                    tryToInitialize(startCount++, currentTimeMillis());
                                }
                            });
                        }
                    }
                });

                if (!loadWebPage())
                    dispose();
            }
        });
    }

    protected boolean isMapInitialized() {
        String result = executeScriptWithResult("isInitialized();");
        return parseBoolean(result);
    }

    // bounds and center

    protected NavigationPosition getNorthEastBounds() {
        return extractLatLng("getNorthEastBounds();");
    }

    protected NavigationPosition getSouthWestBounds() {
        return extractLatLng("getSouthWestBounds();");
    }

    protected NavigationPosition getCurrentMapCenter() {
        return extractLatLng("getCenter();");
    }

    protected Integer getCurrentZoom() {
        Double zoom = parseDouble(executeScriptWithResult("getZoom();"));
        return zoom != null ? zoom.intValue() : null;
    }

    protected String getCallbacks() {
        return executeScriptWithResult("getCallbacks();");
    }

    // script execution

    protected void executeScript(final String script) {
        if (webView == null || script.length() == 0)
            return;

        if (!isFxApplicationThread()) {
            runLater(new Runnable() {
                public void run() {
                    webView.getEngine().executeScript(script);
                    logJavaScript(script, null);
                }
            });
        } else {
            webView.getEngine().executeScript(script);
            logJavaScript(script, null);
        }
    }

    private static final Object LOCK = new Object();

    protected synchronized String executeScriptWithResult(final String script) {
        if (script.length() == 0)
            return null;

        final boolean debug = preferences.getBoolean(DEBUG_PREFERENCE, false);
        final boolean pollingCallback = !script.contains("getCallbacks");
        final Object[] result = new Object[2];

        if (!isFxApplicationThread()) {
            result[1] = false;

            runLater(new Runnable() {
                public void run() {
                    Object r = webView.getEngine().executeScript(script);
                    if (debug && pollingCallback) {
                        log.info("After runLater, executeScript with result " + r);
                    }

                    synchronized (LOCK) {
                        result[0] = r;
                        result[1] = true;
                        LOCK.notifyAll();
                    }
                }
            });

            synchronized (LOCK) {
                while(result[1] == false) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        // intentionally left empty
                    }
                }
            }
        } else {
            result[0] = webView.getEngine().executeScript(script);
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
