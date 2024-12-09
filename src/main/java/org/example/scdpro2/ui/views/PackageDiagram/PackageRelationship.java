package org.example.scdpro2.ui.views.PackageDiagram;

import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import org.example.scdpro2.business.models.BPackageDiagarm.BPackageRelationShip;
/**
 * Represents a relationship between two packages in a diagram.
 * Displays a connection between two packages using dotted lines and an arrow, along with draggable anchors and a relationship label.
 *
 * @param <T> The type of package that the relationship connects, extending {@link javafx.scene.layout.BorderPane}.
 */
public class PackageRelationship<T extends javafx.scene.layout.BorderPane> extends javafx.scene.Node {
    private final Line horizontalLine;
    private final Line verticalLine;
    private final Polygon arrow;
    private final Label relationshipLabel;

    public final T startPackage;
    public final T endPackage;

    private final Pane diagramPane;

    private final Circle startAnchor;
    private final Circle endAnchor;
    private final Circle intersectionAnchor;  // New anchor for the intersection point

    public Point2D startAnchorPosition;
    public Point2D endAnchorPosition;
    public Point2D intersectionAnchorPosition;
    private boolean isLineSelected = false;
    private Color originalColor = Color.BLACK;

    private static final int SNAP_INCREMENT = 5;

    private BPackageRelationShip bPackageRelationShip;
    /**
     * Sets the label text for the relationship.
     *
     * @param text The text to display as the relationship label
     */
    public void setRelationshipLabel(String text)
    {
        relationshipLabel.setText(text);
    }

    /**
     * Constructs a new package relationship between two packages.
     *
     * @param diagramPane The pane where the relationship is drawn
     * @param startPackage The starting package in the relationship
     * @param endPackage The ending package in the relationship
     */
    public PackageRelationship(Pane diagramPane, T startPackage, T endPackage) {
        this.diagramPane = diagramPane;
        this.startPackage = startPackage;
        this.endPackage = endPackage;

        // Create an instance of BPackageRelationship

        // Create lines
        horizontalLine = new Line();
        verticalLine = new Line();
        horizontalLine.setStroke(Color.BLACK);
        verticalLine.setStroke(Color.BLACK);
        horizontalLine.getStrokeDashArray().addAll(5d, 5d);  // Make it dotted
        verticalLine.getStrokeDashArray().addAll(5d, 5d);    // Make it dotted

        // Create arrow
        arrow = new Polygon();
        arrow.getPoints().addAll(0.0, 0.0, -10.0, 5.0, -10.0, -5.0);
        arrow.setFill(Color.BLACK);

        // Create relationship label
        relationshipLabel = new Label("Relation");
        relationshipLabel.setStyle("-fx-font-size: 12px; -fx-background-color: white;");

        // Create draggable anchors
        startAnchor = new Circle(10, Color.TRANSPARENT);
        endAnchor = new Circle(10, Color.TRANSPARENT);
        intersectionAnchor = new Circle(5, Color.BLACK); // Intersection anchor

        // Add to diagram pane
        diagramPane.getChildren().addAll(horizontalLine, verticalLine, arrow, relationshipLabel, startAnchor, endAnchor, intersectionAnchor);

        // Initialize positions
        startAnchorPosition = new Point2D(startPackage.getLayoutX(), startPackage.getLayoutY());
        endAnchorPosition = new Point2D(endPackage.getLayoutX() + endPackage.getWidth(), endPackage.getLayoutY());
        intersectionAnchorPosition = new Point2D((startAnchorPosition.getX() + endAnchorPosition.getX()) / 2,
                (startAnchorPosition.getY() + endAnchorPosition.getY()) / 2);


        // Set initial anchor positions
        updateAnchorsToPackageBoundaries();

        // Update line positions
        updateLines();


        // Listeners to adjust dynamically when packages are moved
        startPackage.layoutXProperty().addListener((obs, oldVal, newVal) -> updateAnchorsToPackageBoundaries());
        startPackage.layoutYProperty().addListener((obs, oldVal, newVal) -> updateAnchorsToPackageBoundaries());
        endPackage.layoutXProperty().addListener((obs, oldVal, newVal) -> updateAnchorsToPackageBoundaries());
        endPackage.layoutYProperty().addListener((obs, oldVal, newVal) -> updateAnchorsToPackageBoundaries());

        addPackageMovementListeners();
        // Add drag listeners for anchors
        addAnchorDragListeners();
        addIntersectionAnchorDragListener();  // Add drag listener for intersection anchor

        // Add mouse event listeners for lines
        addClickListeners();
        addRightClickListeners();
    }

