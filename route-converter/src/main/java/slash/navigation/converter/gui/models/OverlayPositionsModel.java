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
import slash.navigation.common.BoundingBox;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.NavigationPosition;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.converter.gui.models.PositionColumns.DISTANCE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.DISTANCE_DIFFERENCE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_ASCEND_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_DESCEND_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_DIFFERENCE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LATITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LONGITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.PHOTO_COLUMN_INDEX;
import static slash.navigation.gui.helpers.ImageHelper.resize;

/**
 * Caches {@link DistanceAndTime}, {@link ImageAndFile} for a {@link PositionsModelImpl}.
 *
 * @author Christian Pesch
 */
public class OverlayPositionsModel implements PositionsModel {
    private static final int IMAGE_HEIGHT_FOR_IMAGE_COLUMN = 200;
    private final PositionsModel delegate;
    private Map<Integer, ImageAndFile> indexToImageAndFile = new HashMap<>();
    private final Map<Integer, DistanceAndTime> indexToDistanceAndTime = new HashMap<>();
    private double[] distancesFromStart;

    public OverlayPositionsModel(PositionsModel delegate) {
        this.delegate = delegate;
        delegate.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                // clear overlay for updates on columns that have an effect on the distance
                if (e.getColumn() == LONGITUDE_COLUMN_INDEX ||
                        e.getColumn() == LATITUDE_COLUMN_INDEX ||
                        e.getColumn() == ALL_COLUMNS)
                    clearOverlay();
            }
        });
    }

    public OverlayPositionsModel(PositionsModel delegate, CharacteristicsModel characteristicsModel) {
        this(delegate);

        characteristicsModel.addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
            }

            public void intervalRemoved(ListDataEvent e) {
            }

            public void contentsChanged(ListDataEvent e) {
                // clear overlay when route characteristics is changed
                clearOverlay();
            }
        });
    }

    private void clearOverlay() {
        indexToDistanceAndTime.clear();
        distancesFromStart = null;
        indexToImageAndFile.clear();
        if (getRowCount() > 0)
            delegate.fireTableRowsUpdated(0, getRowCount() - 1, DISTANCE_COLUMN_INDEX);
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

    public boolean isContinousRange() {
        return delegate.isContinousRange();
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
                return getDistance(rowIndex);
            case DISTANCE_DIFFERENCE_COLUMN_INDEX:
                return getDistanceDifference(rowIndex);
            case ELEVATION_ASCEND_COLUMN_INDEX:
                return getRoute().getElevationAscend(0, rowIndex);
            case ELEVATION_DESCEND_COLUMN_INDEX:
                return getRoute().getElevationDescend(0, rowIndex);
            case ELEVATION_DIFFERENCE_COLUMN_INDEX:
                return getRoute().getElevationDifference(rowIndex);
        }
        return delegate.getValueAt(rowIndex, columnIndex);
    }

    private ImageAndFile getImageAndFile(int rowIndex) {
        ImageAndFile imageAndFile = indexToImageAndFile.get(rowIndex);
        if (imageAndFile == null) {
            NavigationPosition position = getPosition(rowIndex);
            if (position instanceof Wgs84Position) {
                Wgs84Position wgs84Position = Wgs84Position.class.cast(position);
                File file = wgs84Position.getOrigin(File.class);
                if (file != null && file.exists()) {
                    BufferedImage resize = resize(file, IMAGE_HEIGHT_FOR_IMAGE_COLUMN);
                    if(resize != null) {
                        imageAndFile = new ImageAndFile(new ImageIcon(resize), file);
                        indexToImageAndFile.put(rowIndex, imageAndFile);
                    }
                }
            }
        }
        return imageAndFile;
    }

    private Double getDistance(int rowIndex) {
        if(getRoute().getCharacteristics().equals(Track)) {
            if (distancesFromStart == null)
                distancesFromStart = getRoute().getDistancesFromStart(0, getRoute().getPositionCount() - 1);
            return distancesFromStart[rowIndex];
        }

        if (getRoute().getCharacteristics().equals(Route)) {
            DistanceAndTime distanceAndTime = indexToDistanceAndTime.get(rowIndex);
            return distanceAndTime != null ? distanceAndTime.getDistance() : null;
        }

        return null;
    }

    private Double getDistanceDifference(int rowIndex) {
        if(getRoute().getCharacteristics().equals(Track)) {
            return getRoute().getDistanceDifference(rowIndex);
        }

        if (getRoute().getCharacteristics().equals(Route) && rowIndex > 0) {
            DistanceAndTime previous = indexToDistanceAndTime.get(rowIndex - 1);
            DistanceAndTime current = indexToDistanceAndTime.get(rowIndex);
            if(previous != null && current != null) {
                Double d1 = previous.getDistance();
                Double d2 = current.getDistance();
                if(d1 != null && d2 != null)
                    return d2 - d1;
            }
        }

        return null;
    }

    public void calculatedDistanceFromRouting(Map<Integer, DistanceAndTime> indexToRoutedDistanceAndTime) {
        this.indexToDistanceAndTime.putAll(indexToRoutedDistanceAndTime);
        int firstIndex = getRowCount() - 1;
        int lastIndex = 0;
        for (Integer index : this.indexToDistanceAndTime.keySet()) {
            if (index < firstIndex)
                firstIndex = index;
            else if (index > lastIndex)
                lastIndex = index;
        }
        delegate.fireTableRowsUpdated(firstIndex, lastIndex, DISTANCE_COLUMN_INDEX);
    }

    public void fireTableRowsUpdated(int firstIndex, int lastIndex, int columnIndex) {
        delegate.fireTableRowsUpdated(firstIndex, lastIndex, columnIndex);
    }
}
