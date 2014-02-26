package slash.navigation.converter.gui.mapview.models;

import slash.navigation.maps.models.ItemModel;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;
import java.util.HashMap;
import java.util.Map;

import static javax.swing.event.ListDataEvent.*;
import static javax.swing.event.TableModelEvent.*;

public class TableModelToComboBoxModelAdapter<E> implements ComboBoxModel<E> {
    private final TableModel modelDelegate;
    private final ItemModel<E> selectedDelegate;
    private final Map<ListDataListener, TableModelListener> listToTableListener = new HashMap<ListDataListener, TableModelListener>();

    public TableModelToComboBoxModelAdapter(TableModel modelDelegate, ItemModel<E> selectedDelegate) {
        this.modelDelegate = modelDelegate;
        this.selectedDelegate = selectedDelegate;

        // since DefaultComboBoxModel communicates changes of the selected item like this
        selectedDelegate.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                for (ListDataListener listener : listToTableListener.keySet())
                    listener.contentsChanged(new ListDataEvent(this, CONTENTS_CHANGED, -1, -1));
            }
        });
    }

    public int getSize() {
        return modelDelegate.getRowCount();
    }

    public E getElementAt(int index) {
        return (E) modelDelegate.getValueAt(index, -1);
    }

    public void addListDataListener(ListDataListener listener) {
        TableModelToListDataListenerAdapter adapter = new TableModelToListDataListenerAdapter(listener);
        listToTableListener.put(listener, adapter);
        modelDelegate.addTableModelListener(adapter);
    }

    public void removeListDataListener(ListDataListener listener) {
        TableModelListener adapter = listToTableListener.get(listener);
        if (adapter != null)
            modelDelegate.removeTableModelListener(adapter);
    }

    public Object getSelectedItem() {
        return selectedDelegate.getItem();
    }

    public void setSelectedItem(Object anItem) {
        selectedDelegate.setItem((E) anItem);
    }

    private static class TableModelToListDataListenerAdapter implements TableModelListener {
        private final ListDataListener delegate;

        private TableModelToListDataListenerAdapter(ListDataListener delegate) {
            this.delegate = delegate;
        }

        public void tableChanged(TableModelEvent e) {
            switch (e.getType()) {
                case INSERT:
                    delegate.intervalAdded(new ListDataEvent(this, INTERVAL_ADDED, e.getFirstRow(), e.getLastRow()));
                    break;
                case DELETE:
                    delegate.intervalRemoved(new ListDataEvent(this, INTERVAL_REMOVED, e.getFirstRow(), e.getLastRow()));
                    break;
                case UPDATE:
                    delegate.contentsChanged(new ListDataEvent(this, CONTENTS_CHANGED, e.getFirstRow(), e.getLastRow()));
                    break;
                default:
                    throw new IllegalArgumentException("Event " + e + " cannot be handled");
            }
        }
    }
}
