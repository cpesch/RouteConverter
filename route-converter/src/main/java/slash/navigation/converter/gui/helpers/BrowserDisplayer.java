/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Portions Copyrighted 2015 Christian Pesch
 */

package slash.navigation.converter.gui.helpers;

import com.sun.java.help.impl.ViewAwareComponent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.awt.Color.*;
import static java.awt.Font.*;
import static javax.swing.text.StyleConstants.*;

/**
 * This class is a lightweight component to be included in HTML content within
 * JHContentViewer. It invokes default IDE html browser to show external URL.
 * (Default browser should be external browser to show external URL properly.
 * Component is displayed as a mouse enabled Label. Only text is supported.
 *
 * To use this class within HTML content use the &ltobject&gt tag. Below is an
 * example usage:
 * <p><pre>
 * &ltobject CLASSID="java:slash.navigation.converter.gui.helpers.BrowserDisplayer"&gt
 * &ltparam name="content" value="http://www.netbeans.org"&gt
 * &ltparam name="text" value="Click here"&gt
 * &ltparam name="textFontFamily" value="SansSerif"&gt
 * &ltparam name="textFontSize" value="x-large"&gt
 * &ltparam name="textFontWeight" value="plain"&gt
 * &ltparam name="textFontStyle" value="italic"&gt
 * &ltparam name="textColor" value="red"&gt
 * &lt/object&gt
 * </pre><p>
 * Valid parameters are:
 * <ul>
 * <li>content - a valid external url like http://java.sun.com
 * <li>text - the text of the activator
 * <li>textFontFamily - the font family of the activator text
 * <li>textFontSize - the size of the activator text font. Size is specified
 * in a css terminology. See the setTextFontSize for acceptable syntax.
 * <li>textFontWeight - the activator text font weight
 * <li>textFontStyle - the activator text font style
 * <li>textColor - the activator text color
 * <ul>
 *
 * @author Marek Slama, modified by Christian Pesch
 */

@SuppressWarnings("unused")
public class BrowserDisplayer extends JButton implements ActionListener, ViewAwareComponent {
    private final static Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    private SimpleAttributeSet textAttribs;
    private HTMLDocument doc;
    private Cursor origCursor;

    private String content = "";

    public BrowserDisplayer() {
        setMargin(new Insets(0, 0, 0, 0));
        createLinkLabel();
        addActionListener(this);
        origCursor = getCursor();
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                setCursor(handCursor);
                setToolTipText(getContent());
            }

