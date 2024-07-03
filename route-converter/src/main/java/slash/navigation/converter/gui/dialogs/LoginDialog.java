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
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.feedback.domain.RouteFeedback;
import slash.navigation.gui.SimpleDialog;
import slash.navigation.gui.actions.DialogAction;
import slash.navigation.rest.exception.DuplicateNameException;
import slash.navigation.rest.exception.UnAuthorizedException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JOptionPane.*;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.io.Transfer.trim;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowserForTerms;
import static slash.navigation.gui.helpers.JMenuHelper.setMnemonic;

/**
 * Dialog to login a user to the RouteService.
 *
 * @author Christian Pesch
 */

public class LoginDialog extends SimpleDialog {
    private static final Logger log = Logger.getLogger(LoginDialog.class.getName());

    private final RouteFeedback routeFeedback;
    private JPanel contentPane;
    private JTabbedPane tabbedPane;

    private JTextField textFieldLogin;
    private JPasswordField passwordLogin;
    private JButton buttonLogin;
    private JButton buttonCancel;

    private JTextField textFieldName;
    private JTextField textFieldFirstName;
    private JTextField textFieldLastName;
    private JTextField textFieldEMail;
    private JPasswordField passwordRegister;
    private JPasswordField passwordRepeat;
    private JButton buttonRegister;
    private JButton buttonCancel2;
    private JCheckBox checkBoxAcceptTerms;
    private JLabel labelAcceptTerms;