    // Synchronize changes with the BPackageRelationship model
    /**
     * Handles a click event on the relationship line to toggle its selection.
     *
     * @param event The mouse event associated with the click
     * @param line The line being clicked
     */
    private void handleLineClick(MouseEvent event, Line line) {
        if (event.getClickCount() == 1) {  // Single click
            if (isLineSelected) {
                // Toggle color to revert
                line.setStroke(originalColor);
                horizontalLine.setStroke(originalColor);
                verticalLine.setStroke(originalColor);
                isLineSelected = false;
            } else {
                // Change color
                originalColor = (Color) line.getStroke();  // Save current color
                line.setStroke(Color.RED);  // Change color to red
                horizontalLine.setStroke(Color.RED);
                verticalLine.setStroke(Color.RED);
                isLineSelected = true;
            }
        }
    }
    /**
     * Deletes the lines, anchors, and relationship label from the diagram.
     */
    private void deleteLinesAndAnchors() {
        diagramPane.getChildren().remove(horizontalLine);
        diagramPane.getChildren().remove(verticalLine);
        diagramPane.getChildren().remove(arrow);
        diagramPane.getChildren().remove(startAnchor);
        diagramPane.getChildren().remove(endAnchor);
        diagramPane.getChildren().remove(intersectionAnchor);
        diagramPane.getChildren().remove(relationshipLabel);
    }
    /**
     * Displays a confirmation alert to delete the relationship lines and anchors.
     */
    private void showDeleteConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Lines");
        alert.setHeaderText("Are you sure you want to delete both lines and anchors?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteLinesAndAnchors();
            }
        });
    }
    /**
     * Updates the positions of the anchors relative to the package boundaries.
     */
    private void updateAnchorsToPackageBoundaries() {
        // Update start anchor
        startAnchorPosition = getClosestEdgePoint(startAnchor, startPackage);

        // Update end anchor
        endAnchorPosition = getClosestEdgePoint(endAnchor, endPackage);

        // Calculate intersection anchor position dynamically
        intersectionAnchorPosition = calculateIntersectionAnchorPosition();

        // Update anchor visuals
        updateAnchorVisuals();

        // Update lines and arrow
        updateLines();
    }
    /**
     * Updates the visual position of the anchors.
     */
    private void updateAnchorVisuals() {
        startAnchor.setCenterX(startAnchorPosition.getX());
        startAnchor.setCenterY(startAnchorPosition.getY());
        endAnchor.setCenterX(endAnchorPosition.getX());
        endAnchor.setCenterY(endAnchorPosition.getY());
        intersectionAnchor.setCenterX(intersectionAnchorPosition.getX());
        intersectionAnchor.setCenterY(intersectionAnchorPosition.getY());
    }

    /**
     * Updates the positions of the lines and the arrow based on the anchors.
     */
    private void updateLines() {
        // Update horizontal line
        horizontalLine.setStartX(startAnchorPosition.getX());
        horizontalLine.setStartY(startAnchorPosition.getY());
        horizontalLine.setEndX(endAnchorPosition.getX());
        horizontalLine.setEndY(startAnchorPosition.getY());

        // Update vertical line
        verticalLine.setStartX(endAnchorPosition.getX());
        verticalLine.setStartY(startAnchorPosition.getY());
        verticalLine.setEndX(endAnchorPosition.getX());
        verticalLine.setEndY(endAnchorPosition.getY());

        // Calculate intersection point
        double intersectionX = endAnchorPosition.getX();
        double intersectionY = startAnchorPosition.getY();
        intersectionAnchorPosition = new Point2D(intersectionX, intersectionY);

        // Update intersection circle
        intersectionAnchor.setCenterX(intersectionX);
        intersectionAnchor.setCenterY(intersectionY);

        // Update arrow position relative to intersection anchor
        arrow.setLayoutX(endAnchorPosition.getX());
        arrow.setLayoutY(endAnchorPosition.getY());
        arrow.setRotate(calculateArrowAngle(intersectionAnchorPosition.getX(), intersectionAnchorPosition.getY(),
                endAnchorPosition.getX(), endAnchorPosition.getY()));


        // Update label position
        relationshipLabel.setLayoutX(intersectionX + 10); // Offset slightly for readability
        relationshipLabel.setLayoutY(intersectionY - 10); // Offset slightly for readability

    }
    /**
     * Gets the BPackageRelationShip model associated with this relationship.
     *
     * @return The BPackageRelationShip model
     */
    public BPackageRelationShip getBPackageRelationShip() {
        return bPackageRelationShip;
    }

    // Helper Functions
    /**
     * Calculates the position of the intersection anchor based on the positions of the start and end anchors.
     *
     * @return The position of the intersection anchor
     */
    private Point2D calculateIntersectionAnchorPosition() {
        // Midpoint between start and end anchors
        double midX = (startAnchorPosition.getX() + endAnchorPosition.getX()) / 2;
        double midY = (startAnchorPosition.getY() + endAnchorPosition.getY()) / 2;
        return new Point2D(midX, midY);
    }
    /**
     * Calculates the angle of the arrow based on the positions of the intersection anchor and the end anchor.
     *
     * @param startX The X coordinate of the intersection anchor
     * @param startY The Y coordinate of the intersection anchor
     * @param endX The X coordinate of the end anchor
     * @param endY The Y coordinate of the end anchor
     * @return The angle of the arrow in degrees
     */
    private double calculateArrowAngle(double startX, double startY, double endX, double endY) {
        return Math.toDegrees(Math.atan2(endY - startY, endX - startX));
    }
    /**
     * Gets the closest edge point of the package box relative to the anchor position.
     *
     * @param anchor The anchor circle
     * @param packageBox The package box to check against
     * @return The closest edge point as a {@link javafx.geometry.Point2D}
     */
    private Point2D getClosestEdgePoint(Circle anchor, T packageBox) {
        double boxLeft = packageBox.getLayoutX();
        double boxRight = boxLeft + packageBox.getWidth();
        double boxTop = packageBox.getLayoutY();
        double boxBottom = boxTop + packageBox.getHeight();

        double anchorX = anchor.getCenterX();
        double anchorY = anchor.getCenterY();

        // Find the closest edge point
        double distanceToLeft = Math.abs(anchorX - boxLeft);
        double distanceToRight = Math.abs(anchorX - boxRight);
        double distanceToTop = Math.abs(anchorY - boxTop);
        double distanceToBottom = Math.abs(anchorY - boxBottom);

        if (distanceToLeft <= distanceToRight && distanceToLeft <= distanceToTop && distanceToLeft <= distanceToBottom) {
            // Closest to the left edge
            return new Point2D(boxLeft, Math.min(Math.max(anchorY, boxTop), boxBottom));
        } else if (distanceToRight <= distanceToLeft && distanceToRight <= distanceToTop && distanceToRight <= distanceToBottom) {
            // Closest to the right edge
            return new Point2D(boxRight, Math.min(Math.max(anchorY, boxTop), boxBottom));
        } else if (distanceToTop <= distanceToLeft && distanceToTop <= distanceToRight && distanceToTop <= distanceToBottom) {
            // Closest to the top edge
            return new Point2D(Math.min(Math.max(anchorX, boxLeft), boxRight), boxTop);
        } else {
            // Closest to the bottom edge
            return new Point2D(Math.min(Math.max(anchorX, boxLeft), boxRight), boxBottom);
        }
    }
    /**
     * Snaps the given value to the nearest increment.
     *
     * @param value The value to snap
     * @return The snapped value
     */
    private double snapToIncrement(double value) {
        return Math.round(value / SNAP_INCREMENT) * SNAP_INCREMENT;
    }
    // Listeners
    /**
     * Adds right-click event listeners for the relationship lines to handle deletion.
     */
    private void addRightClickListeners() {
        horizontalLine.setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown() && event.getClickCount() == 2) {  // Double right-click
                showDeleteConfirmation();
            }
        });

        verticalLine.setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown() && event.getClickCount() == 2) {  // Double right-click
                showDeleteConfirmation();
            }
        });
    }
    /**
     * Adds click event listeners for the relationship lines to toggle selection.
     */
    private void addClickListeners() {
        horizontalLine.setOnMouseClicked(event -> handleLineClick(event, horizontalLine));
        verticalLine.setOnMouseClicked(event -> handleLineClick(event, verticalLine));
    }
    /**
     * Adds a drag event listener for an anchor to allow movement along the package boundary.
     *
     * @param anchor The anchor to add the listener to
     * @param packageBox The package box to constrain the movement within
     */
    private void addAnchorDragListener(Circle anchor, T packageBox) {
        anchor.setOnMouseDragged(event -> {
            double anchorX = event.getX();
            double anchorY = event.getY();

            // Get package boundaries
            double boxLeft = packageBox.getLayoutX();
            double boxRight = boxLeft + packageBox.getWidth();
            double boxTop = packageBox.getLayoutY();
            double boxBottom = boxTop + packageBox.getHeight();

            // Determine the closest edge and constrain the anchor to that edge
            double distanceToLeft = Math.abs(anchorX - boxLeft);
            double distanceToRight = Math.abs(anchorX - boxRight);
            double distanceToTop = Math.abs(anchorY - boxTop);
            double distanceToBottom = Math.abs(anchorY - boxBottom);

            if (distanceToLeft <= distanceToRight && distanceToLeft <= distanceToTop && distanceToLeft <= distanceToBottom) {
                // Closest to the left edge
                anchorX = boxLeft;
                anchorY = Math.min(Math.max(anchorY, boxTop), boxBottom);
            } else if (distanceToRight <= distanceToLeft && distanceToRight <= distanceToTop && distanceToRight <= distanceToBottom) {
                // Closest to the right edge
                anchorX = boxRight;
                anchorY = Math.min(Math.max(anchorY, boxTop), boxBottom);
            } else if (distanceToTop <= distanceToLeft && distanceToTop <= distanceToRight && distanceToTop <= distanceToBottom) {
                // Closest to the top edge
                anchorY = boxTop;
                anchorX = Math.min(Math.max(anchorX, boxLeft), boxRight);
            } else {
                // Closest to the bottom edge
                anchorY = boxBottom;
                anchorX = Math.min(Math.max(anchorX, boxLeft), boxRight);
            }

            // Update anchor position
            if (anchor == startAnchor) {
                startAnchorPosition = new Point2D(anchorX, anchorY);
            } else if (anchor == endAnchor) {
                endAnchorPosition = new Point2D(anchorX, anchorY);
            }

            anchor.setCenterX(anchorX);
            anchor.setCenterY(anchorY);

            updateLines();
        });
    }
    /**
     * Adds a drag event listener for the intersection anchor to allow movement.
     */
    private void addIntersectionAnchorDragListener() {
        intersectionAnchor.setOnMouseDragged(event -> {
            double newX = snapToIncrement(event.getX());
            double newY = snapToIncrement(event.getY());

            intersectionAnchorPosition = new Point2D(newX, newY);
            intersectionAnchor.setCenterX(newX);
            intersectionAnchor.setCenterY(newY);

            updateLines();
        });
    }
    /**
     * Adds drag event listeners for both start and end anchors.
     */
    private void addAnchorDragListeners() {
        addAnchorDragListener(startAnchor, startPackage);
        addAnchorDragListener(endAnchor, endPackage);
    }
    /**
     * Adds listeners to adjust the anchors when the associated packages are moved.
     */
    private void addPackageMovementListeners() {
        startPackage.layoutXProperty().addListener((obs, oldVal, newVal) -> updateAnchorsToPackageBoundaries());
        startPackage.layoutYProperty().addListener((obs, oldVal, newVal) -> updateAnchorsToPackageBoundaries());
        endPackage.layoutXProperty().addListener((obs, oldVal, newVal) -> updateAnchorsToPackageBoundaries());
        endPackage.layoutYProperty().addListener((obs, oldVal, newVal) -> updateAnchorsToPackageBoundaries());
    }

    // getters setters
    /**
     * Gets the start anchor of the relationship.
     *
     * @return The start anchor circle
     */
    public Circle getStartAnchor() {
        return startAnchor;
    }
    /**
     * Gets the end anchor of the relationship.
     *
     * @return The end anchor circle
     */
    public Circle getEndAnchor() {
        return endAnchor;
    }
    /**
     * Gets the intersection anchor of the relationship.
     *
     * @return The intersection anchor circle
     */
    public Circle getIntersectionAnchor() {
        return intersectionAnchor;
    }

    /**
     * Gets the start package of the relationship.
     *
     * @return The start package
     */
    public T getStartPackage() {
        return startPackage;
    }
    /**
     * Gets the end package of the relationship.
     *
     * @return The end package
     */
    public T getEndPackage() {
        return  endPackage;
    }

    /**
     * Gets the horizontal line representing the relationship.
     *
     * @return The horizontal line
     */
    public Line getHorizontalLine() {
        return horizontalLine;
    }
    /**
     * Gets the vertical line representing the relationship.
     *
     * @return The vertical line
     */
    public Line getVerticalLine() {
        return verticalLine;
    }

    public Polygon getArrow() {
        return arrow;
    }

    public Label getRelationshipLabel() {
        return relationshipLabel;
    }
}