            public void mouseExited(MouseEvent e) {
                setCursor(origCursor);
                setToolTipText(null);
            }
        });
    }

    public void setViewData(View v) {
        doc = (HTMLDocument) v.getDocument();

        Font font = getFont();
        textAttribs = new SimpleAttributeSet();
        textAttribs.removeAttribute(FontSize);
        textAttribs.removeAttribute(Bold);
        textAttribs.removeAttribute(Italic);
        textAttribs.addAttribute(FontFamily, font.getName());
        textAttribs.addAttribute(FontSize, font.getSize());
        textAttribs.addAttribute(Bold, font.isBold());
        textAttribs.addAttribute(Italic, font.isItalic());
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    private void createLinkLabel() {
        setBorder(new EmptyBorder(1, 1, 1, 1));
        setBorderPainted(false);
        setFocusPainted(false);
        setAlignmentY(getPreferredAlignmentY());
        setContentAreaFilled(false);
        setHorizontalAlignment(LEFT);
        setBackground(UIManager.getColor("EditorPane.background"));
        if (textAttribs != null && textAttribs.isDefined(Foreground)) {
            setForeground((Color) textAttribs.getAttribute(Foreground));
        } else {
            setForeground(blue);
        }
        invalidate();
    }

    private float getPreferredAlignmentY() {
        Font font = getFont();
        // deprecated: FontMetrics fm = getToolkit().getFontMetrics(font);
        FontMetrics fm = new Canvas().getFontMetrics(font);
        float h = fm.getHeight();
        float d = fm.getDescent();
        return (h - d) / h;
    }

    public void setTextFontFamily(String family) {
        textAttribs.removeAttribute(FontFamily);
        textAttribs.addAttribute(FontFamily, family);
        setFont(getAttributeSetFont(textAttribs));
        Font font = getFont();
    }

    public String getTextFontFamily() {
        return StyleConstants.getFontFamily(textAttribs);
    }

    public void setTextFontSize(String size) {
        int newsize;
        StyleSheet css = doc.getStyleSheet();
        try {
            if (size.equals("xx-small")) {
                newsize = (int) css.getPointSize(0);
            } else if (size.equals("x-small")) {
                newsize = (int) css.getPointSize(1);
            } else if (size.equals("small")) {
                newsize = (int) css.getPointSize(2);
            } else if (size.equals("medium")) {
                newsize = (int) css.getPointSize(3);
            } else if (size.equals("large")) {
                newsize = (int) css.getPointSize(4);
            } else if (size.equals("x-large")) {
                newsize = (int) css.getPointSize(5);
            } else if (size.equals("xx-large")) {
                newsize = (int) css.getPointSize(6);
            } else if (size.equals("bigger")) {
                newsize = (int) css.getPointSize("+1");
            } else if (size.equals("smaller")) {
                newsize = (int) css.getPointSize("-1");
            } else if (size.endsWith("pt")) {
                String sz = size.substring(0, size.length() - 2);
                newsize = Integer.parseInt(sz);
            } else {
                newsize = (int) css.getPointSize(size);
            }
        } catch (NumberFormatException nfe) {
            return;
        }
        if (newsize == 0) {
            return;
        }
        textAttribs.removeAttribute(FontSize);
        textAttribs.addAttribute(FontSize, newsize);
        setFont(getAttributeSetFont(textAttribs));
        Font font = getFont();
    }

    public String getTextFontSize() {
        return Integer.toString(getFontSize(textAttribs));
    }

    public void setTextFontWeight(String weight) {
        boolean isBold = "bold".equals(weight);
        textAttribs.removeAttribute(Bold);
        textAttribs.addAttribute(Bold, isBold);
        setFont(getAttributeSetFont(textAttribs));
        Font font = getFont();
    }

    public String getTextFontWeight() {
        if (isBold(textAttribs)) {
            return "bold";
        }
        return "plain";
    }

    public void setTextFontStyle(String style) {
        boolean isItalic = "italic".equals(style);
        textAttribs.removeAttribute(Italic);
        textAttribs.addAttribute(Italic, isItalic);
        setFont(getAttributeSetFont(textAttribs));
        Font font = getFont();
    }

    public String getTextFontStyle() {
        if (isItalic(textAttribs)) {
            return "italic";
        }
        return "plain";
    }

    public void setTextColor(String name) {
        Color color = null;
        if ("black".equals(name)) {
            color = black;
        } else if ("blue".equals(name)) {
            color = blue;
        } else if ("cyan".equals(name)) {
            color = cyan;
        } else if ("darkGray".equals(name)) {
            color = darkGray;
        } else if ("gray".equals(name)) {
            color = gray;
        } else if ("green".equals(name)) {
            color = green;
        } else if ("lightGray".equals(name)) {
            color = lightGray;
        } else if ("magenta".equals(name)) {
            color = magenta;
        } else if ("orange".equals(name)) {
            color = orange;
        } else if ("pink".equals(name)) {
            color = pink;
        } else if ("red".equals(name)) {
            color = red;
        } else if ("white".equals(name)) {
            color = white;
        } else if ("yellow".equals(name)) {
            color = yellow;
        }

        if (color == null) {
            return;
        }
        textAttribs.removeAttribute(Foreground);
        textAttribs.addAttribute(Foreground, color);
        setForeground(color);
    }

    public String getTextColor() {
        Color color = getForeground();
        return color.toString();
    }

    private Font getAttributeSetFont(AttributeSet attr) {
        int style = PLAIN;
        if (isBold(attr)) {
            style |= BOLD;
        }
        if (isItalic(attr)) {
            style |= ITALIC;
        }
        String family = StyleConstants.getFontFamily(attr);
        int size = getFontSize(attr);

        if (isSuperscript(attr) || isSubscript(attr)) {
            size -= 2;
        }

        return doc.getStyleSheet().getFont(family, style, size);
    }

    public void actionPerformed(ActionEvent e) {
        ExternalPrograms.startBrowser(null, content);
    }
}