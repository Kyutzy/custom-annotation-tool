package annotation_tool.runner;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import annotation_tool.services.LocateFile;

public class PrimaryController {

    @FXML
    private Canvas canvas;
    @FXML
    private ComboBox<String> child_folders;
    @FXML
    private TextField parent_folder;
    @FXML
    private Button save_btn;
    @FXML
    private ListView<String> all_contours_found;
    @FXML
    private Button locate;
    @FXML
    private Button output;
    @FXML
    private TextField output_folder;

    private double startX, startY;
    private Image backgroundImage;
    private double imageWidth, imageHeight, canvasWidth, canvasHeight;
    private double scaleX, scaleY;

    private int indexOfImageOnFolder = 0; // Index of the currently loaded image
    private LocateFile locator = new LocateFile();
    private int classNumber = -1;
    private double rotationAngle = 0;

    private Path currentImagePath;

    // List to store multiple annotations
    private List<RectangleData> rectangles = new ArrayList<>();
    private RectangleData lastRectangle = null;

    // Initialization method
    public void initialize() {
        // Initialize canvas dimensions
        canvasWidth = canvas.getWidth();
        canvasHeight = canvas.getHeight();

        // Set up event listeners
        save_btn.setOnMouseClicked(this::onSaveClicked);
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);

        // Capture keyboard events
        canvas.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.S) {
                        onSaveClicked(null);  // Call the save method
                    }
                });
            }
        });
        
        canvas.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.R) {  // Change to 'S' for the save action
                    	rotateImage(90);  // Call the save method
                    }
                });
            }
        });
    }


    @FXML
    private void parentFolderClicked() {
        String path = locator.locateFolder();
        parent_folder.setText(path);
        List<String> childFolders = locator.globsFolder(path);
        child_folders.setItems(FXCollections.observableArrayList(childFolders));
        child_folders.getSelectionModel().selectFirst();
    }

    @FXML
    private void outputFolderClicked() {
        String path = locator.locateFolder();
        output_folder.setText(path);
    }

    @FXML
    private void onRotateButtonClicked() {
        rotateImage(90);  // Rotate by 15 degrees each time the button is clicked
    }

    private void handleMousePressed(MouseEvent event) {
        // Capture the starting point of the rectangle
        startX = event.getX();
        startY = event.getY();
    }

    private void handleMouseDragged(MouseEvent event) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Calculate rectangle coordinates adjusted for rotation
        double[] adjustedCoords = getRotatedCoordinates(startX, startY, event.getX(), event.getY());

        // Draw the rotated image and rectangle
        drawRotatedImageAndShapes(gc, () -> {
            gc.setStroke(Color.BLUE);
            gc.setLineWidth(2);
            gc.strokeRect(adjustedCoords[0], adjustedCoords[1], adjustedCoords[2], adjustedCoords[3]);
        });
    }

    private void handleMouseReleased(MouseEvent event) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Calculate rectangle coordinates adjusted for rotation
        double[] adjustedCoords = getRotatedCoordinates(startX, startY, event.getX(), event.getY());

        // Draw the rotated image and rectangle
        drawRotatedImageAndShapes(gc, () -> {
            gc.setStroke(Color.BLUE);
            gc.setLineWidth(2);
            gc.strokeRect(adjustedCoords[0], adjustedCoords[1], adjustedCoords[2], adjustedCoords[3]);
        });

        // Calculate image coordinates for the rectangle (considering rotation)
        RectangleData rectData = calculateImageCoordinates(startX, startY, event.getX(), event.getY());

        // Store the rectangle data
        rectangles.add(rectData);
        lastRectangle = rectData;
    }

    @FXML
    public void optionChanged() {
    	indexOfImageOnFolder = 0;
    	classNumber++;
    	loadImage();
    	System.out.println(classNumber);
    }
    
    public void loadImage() {
        // Retrieve images from the selected folder with .jpg extension
        String selectedFolder = child_folders.getSelectionModel().getSelectedItem();

        if (selectedFolder == null) {
            System.out.println("No folder selected.");
            return;
        }

        List<String> images = locator.globsImages(selectedFolder);

        if (images.isEmpty()) {
            System.out.println("No images found in the selected folder.");
            return;
        }

        // Update image path with the current index
        currentImagePath = Paths.get(images.get(indexOfImageOnFolder));
        backgroundImage = new Image(currentImagePath.toUri().toString());

        if (backgroundImage.isError()) {
            System.out.println("Error loading the image.");
            return;
        }

        // Update image dimensions and scaling factors
        imageWidth = backgroundImage.getWidth();
        imageHeight = backgroundImage.getHeight();
        scaleX = imageWidth / canvasWidth;
        scaleY = imageHeight / canvasHeight;

        // Reset rotation angle
        rotationAngle = 0;

        // Clear existing annotations
        rectangles.clear();
        lastRectangle = null;

        // Draw the background image on the canvas
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawRotatedImageAndShapes(gc, null);
    }

    public void rotateImage(double angle) {
        rotationAngle += angle;  // Update the rotation angle

        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Draw the rotated image without any shapes
        drawRotatedImageAndShapes(gc, () -> {
            // Redraw existing rectangles
            gc.setStroke(Color.BLUE);
            gc.setLineWidth(2);
            for (RectangleData rect : rectangles) {
                // Adjust rectangle coordinates back to canvas space
                double[] adjustedCoords = getCanvasCoordinatesFromImage(rect);
                gc.strokeRect(adjustedCoords[0], adjustedCoords[1], adjustedCoords[2], adjustedCoords[3]);
            }
        });
    }

    private void drawRotatedImageAndShapes(GraphicsContext gc, Runnable drawShapes) {
        // Clear the canvas
        gc.clearRect(0, 0, canvasWidth, canvasHeight);

        // Save the state to apply rotation
        gc.save();

        // Calculate image center coordinates
        double imageCenterX = canvasWidth / 2;
        double imageCenterY = canvasHeight / 2;

        // Move the origin to the center of the canvas and apply rotation
        gc.translate(imageCenterX, imageCenterY);
        gc.rotate(rotationAngle);
        gc.translate(-imageCenterX, -imageCenterY);

        // Draw the image scaled to canvas size
        gc.drawImage(backgroundImage, 0, 0, canvasWidth, canvasHeight);

        // Draw shapes (rectangles) with the rotation applied
        if (drawShapes != null) {
            drawShapes.run();
        }

        // Restore the original state to reset transformations
        gc.restore();
    }

    private double[] getRotatedCoordinates(double x1, double y1, double x2, double y2) {
        // Calculate the center of the canvas
        double centerX = canvasWidth / 2;
        double centerY = canvasHeight / 2;

        // Adjust points to the rotated coordinate system
        Point2D p1 = rotatePoint(x1, y1, centerX, centerY, -rotationAngle);
        Point2D p2 = rotatePoint(x2, y2, centerX, centerY, -rotationAngle);

        // Calculate rectangle parameters
        double rectX = Math.min(p1.getX(), p2.getX());
        double rectY = Math.min(p1.getY(), p2.getY());
        double rectWidth = Math.abs(p1.getX() - p2.getX());
        double rectHeight = Math.abs(p1.getY() - p2.getY());

        return new double[]{rectX, rectY, rectWidth, rectHeight};
    }

    private Point2D rotatePoint(double x, double y, double pivotX, double pivotY, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees);
        double sin = Math.sin(angleRadians);
        double cos = Math.cos(angleRadians);

        // Translate point to origin
        x -= pivotX;
        y -= pivotY;

        // Rotate point
        double xNew = x * cos - y * sin;
        double yNew = x * sin + y * cos;

        // Translate point back
        x = xNew + pivotX;
        y = yNew + pivotY;

        return new Point2D(x, y);
    }

    private RectangleData calculateImageCoordinates(double startX, double startY, double endX, double endY) {
        // Adjust the points for rotation
        double centerX = canvasWidth / 2;
        double centerY = canvasHeight / 2;
        Point2D pStart = rotatePoint(startX, startY, centerX, centerY, -rotationAngle);
        Point2D pEnd = rotatePoint(endX, endY, centerX, centerY, -rotationAngle);

        // Translate canvas coordinates to image coordinates
        double imageStartX = pStart.getX() * scaleX;
        double imageStartY = pStart.getY() * scaleY;
        double imageEndX = pEnd.getX() * scaleX;
        double imageEndY = pEnd.getY() * scaleY;
        double imageRectX = Math.min(imageStartX, imageEndX);
        double imageRectY = Math.min(imageStartY, imageEndY);
        double imageRectWidth = Math.abs(imageEndX - imageStartX);
        double imageRectHeight = Math.abs(imageEndY - imageStartY);

        // Print the coordinates on the actual image
        System.out.println("Rectangle on actual image:");
        System.out.println("Start: (" + imageRectX + ", " + imageRectY + ")");
        System.out.println("Width: " + imageRectWidth + ", Height: " + imageRectHeight);

        return new RectangleData(imageRectX, imageRectY, imageRectWidth, imageRectHeight);
    }

    private void onSaveClicked(MouseEvent event) {
        if (rectangles.isEmpty()) {
            System.out.println("No annotations to save.");
            return;
        }

        // Prepare YOLO data
        StringBuilder yoloData = new StringBuilder();
        for (RectangleData rect : rectangles) {
            yoloData.append(getYOLOFormattedData(rect)).append("\n");
        }

        // Define the output file path
        String imagePath = currentImagePath.toString();
        String txtFilePath = imagePath.substring(0, imagePath.lastIndexOf('.')) + ".txt";
        System.out.println(txtFilePath);

        // Write data to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(txtFilePath))) {
            writer.write(yoloData.toString());
            System.out.println("YOLO data saved to " + txtFilePath);
        } catch (IOException e) {
            System.err.println("Error saving YOLO data: " + e.getMessage());
        }
        indexOfImageOnFolder++;
        loadImage();
    }

    private String getYOLOFormattedData(RectangleData rect) {
        // Class ID (e.g., 0 if you have only one class)
        int classId = classNumber;

        // Normalize the coordinates
        double xCenter = (rect.x + rect.width / 2) / imageWidth;
        double yCenter = (rect.y + rect.height / 2) / imageHeight;
        double widthNorm = rect.width / imageWidth;
        double heightNorm = rect.height / imageHeight;

        // Ensure values are between 0 and 1
        xCenter = clamp(xCenter, 0, 1);
        yCenter = clamp(yCenter, 0, 1);
        widthNorm = clamp(widthNorm, 0, 1);
        heightNorm = clamp(heightNorm, 0, 1);

        // Format the data
        return String.format("%d %.6f %.6f %.6f %.6f", classId, xCenter, yCenter, widthNorm, heightNorm);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // Inner class for rectangle data
    private static class RectangleData {
        double x, y, width, height;

        RectangleData(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    // Helper method to adjust rectangle coordinates back to canvas space for redrawing after rotation
    private double[] getCanvasCoordinatesFromImage(RectangleData rect) {
        // Convert image coordinates back to canvas coordinates
        double x1 = rect.x / scaleX;
        double y1 = rect.y / scaleY;
        double x2 = (rect.x + rect.width) / scaleX;
        double y2 = (rect.y + rect.height) / scaleY;

        // Rotate points
        double centerX = canvasWidth / 2;
        double centerY = canvasHeight / 2;
        Point2D p1 = rotatePoint(x1, y1, centerX, centerY, rotationAngle);
        Point2D p2 = rotatePoint(x2, y2, centerX, centerY, rotationAngle);

        // Calculate rectangle parameters
        double rectX = Math.min(p1.getX(), p2.getX());
        double rectY = Math.min(p1.getY(), p2.getY());
        double rectWidth = Math.abs(p1.getX() - p2.getX());
        double rectHeight = Math.abs(p1.getY() - p2.getY());

        return new double[]{rectX, rectY, rectWidth, rectHeight};
    }
}
