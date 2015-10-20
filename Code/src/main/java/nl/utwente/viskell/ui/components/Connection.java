package nl.utwente.viskell.ui.components;

import com.google.common.collect.ImmutableMap;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.transform.Transform;
import nl.utwente.viskell.ui.ConnectionCreationManager;
import nl.utwente.viskell.ui.CustomUIPane;
import nl.utwente.viskell.ui.serialize.Bundleable;

import java.util.Map;
import java.util.Optional;


/**
 * This is a Connection that represents a flow between an {@link InputAnchor}
 * and {@link OutputAnchor}. Both anchors are stored referenced respectively as
 * startAnchor and endAnchor {@link Optional} within this class.
 * <p>
 * It is possible for a connection to exist without both anchors being present,
 * whenever the position of either the start or end anchor changes the
 * {@link #invalidateAnchorPositions()} should be called to refresh the visual
 * representation of the connection.
 * </p>
 * 
 * Connection is also a changeListener for a Transform, in order to be able to
 * update the Line's position when the anchor's positions change.
 */
public class Connection extends ConnectionLine implements
        ChangeListener<Transform>, Bundleable {
    /** Starting point of this Line that can be Anchored onto other objects. */
    private Optional<OutputAnchor> startAnchor = Optional.empty();
    /** Ending point of this Line that can be Anchored onto other objects. */
    private Optional<InputAnchor> endAnchor = Optional.empty();

    /** The Pane this Connection is on. */
    private CustomUIPane pane;
    
    /** Property describing the error state. */
    private BooleanProperty errorState;

    /** 
     * Construct a new Connection.
     * @param pane The Pane this Connection is on.
     */
    public Connection(CustomUIPane pane) {
        this.pane = pane;
        this.errorState = new SimpleBooleanProperty(false);
        this.errorState.addListener(this::checkErrorListener);
    }

    /** 
     * Construct a new Connection.
     * @param pane The Pane this Connection is on.
     * @param anchor A ConnectionAnchor of this Connection.
     */
    public Connection(CustomUIPane pane, ConnectionAnchor anchor) {
        this(pane);
        setStartPositionParent(getAnchorPositionInPane(anchor));
        setEndPositionParent(getAnchorPositionInPane(anchor));
        setAnchor(anchor);
    }

    /** 
     * Construct a new Connection.
     * @param pane The Pane this Connection is on.
     * @param from The OutputAnchor of this Connection.
     * @param to The InputAnchor of this Connection.
     */
    public Connection(CustomUIPane pane, OutputAnchor from, InputAnchor to) {
        this(pane);
        this.setAnchor(from);
        this.setAnchor(to);
    }

    
    /** Set a new error state. */
    public void setErrorState(boolean error) {
        errorState.set(error);
    }
   
    
    /* GETTERS */
    
    /**
     * @return The CustomUIPane this Conneciton is on.
     */
    public final CustomUIPane getPane() {
        return pane;
    }

    /** @return this connection's end anchor, if any. */
    public final Optional<InputAnchor> getInputAnchor() {
        return endAnchor;
    }

    /** @return the downstream block, if any. */
    public final Optional<Block> getInputBlock() {
        return endAnchor.map(ConnectionAnchor::getBlock);
    }

    /** @return this connection's start anchor, if any. */
    public final Optional<OutputAnchor> getOutputAnchor() {
        return startAnchor;
    }

    /** @return the upstream block, if any. */
    public final Optional<Block> getOutputBlock() {
        return startAnchor.map(ConnectionAnchor::getBlock);
    }
    
    /**
     * @return The position on the specified anchor local to the CustomUIPane at
     *         which the Line should start or end.
     */
    public Point2D getAnchorPositionInPane(ConnectionAnchor anchor) {
        return getPane().sceneToLocal(anchor.localToScene(anchor.getLocalCenter()));
    }
    
    /* SETTERS */

    /**
     * Sets an OutputAnchor or InputAnchor for this line. After setting the line
     * will update accordingly to the possible state change.
     */
    public void setAnchor(ConnectionAnchor newAnchor) {
        // Add the anchor.
        if (newAnchor instanceof OutputAnchor) {
            startAnchor.ifPresent(a -> disconnect(a));
            startAnchor = Optional.of((OutputAnchor) newAnchor);
        } else if (newAnchor instanceof InputAnchor) {
            endAnchor.ifPresent(a -> disconnect(a));
            endAnchor = Optional.of((InputAnchor) newAnchor);
        } else {
            throw new RuntimeException("InvalidAnchor");
        }
        
        // Add this to the anchor.
        newAnchor.addConnection(this);
        addListeners(newAnchor);

        if (isFullyConnected()) {
            // The ConnectionState changed.
            int state = ConnectionCreationManager.nextConnectionState();
            startAnchor.get().getBlock().updateConnectionState(state);
            endAnchor.get().getBlock().updateConnectionState(state);
        }
        invalidateAnchorPositions();
    }

    /**
     * Sets the free ends (empty anchors) to the specified position.
     * 
     * @param point
     *            Coordinates local to the Line's parent.
     */
    public void setFreeEnds(Point2D point) {
        if (!startAnchor.isPresent()) {
            setStartPositionParent(point);
        }
        if (!endAnchor.isPresent()) {
            setEndPositionParent(point);
        }
    }
    
    /**
     * Listener method that can be attached to a BooleanProperty in order to
     * update the error state based on that property.
     */
    private void checkErrorListener(ObservableValue<? extends Boolean> value, Boolean oldValue, Boolean newValue) {
        ObservableList<String> styleClass = this.getStyleClass();
        styleClass.removeAll("error");
        if (newValue) {
            styleClass.add("error");
        }
    }
    

    /* OTHER METHODS */

    /**
     * @return Whether or not both sides of this Connection are connected to an Anchor.
     */
    public final boolean isFullyConnected() {
        return startAnchor.isPresent() && endAnchor.isPresent();
    }

    /**
     * Properly disconnects the given anchor from this Connection, notifying the anchor of its disconnection.
     */
    public final void disconnect(ConnectionAnchor anchor) {
        boolean disconnected = false;
        boolean wasConnected = isFullyConnected();
        // Find out what anchor to disconnect, and do so.
        if (startAnchor.isPresent() && startAnchor.get().equals(anchor)) {
            startAnchor = Optional.empty();
            disconnected = true;
        } else if (endAnchor.isPresent() && endAnchor.get().equals(anchor)) {
            endAnchor = Optional.empty();
            disconnected = true;
        }
        
        if (disconnected) {
            // Fully disconnect the anchor from this Connection.
            removeListeners(anchor);
            anchor.disconnectConnection(this);
            
            if (wasConnected) {
                int state = ConnectionCreationManager.nextConnectionState();
    
                //Let the now disconnected anchor update its visuals.
                anchor.getBlock().updateConnectionState(state);
                //Let the remaining connected anchors update their visuals.
                startAnchor.ifPresent(a -> a.getBlock().updateConnectionState(state));
                endAnchor.ifPresent(a -> a.getBlock().updateConnectionState(state));
                this.setErrorState(false);
            }
        }
    }

    /**
     * Removes this Connection, disconnecting its anchors and removing this Connection from the pane it is on.
     */
    public final void remove() {
        startAnchor.ifPresent(a -> disconnect(a));
        endAnchor.ifPresent(a -> disconnect(a));
        getPane().getChildren().remove(this);
    }

    /**
     * Adds the listeners this Connections needs to keep its visual
     * representation up-to-date to the given anchor.
     */
    private void addListeners(ConnectionAnchor anchor) {
        anchor.localToSceneTransformProperty().addListener(this);
    }

    /**
     * Removes the listeners this Connections needed to keep its visual
     * representation up-to-date from the given anchor.
     */
    private void removeListeners(ConnectionAnchor anchor) {
        anchor.localToSceneTransformProperty().removeListener(this);
    }

    @Override
    public final void changed(ObservableValue<? extends Transform> observable,
            Transform oldValue, Transform newValue) {
        invalidateAnchorPositions();
    }

    /**
     * Runs both the update Start end End position functions. Use when
     * refreshing UI representation of the Line.
     */
    public void invalidateAnchorPositions() {
        startAnchor.ifPresent(a -> setStartPositionParent(getPane().sceneToLocal(a.localToScene(a.getLocalCenter()))));
        endAnchor.ifPresent(a -> setEndPositionParent(getPane().sceneToLocal(a.localToScene(a.getLocalCenter()))));
    }

    @Override
    public String toString() {
        return "Connection connecting \n(out) " + startAnchor + "   to\n(in)  "
                + endAnchor;
    }

    @Override
    public Map<String, Object> toBundle() {
        ImmutableMap.Builder<String, Object> bundle = ImmutableMap.builder();

        startAnchor.ifPresent(start -> {
            Block block = start.getBlock();
            bundle.put("startBlock", block.hashCode());
            bundle.put("startAnchor", 0);
        });

        endAnchor.ifPresent(end -> {
            Block block = end.getBlock();
            bundle.put("endBlock", block.hashCode());
            bundle.put("endAnchor", block.getAllInputs().indexOf(end));
        });

        return bundle.build();
    }
}