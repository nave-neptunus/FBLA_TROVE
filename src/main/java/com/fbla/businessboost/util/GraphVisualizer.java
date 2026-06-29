package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A force-directed graph visualizer for the Smart Search tab.
 * Simulates physics to arrange a central keyword node and surrounding business nodes.
 */
public class GraphVisualizer {

    private final Pane pane;
    private AnimationTimer timer;
    
    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<Business, Node> businessNodes = new HashMap<>();

    private Consumer<Business> onBusinessClicked;

    private static final double REPULSION_FORCE = 8000;
    private static final double ATTRACTION_FORCE = 0.05;
    private static final double CENTER_ATTRACTION = 0.01;
    private static final double DAMPING = 0.85;

    public GraphVisualizer(Pane pane) {
        this.pane = pane;
        pane.widthProperty().addListener((obs, oldV, newV) -> resetCenterNode());
        pane.heightProperty().addListener((obs, oldV, newV) -> resetCenterNode());
    }

    public void setOnBusinessClicked(Consumer<Business> onBusinessClicked) {
        this.onBusinessClicked = onBusinessClicked;
    }

    public void renderGraph(String keyword, List<LocalAIConcierge.SearchResult> results) {
        stop();
        pane.getChildren().clear();
        nodes.clear();
        edges.clear();
        businessNodes.clear();

        if (results == null || results.isEmpty()) {
            return;
        }

        double cx = pane.getWidth() / 2;
        double cy = pane.getHeight() / 2;
        if (cx <= 0 || cy <= 0) {
            cx = 400; // fallback if pane not yet laid out
            cy = 300;
        }

        // Center Keyword Node
        Node centerNode = new Node(keyword, null, true, cx, cy);
        nodes.add(centerNode);

        // Add Business Nodes
        java.util.Random rnd = new java.util.Random();
        for (LocalAIConcierge.SearchResult res : results) {
            Business b = res.business;
            // Spawn randomly around center
            double px = cx + (rnd.nextDouble() - 0.5) * 200;
            double py = cy + (rnd.nextDouble() - 0.5) * 200;
            Node n = new Node(b.getName(), b, false, px, py);
            nodes.add(n);
            businessNodes.put(b, n);

            // Edge from center to business
            Edge edge = new Edge(centerNode, n, 150); // resting length 150
            edges.add(edge);
        }

        // Add secondary edges between businesses sharing tags
        for (int i = 0; i < results.size(); i++) {
            for (int j = i + 1; j < results.size(); j++) {
                Business b1 = results.get(i).business;
                Business b2 = results.get(j).business;
                
                boolean shareTag = b1.getTags().stream().anyMatch(t -> b2.getTags().contains(t));
                if (shareTag) {
                    Edge e = new Edge(businessNodes.get(b1), businessNodes.get(b2), 250); // longer resting length
                    edges.add(e);
                }
            }
        }

        // Add to pane (edges first so they are behind nodes)
        for (Edge e : edges) {
            pane.getChildren().add(e.line);
        }
        for (Node n : nodes) {
            pane.getChildren().add(n.view);
            if (n.business != null) {
                setupNodeInteractivity(n);
            }
        }

        start();
    }

    private void setupNodeInteractivity(Node n) {
        StackPane view = n.view;
        view.setCursor(Cursor.HAND);
        
        Tooltip tooltip = new Tooltip(n.business.getName() + " - " + n.business.getCategory() + " \n⭐ " + n.business.getRating());
        Tooltip.install(view, tooltip);

        view.setOnMouseEntered(e -> {
            view.setScaleX(1.2);
            view.setScaleY(1.2);
        });
        view.setOnMouseExited(e -> {
            view.setScaleX(1.0);
            view.setScaleY(1.0);
        });
        view.setOnMouseClicked(e -> {
            if (onBusinessClicked != null) {
                onBusinessClicked.accept(n.business);
            }
        });

        // Dragging
        view.setOnMousePressed(e -> {
            n.isDragged = true;
            n.dragOffsetX = e.getSceneX() - n.x;
            n.dragOffsetY = e.getSceneY() - n.y;
            e.consume();
        });
        view.setOnMouseDragged(e -> {
            n.x = e.getSceneX() - n.dragOffsetX;
            n.y = e.getSceneY() - n.dragOffsetY;
            n.vx = 0;
            n.vy = 0;
            e.consume();
        });
        view.setOnMouseReleased(e -> {
            n.isDragged = false;
            e.consume();
        });
    }

