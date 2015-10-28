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

import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.transform.Scale;
import javafx.scene.web.WebView;

import static java.lang.Math.min;
import static javafx.application.Platform.runLater;

/**
 * Implementation for a component that displays the positions of a position list on a map
 * using the JavaFX WebView.
 *
 * @author Christian Pesch
 */

@SuppressWarnings("unused")
public class JavaFX8WebViewMapView extends JavaFX7WebViewMapView {

    // print

    public boolean isSupportsPrinting() {
        return true;
    }

    public boolean isSupportsPrintingWithDirections() {
        return false;
    }

    public void print(final String title, boolean withDirections) {
        if(withDirections)
            throw new UnsupportedOperationException("Printing with directions not supported");

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
}
