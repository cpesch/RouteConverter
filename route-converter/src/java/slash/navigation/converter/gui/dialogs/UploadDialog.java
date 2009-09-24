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
import slash.navigation.BaseRoute;
import slash.navigation.BaseNavigationPosition;
import slash.navigation.util.CompactCalendar;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helper.DialogAction;
import slash.navigation.converter.gui.models.FormatAndRoutesModel;
import slash.navigation.converter.gui.renderer.RouteServiceListCellRenderer;
import slash.navigation.converter.gui.services.CrossingWays;
import slash.navigation.converter.gui.services.RouteCatalog;
import slash.navigation.converter.gui.services.RouteService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Dialog to upload a file to a RouteService
 *
 * @author Christian Pesch
 */

public class UploadDialog extends JDialog {
    private final Preferences preferences = Preferences.userNodeForPackage(getClass());

    private static final DateFormat TIME_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

    private static final String REMEMBER_ME_PREFERENCE = "remember";
    private static final String USERNAME_PREFERENCE = "userName";
    private static final String PASSWORD_PREFERENCE = "userAuthentication";

    private JButton buttonUpload;
    private JButton buttonCancel;
    private JComboBox comboBoxChooseRouteService;
    private JTextField textFieldUserName;
    private JPasswordField textFieldPassword;
    private JCheckBox checkBoxRememberMe;
    private JTextField textFieldName;
    private JTextArea textAreaDescription;
    private JPanel contentPane;

    private String fileUrl;

    public UploadDialog(FormatAndRoutesModel formatAndRoutesModel, String fileUrl) {
        super(RouteConverter.getInstance().getFrame());
        this.fileUrl = fileUrl;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonUpload);

        List<RouteService> services = new ArrayList<RouteService>();
        services.add(new CrossingWays());
        services.add(new RouteCatalog());

