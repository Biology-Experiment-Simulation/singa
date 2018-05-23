package de.bioforscher.singa.javafx.geometry;

import de.bioforscher.singa.javafx.renderer.Renderer;
import de.bioforscher.singa.mathematics.geometry.faces.Circle;
import de.bioforscher.singa.mathematics.geometry.faces.Rectangle;
import de.bioforscher.singa.mathematics.vectors.Vector2D;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Iterator;
import java.util.Set;

/**
 * @author cl
 */
public class IntersectionPlayground extends Application implements Renderer {

    private Canvas canvas;


    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {

        canvas = new Canvas(500, 500);

        BorderPane root = new BorderPane();
        root.setCenter(canvas);

        Rectangle rectangle = new Rectangle(new Vector2D(100, 200), new Vector2D(200,100));
        drawPolygon(rectangle);

        Circle circle = new Circle(new Vector2D(120, 120), 25);
        drawCircle(circle);

        Set<Vector2D> intersections = rectangle.getIntersections(circle);
        intersections.forEach(System.out::println);
        getGraphicsContext().setLineWidth(6);
        getGraphicsContext().setFill(Color.GREEN);
        intersections.forEach(this::drawPoint);
        Iterator<Vector2D> iterator = intersections.iterator();
        Vector2D first = iterator.next();
        Vector2D second = iterator.next();

        System.out.println(circle.getCircumference());
        System.out.println(circle.arcLengthBetweenPoints(first, second));

        // show
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    @Override
    public GraphicsContext getGraphicsContext() {
        return canvas.getGraphicsContext2D();
    }

    @Override
    public double getDrawingWidth() {
        return canvas.getWidth();
    }

    @Override
    public double getDrawingHeight() {
        return canvas.getHeight();
    }

}