    private void resetCenterNode() {
        if (!nodes.isEmpty() && nodes.get(0).isCenter) {
            nodes.get(0).x = pane.getWidth() / 2;
            nodes.get(0).y = pane.getHeight() / 2;
            nodes.get(0).vx = 0;
            nodes.get(0).vy = 0;
        }
    }

    public void start() {
        if (timer != null) timer.stop();
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updatePhysics();
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void updatePhysics() {
        // Calculate Repulsion (Coulomb's Law)
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                Node n1 = nodes.get(i);
                Node n2 = nodes.get(j);

                double dx = n1.x - n2.x;
                double dy = n1.y - n2.y;
                double distSq = dx * dx + dy * dy;

                if (distSq == 0) distSq = 0.1;
                if (distSq < 100000) { // Only repulse if close enough
                    double force = REPULSION_FORCE / distSq;
                    double dist = Math.sqrt(distSq);
                    double fx = (dx / dist) * force;
                    double fy = (dy / dist) * force;

                    if (!n1.isDragged && !n1.isCenter) {
                        n1.vx += fx;
                        n1.vy += fy;
                    }
                    if (!n2.isDragged && !n2.isCenter) {
                        n2.vx -= fx;
                        n2.vy -= fy;
                    }
                }
            }
        }

        // Calculate Attraction (Hooke's Law for springs)
        for (Edge e : edges) {
            Node n1 = e.n1;
            Node n2 = e.n2;

            double dx = n2.x - n1.x;
            double dy = n2.y - n1.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            
            if (dist > 0) {
                double force = (dist - e.restingLength) * ATTRACTION_FORCE;
                double fx = (dx / dist) * force;
                double fy = (dy / dist) * force;

                if (!n1.isDragged && !n1.isCenter) {
                    n1.vx += fx;
                    n1.vy += fy;
                }
                if (!n2.isDragged && !n2.isCenter) {
                    n2.vx -= fx;
                    n2.vy -= fy;
                }
            }
        }

        // Center gravity to keep graph from flying away
        double cx = pane.getWidth() / 2;
        double cy = pane.getHeight() / 2;
        for (Node n : nodes) {
            if (!n.isDragged && !n.isCenter) {
                double dx = cx - n.x;
                double dy = cy - n.y;
                n.vx += dx * CENTER_ATTRACTION;
                n.vy += dy * CENTER_ATTRACTION;
            }
        }

        // Apply velocities and update positions
        for (Node n : nodes) {
            if (!n.isDragged && !n.isCenter) {
                n.vx *= DAMPING;
                n.vy *= DAMPING;
                n.x += n.vx;
                n.y += n.vy;
            }
            // Keep in bounds roughly
            if (n.x < 20) n.x = 20;
            if (n.x > pane.getWidth() - 20) n.x = Math.max(20, pane.getWidth() - 20);
            if (n.y < 20) n.y = 20;
            if (n.y > pane.getHeight() - 20) n.y = Math.max(20, pane.getHeight() - 20);

            n.updateView();
        }

        for (Edge e : edges) {
            e.updateView();
        }
    }

    private static class Node {
        String label;
        Business business;
        boolean isCenter;
        double x, y;
        double vx = 0, vy = 0;
        
        boolean isDragged = false;
        double dragOffsetX = 0, dragOffsetY = 0;

        StackPane view;

        Node(String label, Business business, boolean isCenter, double x, double y) {
            this.label = label;
            this.business = business;
            this.isCenter = isCenter;
            this.x = x;
            this.y = y;

            Circle circle = new Circle(isCenter ? 35 : 25);
            circle.getStyleClass().add(isCenter ? "graph-node-center" : "graph-node");

            Text text = new Text(isCenter ? label : truncate(label, 12));
            text.getStyleClass().add("graph-node-text");

            view = new StackPane(circle, text);
            updateView();
        }

        void updateView() {
            view.setLayoutX(x - (isCenter ? 35 : 25));
            view.setLayoutY(y - (isCenter ? 35 : 25));
        }
        
        private String truncate(String s, int len) {
            if (s.length() <= len) return s;
            return s.substring(0, len-2) + "..";
        }
    }

    private static class Edge {
        Node n1, n2;
        double restingLength;
        Line line;

        Edge(Node n1, Node n2, double restingLength) {
            this.n1 = n1;
            this.n2 = n2;
            this.restingLength = restingLength;
            this.line = new Line();
            this.line.getStyleClass().add("graph-edge");
            updateView();
        }

        void updateView() {
            line.setStartX(n1.x);
            line.setStartY(n1.y);
            line.setEndX(n2.x);
            line.setEndY(n2.y);
        }
    }
}
