/*
    This file is part of BaseRouteConverter.

    BaseRouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    BaseRouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BaseRouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.gui.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import slash.common.helpers.DateTimeParserException;
import slash.common.helpers.DateTimeParserFormatter;
import slash.common.io.Transfer;
import slash.common.type.CompactCalendar;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.ResourceBundle;
import java.util.TimeZone;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.text.MessageFormat.format;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.common.type.CompactCalendar.fromMillisAndTimeZone;
import static slash.navigation.gui.helpers.JMenuHelper.setMnemonic;
import static slash.navigation.gui.helpers.WindowHelper.showError;

/**
 * Dialog to enter a departure time; on confirmation fills every position of the
 * current route with its arrival time via {@link slash.navigation.converter.gui.helpers.PositionAugmenter#addDepartureTimes}.
 *
 * @author Christian Pesch
 */

public class DepartureTimeDialog extends SimpleDialog {
    private JPanel contentPane;
    private JLabel labelDeparture;
    private JTextField textFieldDeparture;
    private JButton buttonOk;
    private JButton buttonCancel;

    public DepartureTimeDialog() {
        super(BaseRouteConverter.getInstance().getFrame(), "departure-time");
        setTitle(BaseRouteConverter.getBundle().getString("departure-time-dialog-title"));
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOk);

        labelDeparture.setText(BaseRouteConverter.getBundle().getString("departure-time-label"));
        textFieldDeparture.setText(getPrefillText());

        buttonOk.addActionListener(new DialogAction(this) {
            public void run() {
                ok();
            }
        });

        setMnemonic(buttonCancel, "cancel-mnemonic");
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
        }, getKeyStroke(VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private DateTimeParserFormatter getDateTimeFormat() {
        String timeZoneId = BaseRouteConverter.getInstance().getTimeZone().getTimeZoneId();
        return Transfer.getDateTimeFormat(timeZoneId);
    }

    private String getPrefillText() {
        PositionsModel positionsModel = BaseRouteConverter.getInstance().getConvertPanel().getPositionsModel();
        if (positionsModel.getRowCount() > 0) {
            NavigationPosition first = positionsModel.getPosition(0);
            if (first.hasTime())
                return getDateTimeFormat().format(first.getTime());
        }

        // otherwise prefill today at 09:00 in the application's configured time zone
        TimeZone timeZone = BaseRouteConverter.getInstance().getTimeZone().getTimeZone();
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return getDateTimeFormat().format(fromMillis(calendar.getTimeInMillis()));
    }

    private void ok() {
        DateTimeParserFormatter formatter = getDateTimeFormat();
        CompactCalendar departure;
        try {
            Calendar parsed = formatter.parse(textFieldDeparture.getText(), null);
            departure = fromMillisAndTimeZone(parsed.getTimeInMillis(), "UTC");
        } catch (DateTimeParserException e) {
            JFrame frame = BaseRouteConverter.getInstance().getFrame();
            showError(frame, format(BaseRouteConverter.getBundle().getString("date-time-format-error"),
                    textFieldDeparture.getText(), formatter.getPatternInfo()), frame.getTitle());
            return; // keep the dialog open and change nothing
        }

        dispose();
        BaseRouteConverter.getInstance().getPositionAugmenter().addDepartureTimes(departure);
    }

    private void cancel() {
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
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panelFields = new JPanel();
        panelFields.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panelFields, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        labelDeparture = new JLabel();
        labelDeparture.setText("");
        panelFields.add(labelDeparture, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldDeparture = new JTextField();
        panelFields.add(textFieldDeparture, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(200, -1), null, 0, false));
        final JPanel panelGap = new JPanel();
        panelGap.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panelGap, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(-1, 10), null, 0, false));
        final JPanel panelButtons = new JPanel();
        panelButtons.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panelButtons, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        buttonOk = new JButton();
        this.$$$loadButtonText$$$(buttonOk, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "ok"));
        panelButtons.add(buttonOk, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panelButtons.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonCancel = new JButton();
        this.$$$loadButtonText$$$(buttonCancel, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "cancel"));
        panelButtons.add(buttonCancel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    /**
     * @noinspection ALL
     */
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
