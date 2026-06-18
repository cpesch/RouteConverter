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
package slash.navigation.download.tools.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Source;
import slash.navigation.download.tools.helpers.GlobToRegex;
import slash.navigation.download.tools.helpers.WgetCommandBuilder;

import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.System.getProperty;
import static java.nio.file.Files.isDirectory;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
import static javax.swing.SortOrder.ASCENDING;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * Desktop UI for snapshot-based mirroring jobs.
 *
 * @author Christian Pesch
 */
public class CatalogMirrorFrame {
    private static final String DEFAULT_SNAPSHOT_DIRECTORY = getProperty("user.home") + "/.routeconverter/snapshot-api.routeconverter.com";
    private static final String DEFAULT_MIRROR_DIRECTORY = "/Volumes/5TB Mirror/Mirrors";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.##");
    private static final String PREFERENCE_SNAPSHOT_DIRECTORY = "snapshotDirectory";
    private static final String PREFERENCE_MIRROR_DIRECTORY = "mirrorDirectory";
    private static final String PREFERENCE_JOB_FILE = "mirrorJobFile";

    private JPanel contentPane;
    private JTextField textFieldSnapshotDirectory;
    private JButton buttonBrowseSnapshotDirectory;
    private JTextField textFieldMirrorDirectory;
    private JButton buttonBrowseMirrorDirectory;
    private JLabel labelJobFile;
    private JTextField textFieldJobFile;
    private JButton buttonBrowseJobFile;
    private JButton buttonReload;
    private JButton buttonStartSelected;
    private JButton buttonStartAll;
    private JTable tableJobs;
    private JTextArea textAreaDetails;
    private JTextArea textAreaLog;
    private JLabel labelSummary;

    private final Preferences preferences = Preferences.userNodeForPackage(CatalogMirrorFrame.class);
    private final MirrorJobsTableModel tableModel = new MirrorJobsTableModel();
    private final SnapshotCatalogLoader snapshotCatalogLoader = new SnapshotCatalogLoader();
    private final WgetCommandBuilder commandBuilder = new WgetCommandBuilder();
    private SwingWorker<Void, String> runningWorker;

    private JFrame frame;

