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
import slash.navigation.converter.gui.helpers.CheckBoxPreferencesSynchronizer;
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import static java.awt.Color.RED;
import static java.awt.event.ItemEvent.SELECTED;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.text.MessageFormat.format;
import static javax.swing.BorderFactory.createLineBorder;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.navigation.base.WaypointType.Airport;
import static slash.navigation.base.WaypointType.Intersection;
import static slash.navigation.base.WaypointType.NonDirectionalBeacon;
import static slash.navigation.base.WaypointType.UserWaypoint;
import static slash.navigation.base.WaypointType.VHFOmnidirectionalRadioRange;
import static slash.navigation.fpl.CountryCode.None;
import static slash.navigation.fpl.GarminFlightPlanFormat.createValidDescription;
import static slash.navigation.fpl.GarminFlightPlanFormat.createValidIdentifier;
import static slash.navigation.fpl.GarminFlightPlanFormat.hasValidDescription;
import static slash.navigation.fpl.GarminFlightPlanFormat.hasValidIdentifier;

/**
 * Dialog for completing information for a Garmin Flight Plan.
 *
 * @author Christian Pesch
 */

public class CompleteFlightPlanDialog extends SimpleDialog {
    private static final Preferences preferences = Preferences.userNodeForPackage(CompleteFlightPlanDialog.class);
    private static final Border INVALID_BORDER = createLineBorder(RED, 2);
    private static final Border VALID_BORDER = new JComboBox().getBorder();
    private static final String PROPOSE_IDENTIFIER_AND_COMMENT_PREFERENCE = "proposeIdentifierAndComment";
    private JPanel contentPane;
    private JLabel labelPosition;
    private JTextField textFieldDescription;
    private JComboBox<CountryCode> comboBoxCountryCode;
    private JTextField textFieldIdentifier;
    private JComboBox<WaypointType> comboBoxWaypointType;
    private JButton buttonPrevious;
    private JButton buttonNextOrFinish;
    private JCheckBox checkBoxProposeIdentifierAndComment;

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
        comboBoxCountryCode.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED)
                    return;
                CountryCode countryCode = (CountryCode) e.getItem();
                getPosition().setCountryCode(countryCode);
                validateModel();
            }
        });
        comboBoxWaypointType.setRenderer(new WaypointTypeListCellRenderer());
        comboBoxWaypointType.setModel(new DefaultComboBoxModel<>(new WaypointType[]{
                Airport, Intersection, NonDirectionalBeacon, UserWaypoint, VHFOmnidirectionalRadioRange
        }));
        comboBoxWaypointType.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED)
                    return;
                WaypointType waypointType = (WaypointType) e.getItem();
                getPosition().setWaypointType(waypointType);
                validateModel();
            }
        });

        new CheckBoxPreferencesSynchronizer(checkBoxProposeIdentifierAndComment, preferences, PROPOSE_IDENTIFIER_AND_COMMENT_PREFERENCE, false);
        checkBoxProposeIdentifierAndComment.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateView();
            }
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
        boolean proposeIdentifierAndCommentSelected = checkBoxProposeIdentifierAndComment.isSelected();
        if (!proposeIdentifierAndCommentSelected)
            return;
        getPosition().setIdentifier(textFieldIdentifier.getText());
        getPosition().setDescription(textFieldDescription.getText());
    }

    private void updateView() {
        labelPosition.setText(format(RouteConverter.getBundle().getString("position-index"), index + 1, route.getPositionCount()));
        GarminFlightPlanPosition position = getPosition();

        boolean proposeIdentifierAndCommentSelected = checkBoxProposeIdentifierAndComment.isSelected();
        textFieldDescription.setText(proposeIdentifierAndCommentSelected ? createValidDescription(position.getDescription()) : position.getDescription());
        textFieldIdentifier.setText(proposeIdentifierAndCommentSelected ? createValidIdentifier(position, route.getPositions()) : position.getIdentifier());

        CountryCode countryCode = position.getCountryCode();
        comboBoxCountryCode.setSelectedItem(countryCode == null ? None : countryCode);
        comboBoxWaypointType.setSelectedItem(position.getWaypointType());
        validateModel();
    }

    private void validateModel() {
        boolean noCountryCode = getPosition().getCountryCode() == null || None.equals(getPosition().getCountryCode());
        boolean validCountryCode = UserWaypoint.equals(getPosition().getWaypointType()) == noCountryCode;
        comboBoxCountryCode.setBorder(validCountryCode ? VALID_BORDER : INVALID_BORDER);
        boolean modifiableCountryCode = !UserWaypoint.equals(getPosition().getWaypointType()) || !noCountryCode;
        comboBoxCountryCode.setEnabled(modifiableCountryCode);

        boolean proposeIdentifierAndCommentSelected = checkBoxProposeIdentifierAndComment.isSelected();
        boolean validIdentifier = hasValidIdentifier(proposeIdentifierAndCommentSelected ? textFieldIdentifier.getText() : getPosition().getIdentifier(), route.getPositions());
        textFieldIdentifier.setBorder(validIdentifier ? VALID_BORDER : INVALID_BORDER);
        boolean validDescription = hasValidDescription(proposeIdentifierAndCommentSelected ? textFieldDescription.getText() : getPosition().getDescription());
        textFieldDescription.setBorder(validDescription ? VALID_BORDER : INVALID_BORDER);

        boolean validWaypointType = getPosition().getWaypointType() != null;
        comboBoxWaypointType.setBorder(validWaypointType ? VALID_BORDER : INVALID_BORDER);

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
        this.$$$loadButtonText$$$(buttonNextOrFinish, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("next"));
        panel2.add(buttonNextOrFinish, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonPrevious = new JButton();
        this.$$$loadButtonText$$$(buttonPrevious, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("previous"));
        panel1.add(buttonPrevious, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("position-colon"));
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelPosition = new JLabel();
        panel3.add(labelPosition, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("country-code-colon"));
        panel3.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxCountryCode = new JComboBox();
        panel3.add(comboBoxCountryCode, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxWaypointType = new JComboBox();
        panel3.add(comboBoxWaypointType, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("waypoint-type-colon"));
        panel3.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("identifier-colon"));
        panel3.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldIdentifier = new JTextField();
        panel3.add(textFieldIdentifier, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("comment-colon"));
        panel3.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldDescription = new JTextField();
        panel3.add(textFieldDescription, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        checkBoxProposeIdentifierAndComment = new JCheckBox();
        this.$$$loadButtonText$$$(checkBoxProposeIdentifierAndComment, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("propose-identifier-and-comment"));
        panel3.add(checkBoxProposeIdentifierAndComment, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("complete-flight-plan-description"));
        contentPane.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 5), null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel5, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
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
