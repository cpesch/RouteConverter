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
package slash.navigation.gui.helpers;

import com.formdev.flatlaf.FlatLightLaf;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link IconHelper}.
 *
 * The fixture images are not checked into the repository as binary files; this test
 * writes them onto the test classpath directory once before any test runs, so {@link
 * IconHelper#loadIcon} resolves them exactly as it would resolve a real icon resource.
 *
 * @author Christian Pesch
 */
public class IconHelperTest {
    private static final String PACKAGE_PATH = "/slash/navigation/gui/helpers/";
    private static final String WITH_RETINA = PACKAGE_PATH + "icon-helper-test-a.png";
    private static final String WITHOUT_RETINA = PACKAGE_PATH + "icon-helper-test-b.png";
    private static final String MISSING = PACKAGE_PATH + "icon-helper-test-does-not-exist.png";

    private static String previousLookAndFeel;

    @BeforeClass
    public static void generateFixturesAndInstallFlatLaf() throws IOException, UnsupportedLookAndFeelException {
        File directory = new File("target/test-classes" + PACKAGE_PATH);
        directory.mkdirs();
        writeImage(new File(directory, "icon-helper-test-a.png"), 16, Color.RED);
        writeImage(new File(directory, "icon-helper-test-a@2x.png"), 32, Color.RED);
        writeImage(new File(directory, "icon-helper-test-b.png"), 16, Color.GREEN);

        previousLookAndFeel = UIManager.getLookAndFeel().getClass().getName();
        UIManager.setLookAndFeel(new FlatLightLaf());
    }

    @AfterClass
    public static void restoreLookAndFeel() throws UnsupportedLookAndFeelException {
        if (previousLookAndFeel != null) {
            try {
                UIManager.setLookAndFeel(previousLookAndFeel);
            } catch (ReflectiveOperationException e) {
                throw new UnsupportedLookAndFeelException(previousLookAndFeel);
            }
        }
    }

    private static void writeImage(File file, int size, Color color) throws IOException {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, size, size);
        graphics.dispose();
        ImageIO.write(image, "png", file);
    }

    @Test
    public void loadsRetinaVariantAsMultiResolutionImage() {
        ImageIcon icon = IconHelper.loadIcon(WITH_RETINA);
        assertNotNull(icon);
        assertEquals(16, icon.getIconWidth());

        Image image = icon.getImage();
        assertTrue(image instanceof MultiResolutionImage);
        List<Image> variants = ((MultiResolutionImage) image).getResolutionVariants();
        assertEquals(2, variants.size());
        assertTrue(variants.stream().anyMatch(variant -> variant.getWidth(null) == 16));
        assertTrue(variants.stream().anyMatch(variant -> variant.getWidth(null) == 32));
    }

    @Test
    public void loadsIconWithoutRetinaVariantAsPlainImage() {
        ImageIcon icon = IconHelper.loadIcon(WITHOUT_RETINA);
        assertNotNull(icon);
        assertFalse(icon.getImage() instanceof MultiResolutionImage);
    }

    @Test
    public void loadsMissingPathAsNull() {
        assertNull(IconHelper.loadIcon(MISSING));
    }

    @Test
    public void cachesLoadedIcon() {
        ImageIcon first = IconHelper.loadIcon(WITH_RETINA);
        ImageIcon second = IconHelper.loadIcon(WITH_RETINA);
        assertSame(first, second);
    }

    @Test
    public void flatLafGreysDisabledMultiResolutionIcon() {
        ImageIcon icon = IconHelper.loadIcon(WITH_RETINA);
        Icon disabledIcon = UIManager.getLookAndFeel().getDisabledIcon(new JButton(), icon);
        assertNotNull(disabledIcon);
        assertTrue(((ImageIcon) disabledIcon).getImage() instanceof MultiResolutionImage);
    }
}
