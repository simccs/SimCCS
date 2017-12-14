package gui;

import java.util.ArrayList;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;

/**
 *
 * @author yaw
 */
class NetworkDisplay extends Pane {

    private DoubleProperty scale = new SimpleDoubleProperty(1.0);
    private ControlActions controlActions;

    public NetworkDisplay() {
        // add scale transform
        scaleXProperty().bind(scale);
        scaleYProperty().bind(scale);
    }

    public double getScale() {
        return scale.get();
    }

    public void setScale(double scale) {
        this.scale.set(scale);
    }

    public void setPivot(double x, double y) {
        setTranslateX(getTranslateX() - x);
        setTranslateY(getTranslateY() - y);
    }

    public void setControlActions(ControlActions controlActions) {
        this.controlActions = controlActions;
    }
    
    public ControlActions getControlActions() {
        return controlActions;
    }
}

class DragContext {

    double mouseAnchorX;
    double mouseAnchorY;

    double translateAnchorX;
    double translateAnchorY;
}

class SceneGestures {

    private static final double MAX_SCALE = 100.0d;
    private static final double MIN_SCALE = 1d;

    private DragContext sceneDragContext = new DragContext();

    NetworkDisplay canvas;

    private ArrayList<Pane> entitiesToResize = new ArrayList<>();
    private double radius = 5;
    private double fontSize = 13;

    public SceneGestures(NetworkDisplay canvas) {
        this.canvas = canvas;
    }

    public void addEntityToResize(Pane p) {
        entitiesToResize.add(p);
    }

    public EventHandler<MouseEvent> getOnMousePressedEventHandler() {
        return onMousePressedEventHandler;
    }

    public EventHandler<MouseEvent> getOnMouseDraggedEventHandler() {
        return onMouseDraggedEventHandler;
    }

    public EventHandler<ScrollEvent> getOnScrollEventHandler() {
        return onScrollEventHandler;
    }

    // For testing.  Feel free to remove.
    public EventHandler<MouseEvent> getOnMouseMovedEventHandler() {
        return onMouseMovedEventHandler;
    }

    private EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>() {

        public void handle(MouseEvent event) {
            if (event.isPrimaryButtonDown()) {
                sceneDragContext.mouseAnchorX = event.getSceneX();
                sceneDragContext.mouseAnchorY = event.getSceneY();

                sceneDragContext.translateAnchorX = canvas.getTranslateX();
                sceneDragContext.translateAnchorY = canvas.getTranslateY();
            } else if (event.isSecondaryButtonDown()) {
                // Up for grabs.
                canvas.getControlActions().getMessenger().setText("Cell number: " + canvas.getControlActions().displayXYToVectorized(event.getX(), event.getY()));
                /*int cellNum = canvas.controlActions.displayXYToVectorized(event.getX(), event.getY());
                HashMap<Integer, HashSet<Integer>> neighbors = canvas.controlActions.getData().getGraphNeighbors();
                String n = "";
                if (neighbors.containsKey(cellNum)) {
                    HashSet<Integer> neighborCells = neighbors.get(cellNum);
                    for (int cell : neighborCells) {
                        n += Integer.toString(cell) + " ";
                    }
                } else {
                    n = "None.";
                }
                canvas.controlActions.getMessenger().setText(cellNum + " Neighbors: " + n);*/
            }
        }
    };

    private EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>() {
        public void handle(MouseEvent event) {
            canvas.setTranslateX(sceneDragContext.translateAnchorX + event.getSceneX() - sceneDragContext.mouseAnchorX);
            canvas.setTranslateY(sceneDragContext.translateAnchorY + event.getSceneY() - sceneDragContext.mouseAnchorY);

            event.consume();
        }
    };

    // For testing.  Feel free to remove.
    private EventHandler<MouseEvent> onMouseMovedEventHandler = new EventHandler<MouseEvent>() {
        public void handle(MouseEvent event) {
            System.out.println(event.getX() + ", " + event.getY() + ", " + event.getSceneX() + "," + event.getSceneY());

            event.consume();
        }
    };

    private EventHandler<ScrollEvent> onScrollEventHandler = new EventHandler<ScrollEvent>() {

        @Override
        public void handle(ScrollEvent event) {

            double delta = 1.2;

            double scale = canvas.getScale();
            double oldScale = scale;

            if (event.getDeltaY() < 0) {
                scale /= delta;
            } else {
                scale *= delta;
            }

            scale = clamp(scale, MIN_SCALE, MAX_SCALE);

            double f = (scale / oldScale) - 1;

            double dx = (event.getSceneX() - (canvas.getBoundsInParent().getWidth() / 2 + canvas.getBoundsInParent().getMinX()));
            double dy = (event.getSceneY() - (canvas.getBoundsInParent().getHeight() / 2 + canvas.getBoundsInParent().getMinY()));

            canvas.setScale(scale);

            canvas.setPivot(f * dx, f * dy);

            // Resize components based on zoom level.
            resizeComponents(scale, oldScale);

            event.consume();
        }

        private void resizeComponents(double newScale, double oldScale) {
            // Resize entities.
            for (Pane p : entitiesToResize) {
                for (Node n : p.getChildren()) {
                    if (n instanceof Circle) {
                        Circle c = (Circle) n;
                        double radius = c.getRadius() * oldScale;
                        c.setRadius(radius / newScale);
                    } else if (n instanceof javafx.scene.shape.Arc) {
                        Arc arc = (Arc) n;
                        double radius = arc.getRadiusX() * oldScale;
                        arc.setRadiusX(radius / newScale);
                        arc.setRadiusY(radius / newScale);
                    } else if (n instanceof Label) {
                        Label l = (Label) n;
                        l.setFont(new Font("System Regular", fontSize / Math.max(newScale / 4, 1)));
                        // TODO: Going to have to be more clever to shift labels...
                    } else if (n instanceof Line) {
                        Line l = (Line) n;
                        double radius = l.getStrokeWidth() * oldScale;
                        l.setStrokeWidth(radius / newScale);
                    }
                }
            }
        }
    };

    public static double clamp(double value, double min, double max) {
        if (Double.compare(value, min) < 0) {
            return min;
        }

        if (Double.compare(value, max) > 0) {
            return max;
        }

        return value;
    }
}
