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

package slash.navigation.converter.gui.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import slash.navigation.base.WaypointType;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.renderer.CountryCodeListCellRenderer;
import slash.navigation.converter.gui.renderer.WaypointTypeListCellRenderer;
import slash.navigation.fpl.CountryCode;
import slash.navigation.fpl.GarminFlightPlanPosition;
import slash.navigation.fpl.GarminFlightPlanRoute;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

import static java.awt.Color.RED;
import static java.awt.event.ItemEvent.SELECTED;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.text.MessageFormat.format;
import static javax.swing.BorderFactory.createLineBorder;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.navigation.base.WaypointType.*;
import static slash.navigation.fpl.GarminFlightPlanFormat.*;

/**
 * Dialog for completing information for a Garmin Flight Plan.
 *
 * @author Christian Pesch
 */

public class CompleteFlightPlanDialog extends SimpleDialog {
    private static final Border INVALID_BORDER = createLineBorder(RED, 2);
    private static final Border VALID_BORDER = new JComboBox<>().getBorder();
    private JPanel contentPane;
    private JLabel labelPosition;
    private JTextField textFieldDescription;
    private JComboBox<CountryCode> comboBoxCountryCode;
    private JTextField textFieldIdentifier;
    private JComboBox<WaypointType> comboBoxWaypointType;
    private JButton buttonPrevious;
    private JButton buttonNextOrFinish;

    private GarminFlightPlanRoute route;
    private int index;

    public CompleteFlightPlanDialog(GarminFlightPlanRoute routeToComplete) {
        super(RouteConverter.getInstance().getFrame(), "complete-flightplan");
        this.route = routeToComplete;
        setTitle(RouteConverter.getBundle().getString("complete-flight-plan-title"));
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonNextOrFinish);

        buttonPrevious.addActionListener(new DialogAction(this) {
            public void run() {
                updateModel();

                if (index > 0) {
                    index--;
                    updateView();
                }
            }
        });

        buttonNextOrFinish.addActionListener(new DialogAction(this) {
            public void run() {
                updateModel();

                if (index < route.getPositionCount() - 1) {
                    index++;
                    updateView();
                } else
                    close();
            }
        });

        textFieldDescription.addKeyListener(new KeyAdapter() {
            protected void update() {
                getPosition().setDescription(textFieldDescription.getText());
                validateModel();
            }
        });
        textFieldIdentifier.addKeyListener(new KeyAdapter() {
            protected void update() {
                getPosition().setIdentifier(textFieldIdentifier.getText());
                validateModel();
            }
        });

        comboBoxCountryCode.setRenderer(new CountryCodeListCellRenderer());
        comboBoxCountryCode.setModel(new DefaultComboBoxModel<>(CountryCode.values()));
        comboBoxCountryCode.addItemListener(e -> {
            if (e.getStateChange() != SELECTED)
                return;
            CountryCode countryCode = (CountryCode) e.getItem();
            getPosition().setCountryCode(countryCode);
            validateModel();
        });
        comboBoxWaypointType.setRenderer(new WaypointTypeListCellRenderer());
        comboBoxWaypointType.setModel(new DefaultComboBoxModel<>(new WaypointType[]{
                Airport, Intersection, NonDirectionalBeacon, UserWaypoint, VHFOmnidirectionalRadioRange
        }));
        comboBoxWaypointType.addItemListener(e -> {
            if (e.getStateChange() != SELECTED)
                return;
            WaypointType waypointType = (WaypointType) e.getItem();
            getPosition().setWaypointType(waypointType);
            if (waypointType.equals(UserWaypoint))
                getPosition().setCountryCode(null);
            validateModel();
        });

        updateView();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        contentPane.registerKeyboardAction(new DialogAction(this) {
            public void run() {
                close();
            }
        }, getKeyStroke(VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private GarminFlightPlanPosition getPosition() {
        return route.getPosition(index);
    }

    private void updateModel() {
        getPosition().setIdentifier(textFieldIdentifier.getText());
        getPosition().setDescription(textFieldDescription.getText());
    }

    private void updateView() {
        labelPosition.setText(format(RouteConverter.getBundle().getString("position-index"), index + 1, route.getPositionCount()));
        GarminFlightPlanPosition position = getPosition();

        textFieldIdentifier.setText(createValidIdentifier(position, route.getPositions()));
        textFieldDescription.setText(createValidDescription(position.getDescription()));

        comboBoxCountryCode.setSelectedItem(createValidCountryCode(position));
        comboBoxWaypointType.setSelectedItem(createValidWaypointType(position));
        validateModel();
    }

    private void validateModel() {
        boolean validIdentifier = hasValidIdentifier(textFieldIdentifier.getText());
        textFieldIdentifier.setBorder(validIdentifier ? VALID_BORDER : INVALID_BORDER);
        boolean validDescription = hasValidDescription(textFieldDescription.getText());
        textFieldDescription.setBorder(validDescription ? VALID_BORDER : INVALID_BORDER);

        boolean validWaypointType = comboBoxWaypointType.getSelectedItem() != null;
        comboBoxWaypointType.setBorder(validWaypointType ? VALID_BORDER : INVALID_BORDER);
        boolean validCountryCode = Airport.equals(comboBoxWaypointType.getSelectedItem()) ?
                !CountryCode.None.equals(comboBoxCountryCode.getSelectedItem()) :
                UserWaypoint.equals(comboBoxWaypointType.getSelectedItem()) ?
                        CountryCode.None.equals(comboBoxCountryCode.getSelectedItem()) :
                        !CountryCode.None.equals(comboBoxCountryCode.getSelectedItem());
        comboBoxCountryCode.setBorder(validCountryCode ? VALID_BORDER : INVALID_BORDER);

        buttonPrevious.setEnabled(index > 0);
        buttonNextOrFinish.setEnabled(index < route.getPositionCount() && validCountryCode && validIdentifier &&
                validDescription && validWaypointType);
        $$$loadButtonText$$$(buttonNextOrFinish, index == route.getPositionCount() - 1 ?
                RouteConverter.getBundle().getString("finish") : RouteConverter.getBundle().getString("next"));
    }

    private void close() {
        dispose();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(5, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonNextOrFinish = new JButton();
        this.$$$loadButtonText$$$(buttonNextOrFinish, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "next"));
        panel2.add(buttonNextOrFinish, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonPrevious = new JButton();
        this.$$$loadButtonText$$$(buttonPrevious, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "previous"));
        panel1.add(buttonPrevious, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "position-colon"));
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelPosition = new JLabel();
        panel3.add(labelPosition, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "country-code-colon"));
        panel3.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxCountryCode = new JComboBox();
        panel3.add(comboBoxCountryCode, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxWaypointType = new JComboBox();
        panel3.add(comboBoxWaypointType, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "waypoint-type-colon"));
        panel3.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "identifier-colon"));
        panel3.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldIdentifier = new JTextField();
        panel3.add(textFieldIdentifier, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "comment-colon"));
        panel3.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldDescription = new JTextField();
        panel3.add(textFieldDescription, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "complete-flight-plan-description"));
        contentPane.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 5), null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel5, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    private abstract class KeyAdapter implements KeyListener {
        protected abstract void update();

        public void keyTyped(KeyEvent e) {
            update();
        }

        public void keyPressed(KeyEvent e) {
            update();
        }

        public void keyReleased(KeyEvent e) {
            update();
        }
    }
}
