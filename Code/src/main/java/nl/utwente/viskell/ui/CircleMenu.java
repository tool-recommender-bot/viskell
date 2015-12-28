package nl.utwente.viskell.ui;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import jfxtras.scene.layout.CircularPane;
import nl.utwente.viskell.ui.components.Block;

/**
 * Circle menu is a context based menu implementation for {@link Block} classes.
 * Preferably each block class has it's own instance of circle menu. When a
 * block based class has significant differences to other block classes that
 * result in different context based actions it should use a specialized
 * extension of circle menu instead of this one.
 * <p>
 * Current context based features include delete. Copy, Paste and Save
 * functionality is under development.
 * </p>
 */
public class CircleMenu extends CircularPane {

    /** The context of the menu. */
    private Block block;

    /** Timed delay for hide of this menu after inactivity. */
    private Timeline hideDelay;
    
    /** Show the Circle menu for a specific block. */
    public static void showFor(Block block, Point2D pos) {
        CircleMenu menu = new CircleMenu(block);
        double centerX = pos.getX() - (menu.prefWidth(-1) / 2);
        double centerY = pos.getY() - (menu.prefHeight(-1) / 2);
        menu.setLayoutX(centerX);
        menu.setLayoutY(centerY);
        block.getToplevel().addMenu(menu);
    }
    
    private CircleMenu(Block block) {
        super();
        this.block = block;

        this.hideDelay = new Timeline(new KeyFrame(Duration.millis(4000), e-> this.hide()));
        this.hideDelay.play();
        this.setMouseTransparent(true);
        
        // Define menu items
        
        // Cut Option
        ImageView image = makeImageView("/ui/icons/appbar.scissor.png");
        MenuButton delete = new MenuButton("cut", image);
        delete.setOnActivate(() -> delete());
        this.add(delete);

        // Copy Option
        image = makeImageView("/ui/icons/appbar.page.copy.png");
        MenuButton copy = new MenuButton("copy", image);
        copy.setOnActivate(() -> copy());
        this.add(copy);

        // Paste Option
        image = makeImageView("/ui/icons/appbar.clipboard.paste.png");
        MenuButton paste = new MenuButton("paste", image);
        paste.setOnActivate(() -> paste());
        this.add(paste);

        // Save Option
        image = makeImageView("/ui/icons/appbar.save.png");
        MenuButton save = new MenuButton("save", image);
        save.setOnActivate(() -> saveBlock());
        this.add(save);

        // opening animation
        this.setScaleX(0.1);
        this.setScaleY(0.1);
        ScaleTransition opening = new ScaleTransition(Duration.millis(250), this);
        opening.setToX(1);
        opening.setToY(1);
        opening.setOnFinished(e -> this.setMouseTransparent(false));
        opening.play();
    }
    
    /** hide this menu by removing it */
    private void hide() {
        this.block.getToplevel().removeMenu(this);
    }
    
    private ImageView makeImageView(String path) {
        ImageView image = new ImageView(new Image(this.getClass().getResourceAsStream(path)));
        return image;
    }

    /** Copy the {@link Block} in this context. */
    private void copy() {
        block.getToplevel().copyBlock(block);
    }

    /** Paste {@link Block} from memory. */
    private void paste() {
    }

    /** Delete the {@link Block} in this context. */
    private void delete() {
        block.getToplevel().removeBlock(block);
    }

    /** Saves the {@link Block} in this context. */
    private void saveBlock() {
        // TODO store block in custom catalog?
    }
    
    /** A touch enabled button within this circle menu */
    private class MenuButton extends StackPane {
        /** Whether this button has been pressed */
        private boolean wasPressed;
        
        /** The action to execute on a 'click' */
        private Runnable clickAction;
        
        /**
         * @param name of the button
         * @param image node shown on the button
         */
        private MenuButton(String name, Node image) {
            super();
            Circle backing = new Circle(0, 0, 32, Color.GOLD);
            backing.setEffect(new DropShadow(20, 5, 5, Color.BLACK));
            backing.setStroke(Color.BLACK);
            backing.setStrokeWidth(1);
            this.getChildren().addAll(backing, image);
            this.setPrefSize(64, 64);
            
            this.wasPressed = false;
            
            this.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onPress);
            this.addEventHandler(TouchEvent.TOUCH_PRESSED, this::onPress);
            this.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onRelease);
            this.addEventHandler(TouchEvent.TOUCH_RELEASED, this::onRelease);
        }
        
        /** Sets the action to execute on a 'click' */
        private void setOnActivate(Runnable action) {
            this.clickAction = action;
        }
        
        /** Handles a press event for this button. */
        private void onPress(Event event) {
            this.wasPressed = true;
            CircleMenu.this.hideDelay.playFromStart();
            event.consume();
        }
        
        /** Handles a release event for this button. */
        private void onRelease(Event event) {
            if (this.wasPressed) {
                if (this.clickAction != null) {
                    this.clickAction.run();
                    // avoid double actions
                    this.clickAction = null;
                }
                
                CircleMenu.this.hide();
            }
            event.consume();
        }
    }
}
