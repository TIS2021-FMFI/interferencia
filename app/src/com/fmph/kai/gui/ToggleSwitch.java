package com.fmph.kai.gui;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.function.Function;

/**
 * The definition of the Toggle Switch GUI element and its behaviour.
 */
public class ToggleSwitch extends StackPane {
    private final Rectangle back = new Rectangle(40, 10, Color.RED);
    private final Button button = new Button();
    private final String buttonStyleOff = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 0.2, 0.0, 0.0, 2); -fx-background-color: WHITE;";
    private final String buttonStyleOn = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 0.2, 0.0, 0.0, 2); -fx-background-color: #00893d;";
    private boolean state;
    private OnEnabled onEnabled;
    private OnDisabled onDisabled;

    private void init() {
        getChildren().addAll(back, button);
        setMinSize(30, 15);
        back.minWidth(40);
        back.maxWidth(40);
        back.maxHeight(10);
        back.minHeight(10);
        back.setArcHeight(back.getHeight());
        back.setArcWidth(back.getHeight());
        back.setFill(Color.valueOf("#ced5da"));
        double r = 2.0;
        button.setShape(new Circle(r));
        setAlignment(button, Pos.CENTER_LEFT);
        button.setMaxSize(15, 15);
        button.setMinSize(15, 15);
        button.setStyle(buttonStyleOff);
    }

    /**
     * Initializes the element and click behaviour.
     */
    public ToggleSwitch() {
        init();
        EventHandler<Event> click = new EventHandler<Event>() {
            @Override
            public void handle(Event e) {
                if (state) {
                    button.setStyle(buttonStyleOff);
                    back.setFill(Color.valueOf("#ced5da"));
                    setAlignment(button, Pos.CENTER_LEFT);
                    state = false;
                    onDisabled.invoke();
                } else {
                    button.setStyle(buttonStyleOn);
                    back.setFill(Color.valueOf("#80C49E"));
                    setAlignment(button, Pos.CENTER_RIGHT);
                    state = true;
                    onEnabled.invoke();
                }
            }
        };

        button.setFocusTraversable(false);
        setOnMouseClicked(click);
        button.setOnMouseClicked(click);
    }

    /**
     * Sets the actions to be performed when the toggle is in 'On' position.
     * @param onEnabled is the callback function
     */
    public void setOnEnabled(OnEnabled onEnabled) {
        this.onEnabled = onEnabled;
    }

    /**
     * Sets the actions to be performed when the toggle is in 'Off' position.
     * @param onDisabled is the callback function
     */
    public void setOnDisabled(OnDisabled onDisabled) {
        this.onDisabled = onDisabled;
    }

    /**
     * Defines the function to be invoked on the switch to the active state.
     */
    public interface OnEnabled {
        void invoke();
    }

    /**
     * Defines the function to be invoked on the switch to the disabled state.
     */
    public interface OnDisabled {
        void invoke();
    }
}