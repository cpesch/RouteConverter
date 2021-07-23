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
package slash.navigation.converter.gui.models;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.*;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.lang.Integer.MAX_VALUE;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.*;
import static slash.navigation.converter.gui.models.PositionColumns.*;
import static slash.navigation.gui.events.IgnoreEvent.isIgnoreEvent;
import static slash.navigation.gui.helpers.ImageHelper.resize;

/**
 * Caches {@link DistanceAndTime}, {@link ImageAndFile} for a {@link PositionsModelImpl}.
 *
 * @author Christian Pesch
 */
public class OverlayPositionsModel implements PositionsModel {
    private static final int IMAGE_HEIGHT_FOR_IMAGE_COLUMN = 200;
    private final PositionsModel delegate;
    private DistanceAndTimeAggregator distanceAndTimeAggregator;
    private final Map<Integer, ImageAndFile> indexToImageAndFile = new HashMap<>();

    public OverlayPositionsModel(PositionsModel delegate) {
        this.delegate = delegate;
        delegate.addTableModelListener(e -> {
            // clear overlay for updates on columns that have an effect on the distance
            int columnIndex = e.getColumn();
            if (columnIndex == LONGITUDE_COLUMN_INDEX ||
                    columnIndex == LATITUDE_COLUMN_INDEX ||
                    columnIndex == ALL_COLUMNS)
                clearOverlay();
        });
    }

    public OverlayPositionsModel(PositionsModel delegate, CharacteristicsModel characteristicsModel,
                                 DistanceAndTimeAggregator distanceAndTimeAggregator) {
        this(delegate);
        this.distanceAndTimeAggregator = distanceAndTimeAggregator;

        characteristicsModel.addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
            }

            public void intervalRemoved(ListDataEvent e) {
            }