    public CatalogMirrorFrame() {
        configureDefaults();
        configureTable();
        configureActions();
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    private void configureDefaults() {
        textFieldSnapshotDirectory.setText(preferences.get(PREFERENCE_SNAPSHOT_DIRECTORY, DEFAULT_SNAPSHOT_DIRECTORY));
        textFieldMirrorDirectory.setText(preferences.get(PREFERENCE_MIRROR_DIRECTORY, DEFAULT_MIRROR_DIRECTORY));
        // legacy mirror-jobs-file row stays in the generated layout but is no longer used;
        // scan + mirror config now comes from each datasource <source> element in the snapshot.
        labelJobFile.setVisible(false);
        textFieldJobFile.setVisible(false);
        buttonBrowseJobFile.setVisible(false);
        textAreaDetails.setEditable(false);
        textAreaLog.setEditable(false);
        textAreaDetails.setCaretPosition(0);
        textAreaLog.setCaretPosition(0);
    }

    private void configureTable() {
        tableJobs.setModel(tableModel);
        tableJobs.setSelectionMode(MULTIPLE_INTERVAL_SELECTION);
        tableJobs.setAutoCreateRowSorter(true);
        tableJobs.setFillsViewportHeight(true);
        tableJobs.getRowSorter().setSortKeys(List.of(new SortKey(0, ASCENDING)));
        tableJobs.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                updateDetails();
        });
        tableJobs.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 && !isRunning())
                    startSelectedJobs();
            }
        });

        DefaultTableCellRenderer numberRenderer = new DefaultTableCellRenderer();
        numberRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int column : new int[]{2, 3, 4, 5}) {
            tableJobs.getColumnModel().getColumn(column).setCellRenderer(numberRenderer);
        }

        DefaultTableCellRenderer sizeRenderer = new DefaultTableCellRenderer() {
            protected void setValue(Object value) {
                if (value instanceof Long longValue)
                    setText(formatBytes(longValue));
                else
                    super.setValue(value);
            }
        };
        sizeRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tableJobs.getColumnModel().getColumn(6).setCellRenderer(sizeRenderer);

        for (int column : new int[]{2, 3, 4}) {
            TableColumn tableColumn = tableJobs.getColumnModel().getColumn(column);
            tableColumn.setPreferredWidth(55);
            tableColumn.setMaxWidth(70);
        }
    }

    private void configureActions() {
        buttonBrowseSnapshotDirectory.addActionListener(e -> chooseDirectory(textFieldSnapshotDirectory, "Choose snapshot directory"));
        buttonBrowseMirrorDirectory.addActionListener(e -> chooseDirectory(textFieldMirrorDirectory, "Choose mirror directory"));
        buttonReload.addActionListener(e -> reloadData());
        buttonStartSelected.addActionListener(e -> startSelectedJobs());
        buttonStartAll.addActionListener(e -> startAllJobs());
    }

    private void chooseDirectory(JTextField target, String title) {
        JFileChooser chooser = new JFileChooser(target.getText());
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
            target.setText(chooser.getSelectedFile().getAbsolutePath());
    }

    public void savePreferences() {
        preferences.put(PREFERENCE_SNAPSHOT_DIRECTORY, textFieldSnapshotDirectory.getText().trim());
        preferences.put(PREFERENCE_MIRROR_DIRECTORY, textFieldMirrorDirectory.getText().trim());
    }

    public void reloadData() {
        if (isRunning())
            return;

        try {
            savePreferences();
            SnapshotCatalogData snapshotCatalogData = loadSnapshotCatalogData();
            List<MirrorJobRow> rows = new ArrayList<>();
            for (SnapshotJobInfo info : snapshotCatalogData.getJobInfos()) {
                if (info.source() != null)
                    rows.add(new MirrorJobRow(info));
            }
            tableModel.setRows(rows);
            labelSummary.setText("Loaded " + rows.size() + " mirror jobs from " + snapshotCatalogData.getDataSourceCount() +
                    " data sources (" + snapshotCatalogData.getEditionCount() + " editions) in " + snapshotCatalogData.getSnapshotRoot());
            appendLog("Loaded " + rows.size() + " mirror jobs from snapshot");
            if (!rows.isEmpty())
                tableJobs.setRowSelectionInterval(0, 0);
            updateDetails();
        } catch (Exception e) {
            tableModel.setRows(List.of());
            labelSummary.setText("Could not load data: " + e.getMessage());
            textAreaDetails.setText("");
            appendLog("ERROR: " + e.getMessage());
            showMessageDialog(frame, e.getMessage(), "Load failed", ERROR_MESSAGE);
        }
    }

    private SnapshotCatalogData loadSnapshotCatalogData() throws Exception {
        Path snapshotDirectory = getSnapshotDirectory();
        return snapshotCatalogLoader.load(snapshotDirectory);
    }

    private void startSelectedJobs() {
        if (isRunning())
            return;

        int[] selectedRows = tableJobs.getSelectedRows();
        if (selectedRows.length == 0) {
            showMessageDialog(frame, "Please select at least one mirror job.", "No selection", INFORMATION_MESSAGE);
            return;
        }

        List<MirrorJobRow> rows = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            rows.add(tableModel.getRow(tableJobs.convertRowIndexToModel(selectedRow)));
        }
        startJobs(rows);
    }

    private void startAllJobs() {
        if (isRunning())
            return;
        startJobs(tableModel.getRows());
    }

    private void startJobs(List<MirrorJobRow> rows) {
        if (rows.isEmpty())
            return;

        Path mirrorDirectory;
        try {
            mirrorDirectory = getMirrorDirectory();
            savePreferences();
            if (!Files.exists(mirrorDirectory))
                Files.createDirectories(mirrorDirectory);
        } catch (IOException e) {
            showMessageDialog(frame, e.getMessage(), "Invalid mirror directory", ERROR_MESSAGE);
            return;
        }

        setRunning(true);
        runningWorker = new SwingWorker<>() {
            protected Void doInBackground() {
                for (MirrorJobRow row : rows) {
                    if (isCancelled())
                        break;
                    updateStatus(row, "Running");
                    logAsync("Starting " + row.getId() + " from " + row.getSnapshotJobInfo().mirrorUrl());
                    try {
                        int exitCode = runWget(row, mirrorDirectory);
                        if (exitCode == 0) {
                            runCleanup(row, mirrorDirectory);
                            updateStatus(row, "Done");
                            logAsync("Finished " + row.getId());
                        } else {
                            updateStatus(row, "Failed (exit " + exitCode + ")");
                            logAsync("ERROR: " + row.getId() + " failed with exit code " + exitCode);
                        }
                    } catch (Exception e) {
                        updateStatus(row, "Failed");
                        logAsync("ERROR: " + row.getId() + ": " + e.getMessage());
                    }
                }
                return null;
            }

            protected void done() {
                setRunning(false);
                runningWorker = null;
                updateDetails();
            }
        };
        runningWorker.execute();
    }

    private int runWget(MirrorJobRow row, Path mirrorDirectory) throws IOException, InterruptedException {
        List<String> command = commandBuilder.buildCommand(row.getDataSource(), row.getSource(), mirrorDirectory);
        publishCommand(row.getId(), commandBuilder.formatCommand(command));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                publishCommand(row.getId(), line);
            }
        }
        return process.waitFor();
    }

    private void runCleanup(MirrorJobRow row, Path mirrorDirectory) throws IOException {
        Source source = row.getSource();
        if (source == null || source.getExcludes() == null || source.getExcludes().isEmpty())
            return;
        List<Pattern> patterns = new ArrayList<>();
        for (String exclude : source.getExcludes())
            patterns.add(Pattern.compile(GlobToRegex.convert(exclude)));

        try (Stream<Path> paths = Files.walk(mirrorDirectory, FileVisitOption.FOLLOW_LINKS)) {
            List<Path> candidates = paths.filter(Files::isRegularFile).toList();
            for (Path candidate : candidates) {
                String relative = mirrorDirectory.relativize(candidate).toString();
                for (Pattern pattern : patterns) {
                    if (pattern.matcher(relative).matches()) {
                        Files.deleteIfExists(candidate);
                        publishCommand(row.getId(), "Removed " + relative);
                        break;
                    }
                }
            }
        }
    }

    private void publishCommand(String id, String message) {
        logAsync("[" + id + "] " + message);
    }

    private void logAsync(String message) {
        invokeLater(() -> appendLog(message));
    }

    private void updateStatus(MirrorJobRow row, String status) {
        row.setStatus(status);
        invokeLater(() -> tableModel.updateRow(row));
    }

    private boolean isRunning() {
        return runningWorker != null;
    }

    private void setRunning(boolean running) {
        buttonReload.setEnabled(!running);
        buttonStartSelected.setEnabled(!running);
        buttonStartAll.setEnabled(!running);
        buttonBrowseSnapshotDirectory.setEnabled(!running);
        buttonBrowseMirrorDirectory.setEnabled(!running);
    }

    private void updateDetails() {
        int selectedRow = tableJobs.getSelectedRow();
        if (selectedRow < 0) {
            textAreaDetails.setText("Select a mirror job to see snapshot details and the generated wget command.");
            return;
        }

        MirrorJobRow row = tableModel.getRow(tableJobs.convertRowIndexToModel(selectedRow));
        textAreaDetails.setText(buildDetails(row));
        textAreaDetails.setCaretPosition(0);
    }

    private String buildDetails(MirrorJobRow row) {
        StringBuilder builder = new StringBuilder();
        SnapshotJobInfo info = row.getSnapshotJobInfo();
        Source source = row.getSource();
        DataSource dataSource = row.getDataSource();

        builder.append("Mirror job\n");
        builder.append("========\n");
        builder.append("ID: ").append(row.getId()).append('\n');
        builder.append("URL: ").append(info.mirrorUrl()).append('\n');
        builder.append("Level: ").append(source != null && source.getLevel() != null ? source.getLevel() : "-").append('\n');
        builder.append("Includes: ").append(asText(source != null ? source.getIncludes() : null)).append('\n');
        builder.append("Excludes: ").append(asText(source != null ? source.getExcludes() : null)).append('\n');
        builder.append("Status: ").append(row.getStatus()).append("\n\n");

        builder.append("Generated command\n");
        builder.append("-----------------\n");
        builder.append(commandBuilder.formatCommand(commandBuilder.buildCommand(dataSource, source, getMirrorDirectoryUnchecked()))).append("\n\n");

        builder.append("Snapshot data\n");
        builder.append("-------------\n");
        builder.append("Name: ").append(info.name()).append('\n');
        builder.append("Base URL: ").append(info.baseUrl()).append('\n');
        builder.append("Directory: ").append(dataSource.getDirectory()).append('\n');
        builder.append("Action: ").append(dataSource.getAction()).append('\n');
        builder.append("Href: ").append(dataSource.getHref()).append('\n');
        builder.append("Files: ").append(info.fileCount()).append('\n');
        builder.append("Maps: ").append(info.mapCount()).append('\n');
        builder.append("Themes: ").append(info.themeCount()).append('\n');
        builder.append("Downloadables: ").append(info.downloadableCount()).append('\n');
        builder.append("Total size: ").append(formatBytes(info.totalSize())).append('\n');
        builder.append("Snapshot XML: ").append(info.snapshotFile()).append('\n');
        return builder.toString();
    }

    private String asText(List<String> values) {
        return values == null || values.isEmpty() ? "-" : String.join(" | ", values);
    }

    private Path getSnapshotDirectory() {
        return Paths.get(textFieldSnapshotDirectory.getText().trim());
    }

    private Path getMirrorDirectory() {
        return Paths.get(textFieldMirrorDirectory.getText().trim());
    }

    private Path getMirrorDirectoryUnchecked() {
        String value = textFieldMirrorDirectory.getText().trim();
        return value.isEmpty() ? Paths.get(DEFAULT_MIRROR_DIRECTORY) : Paths.get(value);
    }

    private void appendLog(String message) {
        textAreaLog.append("[" + LocalTime.now().format(TIME_FORMAT) + "] " + message + "\n");
        textAreaLog.setCaretPosition(textAreaLog.getDocument().getLength());
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0L)
            return "0 B";

        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }
        return DECIMAL_FORMAT.format(value) + " " + units[unitIndex];
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
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), 8, 8));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), 8, 4));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Snapshot directory");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldSnapshotDirectory = new JTextField();
        panel1.add(textFieldSnapshotDirectory, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
                null, new Dimension(400, -1), null, 0, false));
        buttonBrowseSnapshotDirectory = new JButton();
        buttonBrowseSnapshotDirectory.setText("Browse...");
        panel1.add(buttonBrowseSnapshotDirectory, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelJobFile = new JLabel();
        labelJobFile.setText("Mirror jobs file");
        panel1.add(labelJobFile, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldJobFile = new JTextField();
        panel1.add(textFieldJobFile, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false));
        buttonBrowseJobFile = new JButton();
        buttonBrowseJobFile.setText("Browse...");
        panel1.add(buttonBrowseJobFile, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Mirror drive directory");
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldMirrorDirectory = new JTextField();
        panel1.add(textFieldMirrorDirectory, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false));
        buttonBrowseMirrorDirectory = new JButton();
        buttonBrowseMirrorDirectory.setText("Browse...");
        panel1.add(buttonBrowseMirrorDirectory, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), 8, 0));
        contentPane.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null, 0, false));
        labelSummary = new JLabel();
        labelSummary.setText("No data loaded");
        panel2.add(labelSummary, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonReload = new JButton();
        buttonReload.setText("Reload");
        panel2.add(buttonReload, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonStartSelected = new JButton();
        buttonStartSelected.setText("Start selected");
        panel2.add(buttonStartSelected, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonStartAll = new JButton();
        buttonStartAll.setText("Start all");
        panel2.add(buttonStartAll, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JSplitPane splitPane1 = new JSplitPane();
        splitPane1.setDividerLocation(360);
        splitPane1.setOrientation(0);
        contentPane.add(splitPane1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, new Dimension(200, 500), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        splitPane1.setTopComponent(scrollPane1);
        tableJobs = new JTable();
        scrollPane1.setViewportView(tableJobs);
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        splitPane1.setBottomComponent(tabbedPane1);
        final JScrollPane scrollPane2 = new JScrollPane();
        tabbedPane1.addTab("Details", scrollPane2);
        textAreaDetails = new JTextArea();
        textAreaDetails.setFont(new Font("Monospaced", Font.PLAIN, 12));
        scrollPane2.setViewportView(textAreaDetails);
        final JScrollPane scrollPane3 = new JScrollPane();
        tabbedPane1.addTab("Log", scrollPane3);
        textAreaLog = new JTextArea();
        textAreaLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        scrollPane3.setViewportView(textAreaLog);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}


