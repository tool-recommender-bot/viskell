package nl.utwente.viskell.ui.components;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import nl.utwente.viskell.ghcj.GhciSession;
import nl.utwente.viskell.haskell.expr.Expression;
import nl.utwente.viskell.haskell.type.HaskellTypeError;
import nl.utwente.viskell.haskell.type.Type;
import nl.utwente.viskell.haskell.type.TypeChecker;
import nl.utwente.viskell.haskell.type.TypeScope;
import nl.utwente.viskell.ui.ToplevelPane;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * DisplayBlock is an extension of {@link Block} that only provides a display of
 * the input it receives through it's {@link InputAnchor}. The input will be
 * rendered visually on the Block. DisplayBlock can be empty and contain no
 * value at all, the value can be altered at any time by providing a different
 * input source using a {@link Connection}.
 */
public class DisplayBlock extends Block implements ConnectionAnchor.Target {

    /** The Anchor that is used as input. */
    protected InputAnchor inputAnchor;

    /** The space containing the input anchor. */
    @FXML protected Pane inputSpace;

    /** The label on which to display the value of this block */
    @FXML protected Label value;
    
    /** Show class constrained type variable for the input anchor */
    private final Type showConstraint;
            
    /**
     * Creates a new instance of DisplayBlock.
     * @param pane
     *            The pane on which this DisplayBlock resides.
     */
    public DisplayBlock(ToplevelPane pane) {
        this(pane, "DisplayBlock");
    }
    
    protected DisplayBlock(ToplevelPane pane, String fxml) {
        super(pane);
        this.showConstraint = pane.getEnvInstance().buildType("Show a => a");
        loadFXML(fxml);

        inputAnchor = new InputAnchor(this);
        inputSpace.getChildren().add(0, inputAnchor);
    }

    @Override
    public void invalidateVisualState() {
        this.inputAnchor.invalidateVisualState();

        if (inputAnchor.hasValidConnection()) {
            try { 
                TypeChecker.unify("is showable", inputAnchor.getType().getFresh(), showConstraint.getFresh());
            
                GhciSession ghci = getToplevel().getGhciSession();

                ListenableFuture<String> result = ghci.pull(inputAnchor.getFullExpr());

                Futures.addCallback(result, new FutureCallback<String>() {
                    public void onSuccess(String s) {
                        // Can't call setOutput directly - this may not be JavaFX app thread.
                        // Instead, schedule setting the output.
                        Platform.runLater(() -> value.setText(s));
                    }

                    public void onFailure(Throwable throwable) {
                        Platform.runLater(() -> value.setText("?!?!?!"));
                    }
                });

            } catch (HaskellTypeError e) {
                value.setText("_ :: " + inputAnchor.getStringType());
            }
            
        } else {
            value.setText("?");
        }
    }
    
    //TODO NOTE: only used for a meaningless test
    public String getOutput() {
        return value.getText();
    }
    
    @Override
    public ConnectionAnchor getAssociatedAnchor() {
        return inputAnchor;
    }

    @Override
    public List<InputAnchor> getAllInputs() {
        return ImmutableList.of(inputAnchor);
    }

    @Override
    public List<OutputAnchor> getAllOutputs() {
        return ImmutableList.of();
    }
    
    @Override
    public Optional<Block> getNewCopy() {
        return Optional.of(new DisplayBlock(this.getToplevel()));
    }
    
    @Override
    public Expression getLocalExpr(Set<OutputAnchor> outsideAnchors) {
        return inputAnchor.getLocalExpr(outsideAnchors);
    }
    
    @Override
    public void refreshAnchorTypes() {
        this.inputAnchor.setFreshRequiredType(TypeScope.unique("q"), new TypeScope());        
    }

    @Override
    public String toString() {
        return "DisplayBlock[" + value.getText() + "]";
    }

}