    public LoginDialog(RouteFeedback routeFeedback) {
        super(RouteConverter.getInstance().getFrame(), "login");
        this.routeFeedback = routeFeedback;
        setTitle(RouteConverter.getBundle().getString("login-title"));
        setContentPane(contentPane);
        setModal(true);
        setDefaultButton();

        // always have the right default button
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                setDefaultButton();
            }
        });

        buttonLogin.addActionListener(new DialogAction(this) {
            public void run() {
                login();
            }
        });

        setMnemonic(buttonCancel, "cancel-mnemonic");
        buttonCancel.addActionListener(new DialogAction(this) {
            public void run() {
                cancel();
            }
        });

        labelAcceptTerms.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                startBrowserForTerms(LoginDialog.this);
            }
        });

        buttonRegister.addActionListener(new DialogAction(this) {
            public void run() {
                register();
            }
        });

        setMnemonic(buttonCancel2, "cancel-mnemonic");
        buttonCancel2.addActionListener(new DialogAction(this) {
            public void run() {
                cancel();
            }
        });

        textFieldLogin.setText(RouteConverter.getInstance().getCredentials().getUserName());

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

    private void setDefaultButton() {
        if (tabbedPane.getSelectedIndex() == 0)
            getRootPane().setDefaultButton(buttonLogin);
        else
            getRootPane().setDefaultButton(buttonRegister);
    }

    private boolean successful;

    public boolean isSuccessful() {
        return successful;
    }

    private void login() {
        JFrame frame = RouteConverter.getInstance().getFrame();

        String userName = trim(textFieldLogin.getText());
        if (userName == null) {
            showMessageDialog(frame, new JLabel(RouteConverter.getBundle().getString("login-no-username-error")),
                    frame.getTitle(), ERROR_MESSAGE);
            return;
        }
        String password = trim(new String(passwordLogin.getPassword()));
        if (password == null) {
            showMessageDialog(frame, new JLabel(RouteConverter.getBundle().getString("login-no-password-error")),
                    frame.getTitle(), ERROR_MESSAGE);
            return;
        }

        try {
            routeFeedback.checkForLogin(userName, password);

            successful = true;
            RouteConverter.getInstance().setLogin(userName, password);

            showMessageDialog(frame, new JLabel(RouteConverter.getBundle().getString("login-success")),
                    frame.getTitle(), INFORMATION_MESSAGE);

            dispose();
        } catch (UnAuthorizedException e) {
            showMessageDialog(frame, new JLabel(RouteConverter.getBundle().getString("login-failure")),
                    frame.getTitle(), ERROR_MESSAGE);
        } catch (Throwable t) {
            log.severe("Could not login: " + t);
            showMessageDialog(frame, new JLabel(MessageFormat.format(RouteConverter.getBundle().getString("route-service-error"),
                    t.getClass().getSimpleName(), getLocalizedMessage(t))), frame.getTitle(), ERROR_MESSAGE);
        }
    }

    private void register() {
        JFrame frame = RouteConverter.getInstance().getFrame();

        String userName = trim(textFieldName.getText());
        if (userName == null || userName.length() < 4) {
            showMessageDialog(frame, new JLabel(RouteConverter.getBundle().getString("register-username-too-short-error")),
                    frame.getTitle(), ERROR_MESSAGE);
            return;
        }
        String firstName = trim(textFieldFirstName.getText());
        if (firstName == null || firstName.length() < 3) {
            showMessageDialog(frame, new JLabel(RouteConverter.getBundle().getString("register-first-name-too-short-error")),
                    frame.getTitle(), ERROR_MESSAGE);
            return;
        }
        String lastName = trim(textFieldLastName.getText());
        if (lastName == null || lastName.length() < 3) {
            showMessageDialog(frame, new JLabel(RouteConverter.getBundle().getString("register-last-name-too-short-error")),
                    frame.getTitle(), ERROR_MESSAGE);
            return;
        }

        String email = trim(textFieldEMail.getText());
        if (email == null || !email.contains("@") || !email.contains(".")) {
            showMessageDialog(frame, new JLabel(RouteConverter.getBundle().getString("register-email-invalid-error")),
                    frame.getTitle(), ERROR_MESSAGE);
            return;
        }

        String password = trim(new String(passwordRegister.getPassword()));
        if (password == null || password.length() < 4) {
            showMessageDialog(frame, new JLabel(RouteConverter.getBundle().getString("register-password-too-short-error")),
                    frame.getTitle(), ERROR_MESSAGE);
            return;
        }
        String repeat = trim(new String(passwordRepeat.getPassword()));
        if (!password.equals(repeat)) {
            showMessageDialog(frame, new JLabel(RouteConverter.getBundle().getString("register-password-do-not-match-error")),
                    frame.getTitle(), ERROR_MESSAGE);
            return;
        }

        if (!checkBoxAcceptTerms.isSelected()) {
            showMessageDialog(frame, new JLabel(RouteConverter.getBundle().getString("register-terms-not-accepted-error")),
                    frame.getTitle(), ERROR_MESSAGE);
            return;
        }

        try {
            routeFeedback.addUser(userName, password, firstName, lastName, email);

            successful = true;
            RouteConverter.getInstance().setLogin(userName, password);

            showMessageDialog(frame, new JLabel(RouteConverter.getBundle().getString("register-success")),
                    frame.getTitle(), INFORMATION_MESSAGE);

            dispose();
        } catch (DuplicateNameException e) {
            showMessageDialog(frame, new JLabel(RouteConverter.getBundle().getString("register-username-exists-error")),
                    frame.getTitle(), ERROR_MESSAGE);
        } catch (Throwable t) {
            log.severe("Could not register: " + t);
            showMessageDialog(frame, new JLabel(MessageFormat.format(RouteConverter.getBundle().getString("route-service-error"),
                    t.getClass().getSimpleName(), getLocalizedMessage(t))), frame.getTitle(), ERROR_MESSAGE);
        }
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
        contentPane.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane = new JTabbedPane();
        contentPane.add(tabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(300, 290), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(6, 2, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "login"), panel1);
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "username-colon"));
        panel1.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldLogin = new JTextField();
        textFieldLogin.setText("");
        panel1.add(textFieldLogin, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        passwordLogin = new JPasswordField();
        panel1.add(passwordLogin, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "password-colon"));
        panel1.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonLogin = new JButton();
        this.$$$loadButtonText$$$(buttonLogin, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "login"));
        panel2.add(buttonLogin, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonCancel = new JButton();
        this.$$$loadButtonText$$$(buttonCancel, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "cancel"));
        panel2.add(buttonCancel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "please-login"));
        panel1.add(label3, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(11, 2, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "register"), panel4);
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "username-colon"));
        panel4.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "password-colon"));
        panel4.add(label5, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "repeat-password-colon"));
        panel4.add(label6, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldName = new JTextField();
        panel4.add(textFieldName, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "email-colon"));
        panel4.add(label7, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldEMail = new JTextField();
        panel4.add(textFieldEMail, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        passwordRegister = new JPasswordField();
        panel4.add(passwordRegister, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        passwordRepeat = new JPasswordField();
        panel4.add(passwordRepeat, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel5, new GridConstraints(9, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRegister = new JButton();
        this.$$$loadButtonText$$$(buttonRegister, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "register"));
        panel5.add(buttonRegister, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel5.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonCancel2 = new JButton();
        this.$$$loadButtonText$$$(buttonCancel2, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "cancel"));
        panel5.add(buttonCancel2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        this.$$$loadLabelText$$$(label8, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "please-register"));
        panel4.add(label8, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        this.$$$loadLabelText$$$(label9, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "first-name-colon"));
        panel4.add(label9, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        this.$$$loadLabelText$$$(label10, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "last-name-colon"));
        panel4.add(label10, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldFirstName = new JTextField();
        panel4.add(textFieldFirstName, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        textFieldLastName = new JTextField();
        panel4.add(textFieldLastName, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        labelAcceptTerms = new JLabel();
        this.$$$loadLabelText$$$(labelAcceptTerms, this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "accept-terms"));
        panel4.add(labelAcceptTerms, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxAcceptTerms = new JCheckBox();
        checkBoxAcceptTerms.setHideActionText(true);
        panel4.add(checkBoxAcceptTerms, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel4.add(spacer4, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel6, new GridConstraints(10, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 10), null, null, 0, false));
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

}
