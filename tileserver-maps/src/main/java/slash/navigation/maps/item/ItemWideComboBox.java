package slash.navigation.maps.item;

import java.awt.Dimension;

import javax.swing.JComboBox; 
 
// Taken from http://www.jroller.com/santhosh/entry/make_jcombobox_popup_wide_enough
//
// got this workaround from the following bug: 
//      http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4618607 
public class ItemWideComboBox<E extends Item> extends JComboBox<E>{ 
    private boolean layingOut = false; 
 
    public void doLayout(){ 
        try{ 
            layingOut = true; 
            super.doLayout(); 
        }finally{ 
            layingOut = false; 
        } 
    } 
 
    public Dimension getSize(){ 
        Dimension dim = super.getSize(); 
        
        if (!layingOut) {
        	for (int i = 0; i < getItemCount(); i++) {
        		Item item = getItemAt(i);
        		
        		if(item.getDescription() != null) {
        			dim.width = Math.max(dim.width, getFontMetrics(getFont()).stringWidth(item.getDescription())+30);
        		}
        	}
            dim.width = Math.max(dim.width, getPreferredSize().width);
        }
        
        return dim; 
    }
}