        comboBoxChooseRouteService.setModel(new DefaultComboBoxModel(services.toArray()));
        comboBoxChooseRouteService.setRenderer(new RouteServiceListCellRenderer());
        comboBoxChooseRouteService.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                RouteService routeService = (RouteService) e.getItem();
                textFieldUserName.setText(preferences.get(USERNAME_PREFERENCE + routeService.getName(), ""));
                textFieldPassword.setText(preferences.get(PASSWORD_PREFERENCE + routeService.getName(), ""));
                checkBoxRememberMe.setSelected(preferences.getBoolean(REMEMBER_ME_PREFERENCE + routeService.getName(), true));
            }
        });

        RouteService serviceForRouteUrl = null;
        for (RouteService service : services) {
            if (service.isOriginOf(fileUrl)) {
                serviceForRouteUrl = service;
                break;
            }
        }

        if (serviceForRouteUrl != null) {
            setTitle(RouteConverter.getBundle().getString("save-title"));
            buttonUpload.setText(RouteConverter.getBundle().getString("save"));
            comboBoxChooseRouteService.setSelectedItem(serviceForRouteUrl);
        } else {
            setTitle(RouteConverter.getBundle().getString("upload-title"));
            buttonUpload.setText(RouteConverter.getBundle().getString("upload"));
        }

        BaseRoute firstRoute = formatAndRoutesModel.getRoutes().get(0);
        textFieldName.setText(firstRoute.getName());

        String startTime = null, endTime = null;
        if (firstRoute.getPositionCount() > 0) {
            startTime = formatTime(firstRoute.getPosition(0));
            endTime = formatTime(firstRoute.getPosition(firstRoute.getPositionCount() - 1));
        }

        // TODO add lots of error checking here
        // TODO see LengthToJLabelAdapter
        double meters = firstRoute.getDistance(0, firstRoute.getPositionCount() - 1);
        long milliSeconds = firstRoute.getDuration();
        String length = (meters > 0 ? MessageFormat.format(RouteConverter.getBundle().getString("length-value"), meters / 1000.0) : "-");
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.add(Calendar.MILLISECOND, (int) milliSeconds);
        Date date = calendar.getTime();
        String duration = MessageFormat.format(RouteConverter.getBundle().getString("duration-value"), date);

        textAreaDescription.setText(
                (firstRoute.getDescription() != null ? firstRoute.getDescription() + "\n" : "") +
                (startTime != null ? "from " + startTime : "") + 
                (endTime != null ? " to " + endTime : "") + "\n" +
                "length: " + length + "\n" +
                "duration: " + duration + "\n"
        );
        textAreaDescription.setBorder(textFieldName.getBorder());
        textAreaDescription.setFont(textFieldName.getFont());

        buttonUpload.addActionListener(new DialogAction(this) {
            public void run() {
                upload();
            }
        });

        buttonCancel.addActionListener(new DialogAction(this) {
            public void run() {
                cancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });

        contentPane.registerKeyboardAction(new DialogAction(this) {
            public void run() {
                cancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void upload() {
        RouteService routeService = (RouteService) comboBoxChooseRouteService.getSelectedItem();
        String userName = textFieldUserName.getText();
        char[] password = textFieldPassword.getPassword();

        routeService.upload(userName, password, fileUrl, textFieldName.getText(), textAreaDescription.getText());

        /*
        if(true) { // TODO if has been read by the service: PUT back
            // TODO use file name read from server
            File tempFile = RouteCatalog.createTempFile(null);
            new NavigationFileParser().write(formatAndRoutesModel.getRoutes(), (MultipleRoutesFormat) format, tempFile);
            // TODO use RouteCatalog object from BrowsePanel
            RouteCatalog routeService = new RouteCatalog(System.getProperty("catalog", "http://www.routeconverter.de/catalog/"));
            // TODO update description? or only file? where is description extracted from?
            routeService.updateRoute(categoryUrl, routeUrl, description, fileUrl);
            routeService.updateFile(fileUrl, tempFile);
        } else {
            // TODO store new file to service
        }
        */
        
        preferences.put(USERNAME_PREFERENCE + routeService.getName(), userName);
        preferences.putByteArray(PASSWORD_PREFERENCE + routeService.getName(), new String(password).getBytes());
        preferences.putBoolean(REMEMBER_ME_PREFERENCE + routeService.getName(), checkBoxRememberMe.isSelected());
    }

    private void cancel() {
        dispose();
    }

    private String formatTime(BaseNavigationPosition position) {
        CompactCalendar calendar = position.getTime();
        if (calendar == null)
            return null;
        return TIME_FORMAT.format(calendar.getTime().getTime());
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
        contentPane.setLayout(new GridLayoutManager(12, 2, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(11, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonUpload = new JButton();
        this.$$$loadButtonText$$$(buttonUpload, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("upload"));
        panel1.add(buttonUpload, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonCancel = new JButton();
        this.$$$loadButtonText$$$(buttonCancel, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("cancel"));
        panel1.add(buttonCancel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        contentPane.add(spacer2, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("RouteService:");
        contentPane.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxChooseRouteService = new JComboBox();
        contentPane.add(comboBoxChooseRouteService, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("username-colon"));
        contentPane.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("password-colon"));
        contentPane.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Please select to which RouteService the route should be uploaded:");
        contentPane.add(label4, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldUserName = new JTextField();
        contentPane.add(textFieldUserName, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        textFieldPassword = new JPasswordField();
        contentPane.add(textFieldPassword, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("name-colon"));
        contentPane.add(label5, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldName = new JTextField();
        contentPane.add(textFieldName, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("description-colon"));
        contentPane.add(label6, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Please name the route and provide a description:");
        contentPane.add(label7, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textAreaDescription = new JTextArea();
        contentPane.add(textAreaDescription, new GridConstraints(8, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel2, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(10, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 5), null, null, 0, false));
        checkBoxRememberMe = new JCheckBox();
        this.$$$loadButtonText$$$(checkBoxRememberMe, ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("remember"));
        contentPane.add(checkBoxRememberMe, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
}