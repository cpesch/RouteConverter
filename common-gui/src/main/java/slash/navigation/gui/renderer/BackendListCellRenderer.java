package slash.navigation.gui.renderer;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class BackendListCellRenderer extends DefaultListCellRenderer {
	private ListCellRenderer backend;
	
	public BackendListCellRenderer(ListCellRenderer backend) {
		this.backend = backend;
	}
	
	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus) {
		Component c = backend.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		if(!(c instanceof JLabel)) {
			c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		}
		
		return c;
	}

}
