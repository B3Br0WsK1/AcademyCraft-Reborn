package cn.academy.client.gui;
import net.minecraft.resources.Identifier;


/**
 * Represents one instance of message to be displayed on the main gui.
 * @author WeAthFolD
 */
public interface INotification {
    
    Identifier getIcon();
    
    /**
     * The title string to be directly displayed.
     */
    String getTitle();
    
    /**
     * The content string to be directly displayed.
     */
    String getContent();
    
}