            public void contentsChanged(ListDataEvent e) {
                // ignore events following setRoute()
                if (isIgnoreEvent(e))
                    return;
                // clear ImageAndFile and DistanceAndTimeAggregator caches when route characteristics is changed
                clearOverlay();
                distanceAndTimeAggregator.clearDistancesAndTimes();
            }
        });

        distanceAndTimeAggregator.addDistancesAndTimesAggregatorListener(new DistancesAndTimesAggregatorListener() {
            public void distancesAndTimesChanged(int firstIndex, int lastIndex) {
                invokeLater(() -> {
                    // make JTable rerender the distance and time column cells
                    fireTableRowsUpdatedInContinousRange(firstIndex, lastIndex, DISTANCE_COLUMN_INDEX);
                    fireTableRowsUpdatedInContinousRange(firstIndex, lastIndex, TIME_COLUMN_INDEX);
                });
            }
        });
    }

    private void clearOverlay() {
        indexToImageAndFile.clear();
    }

    // TableModel

    public int getRowCount() {
        return delegate.getRowCount();
    }

    public int getColumnCount() {
        return delegate.getColumnCount();
    }

    public String getColumnName(int columnIndex) {
        return delegate.getColumnName(columnIndex);
    }

    public Class<?> getColumnClass(int columnIndex) {
        return delegate.getColumnClass(columnIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return delegate.isCellEditable(rowIndex, columnIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        delegate.setValueAt(aValue, rowIndex, columnIndex);
    }

    public void edit(int rowIndex, PositionColumnValues columnToValues, boolean fireEvent, boolean trackUndo) {
        delegate.edit(rowIndex, columnToValues, fireEvent, trackUndo);
    }

    public void addTableModelListener(TableModelListener l) {
        delegate.addTableModelListener(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        delegate.removeTableModelListener(l);
    }

    // PositionsModel

    public BaseRoute getRoute() {
        return delegate.getRoute();
    }

    public void setRoute(BaseRoute route) {
        delegate.setRoute(route);
    }

    public NavigationPosition getPosition(int rowIndex) {
        return delegate.getPosition(rowIndex);
    }

    public int getIndex(NavigationPosition position) {
        return delegate.getIndex(position);
    }

    public List<NavigationPosition> getPositions(int[] rowIndices) {
        return delegate.getPositions(rowIndices);
    }

    public List<NavigationPosition> getPositions(int firstIndex, int lastIndex) {
        return delegate.getPositions(firstIndex, lastIndex);
    }

    public DistanceAndTimeAggregator getDistanceAndTimeAggregator() {
        return distanceAndTimeAggregator;
    }

    public double[] getDistancesFromStart(int startIndex, int endIndex) {
        if (getRoute().getCharacteristics().equals(Waypoints))
            return null;

        double[] result = new double[endIndex - startIndex + 1];
        int index = 0;
        double distance = 0.0;
        while (index <= endIndex) {
            DistanceAndTime distanceAndTime = distanceAndTimeAggregator.getAbsoluteDistancesAndTimes().get(index);
            if (distanceAndTime != null && distanceAndTime.getDistance() != null)
                distance = distanceAndTime.getDistance();
            if (index >= startIndex)
                result[index - startIndex] = distance;
            index++;
        }
        return result;
    }

    public double[] getDistancesFromStart(int[] indices) {
        if (getRoute().getCharacteristics().equals(Waypoints))
            return null;

        double[] result = new double[indices.length];
        Arrays.sort(indices);

        for (int i = 0; i < indices.length; i++) {
            DistanceAndTime distanceAndTime = distanceAndTimeAggregator.getAbsoluteDistancesAndTimes().get(indices[i]);
            if (distanceAndTime != null && distanceAndTime.getDistance() != null)
                result[i] = distanceAndTime.getDistance();
        }
        return result;
    }

    public long[] getTimesFromStart(int startIndex, int endIndex) {
        if (getRoute().getCharacteristics().equals(Waypoints))
            return null;

        long[] result = new long[endIndex - startIndex + 1];
        int index = 0;
        long time = 0;
        while (index <= endIndex) {
            DistanceAndTime distanceAndTime = distanceAndTimeAggregator.getAbsoluteDistancesAndTimes().get(index);
            if (distanceAndTime != null && distanceAndTime.getTimeInMillis() != null)
                time = distanceAndTime.getTimeInMillis();
            if (index >= startIndex)
                result[index - startIndex] = time;
            index++;
        }
        return result;
    }

    public long[] getTimesFromStart(int[] indices) {
        if (getRoute().getCharacteristics().equals(Waypoints))
            return null;

        long[] result = new long[indices.length];
        Arrays.sort(indices);

        for (int i = 0; i < indices.length; i++) {
            DistanceAndTime distanceAndTime = distanceAndTimeAggregator.getAbsoluteDistancesAndTimes().get(indices[i]);
            if (distanceAndTime != null && distanceAndTime.getTimeInMillis() != null)
                result[i] = distanceAndTime.getTimeInMillis();
        }
        return result;
    }


    public int[] getContainedPositions(BoundingBox boundingBox) {
        return delegate.getContainedPositions(boundingBox);
    }

    public int[] getPositionsWithinDistanceToPredecessor(double distance) {
        return delegate.getPositionsWithinDistanceToPredecessor(distance);
    }

    public int[] getInsignificantPositions(double threshold) {
        return delegate.getInsignificantPositions(threshold);
    }

    public int getClosestPosition(double longitude, double latitude, double threshold) {
        return delegate.getClosestPosition(longitude, latitude, threshold);
    }

    public int getClosestPosition(CompactCalendar time, long threshold) {
        return delegate.getClosestPosition(time, threshold);
    }

    public void add(int rowIndex, Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        delegate.add(rowIndex, longitude, latitude, elevation, speed, time, description);
    }

    public void add(int rowIndex, BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException {
        delegate.add(rowIndex, route);
    }

    public void add(int rowIndex, List<BaseNavigationPosition> positions) {
        delegate.add(rowIndex, positions);
    }

    public void remove(int firstIndex, int lastIndex) {
        delegate.remove(firstIndex, lastIndex);
    }

    public void remove(int[] rowIndices) {
        delegate.remove(rowIndices);
    }

    public void sort(Comparator<NavigationPosition> comparator) {
        delegate.sort(comparator);
    }

    public void revert() {
        delegate.revert();
    }

    public void top(int[] rowIndices) {
        delegate.top(rowIndices);
    }

    public void up(int[] rowIndices, int delta) {
        delegate.up(rowIndices, delta);
    }

    public void down(int[] rowIndices, int delta) {
        delegate.down(rowIndices, delta);
    }

    public void bottom(int[] rowIndices) {
        delegate.bottom(rowIndices);
    }

    // Overlay operations

    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case PHOTO_COLUMN_INDEX:
                return getImageAndFile(rowIndex);
            case DISTANCE_COLUMN_INDEX:
                return !getRoute().getCharacteristics().equals(Waypoints) ? getDistance(rowIndex) : null;
            case DISTANCE_DIFFERENCE_COLUMN_INDEX:
                return !getRoute().getCharacteristics().equals(Waypoints) ? getDistanceDifference(rowIndex) : null;
            case TIME_COLUMN_INDEX:
                if (getRoute().getCharacteristics().equals(Route))
                    return getTime(rowIndex);
                break;
            case ELEVATION_ASCEND_COLUMN_INDEX:
                return !getRoute().getCharacteristics().equals(Waypoints) ? getRoute().getElevationAscend(0, rowIndex) : null;
            case ELEVATION_DESCEND_COLUMN_INDEX:
                return !getRoute().getCharacteristics().equals(Waypoints) ? getRoute().getElevationDescend(0, rowIndex) : null;
            case ELEVATION_DIFFERENCE_COLUMN_INDEX:
                return !getRoute().getCharacteristics().equals(Waypoints) ? getRoute().getElevationDifference(rowIndex) : null;
        }
        return delegate.getValueAt(rowIndex, columnIndex);
    }

    private ImageAndFile getImageAndFile(int rowIndex) {
        ImageAndFile imageAndFile = indexToImageAndFile.get(rowIndex);
        if (imageAndFile == null) {
            NavigationPosition position = getPosition(rowIndex);
            if (position instanceof Wgs84Position) {
                Wgs84Position wgs84Position = (Wgs84Position) position;
                File file = wgs84Position.getOrigin(File.class);
                if (file != null && file.exists()) {
                    BufferedImage resize = resize(file, IMAGE_HEIGHT_FOR_IMAGE_COLUMN);
                    if (resize != null) {
                        imageAndFile = new ImageAndFile(new ImageIcon(resize), file);
                        indexToImageAndFile.put(rowIndex, imageAndFile);
                    }
                }
            }
        }
        return imageAndFile;
    }

    private Double getDistance(int rowIndex) {
        double[] distancesFromStart = getDistancesFromStart(rowIndex, rowIndex);
        return distancesFromStart != null ? distancesFromStart[0] : null;
    }

    private Double getDistanceDifference(int rowIndex) {
        if (getRoute().getCharacteristics().equals(Track)) {
            return getRoute().getDistanceDifference(rowIndex);
        }

        if (getRoute().getCharacteristics().equals(Route)) {
            DistanceAndTime distanceAndTime = distanceAndTimeAggregator.getRelativeDistancesAndTimes().get(rowIndex);
            if(distanceAndTime != null)
                return distanceAndTime.getDistance();
        }
        return null;
    }

    private CompactCalendar getTime(int rowIndex) {
        long[] timesFromStart = getTimesFromStart(rowIndex, rowIndex);
        return timesFromStart != null ? fromMillis(timesFromStart[0]) : null;
    }

    public boolean isContinousRange() {
        return delegate.isContinousRange();
    }

    public void fireTableRowsUpdated(int firstIndex, int lastIndex, int columnIndex) {
        delegate.fireTableRowsUpdated(firstIndex, lastIndex, columnIndex);
    }

    public void fireTableRowsUpdatedInContinousRange(int firstIndex, int lastIndex, int columnIndex) {
        delegate.fireTableRowsUpdatedInContinousRange(firstIndex, lastIndex, columnIndex);
    }
}
