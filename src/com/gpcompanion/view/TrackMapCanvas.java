package com.gpcompanion.view;

import com.gpcompanion.model.Driver;
import com.gpcompanion.model.RaceSession;
import com.gpcompanion.model.TrackPoint;
import com.gpcompanion.model.TrackProfile;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.List;

public class TrackMapCanvas extends Canvas {

    private TrackProfile trackProfile;
    
    private double scale = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;

    private static final Color TRACK_GLOW = Color.web("#00dba9", 0.3);
    private static final Color TRACK_CORE = Color.web("#00dba9");
    private static final Color DRIVER_DOT = Color.web("#e00000");
    private static final Color LEADER_DOT = Color.web("#00dba9"); // Optional distinct color for leader if desired, but spec says "moving red driver dots"
    
    private static final double TRACK_WIDTH = 3.0;
    private static final double DOT_RADIUS = 6.0;

    public TrackMapCanvas(double width, double height) {
        super(width, height);
    }

    public void setTrackProfile(TrackProfile profile) {
        this.trackProfile = profile;
        redraw(null);
    }

    public void redraw(RaceSession session) {
        double w = getWidth();
        double h = getHeight();
        GraphicsContext gc = getGraphicsContext2D();

        gc.clearRect(0, 0, w, h);

        if (trackProfile == null) return;
        
        calculateTransform(w, h);

        drawTrack(gc, w, h);

        if (session != null) {
            drawCars(gc, session, w, h);
        }
    }

    private void calculateTransform(double w, double h) {
        List<TrackPoint> pts = trackProfile.getCurvePoints();
        if (pts == null || pts.isEmpty()) return;

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (TrackPoint p : pts) {
            if (p.getX() < minX) minX = p.getX();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getY() > maxY) maxY = p.getY();
        }

        double trackW = maxX - minX;
        if (trackW == 0) trackW = 1;
        double trackH = maxY - minY;
        if (trackH == 0) trackH = 1;

        double scaleX = w * 0.8 / trackW;
        double scaleY = h * 0.8 / trackH;
        this.scale = Math.min(scaleX, scaleY);

        this.offsetX = (w - trackW * this.scale) / 2 - minX * this.scale;
        this.offsetY = (h - trackH * this.scale) / 2 - minY * this.scale;
    }

    private void drawTrack(GraphicsContext gc, double w, double h) {
        List<TrackPoint> pts = trackProfile.getCurvePoints();
        if (pts == null || pts.size() < 2) return;

        // Draw glow layer
        gc.setStroke(TRACK_GLOW);
        gc.setLineWidth(TRACK_WIDTH * 2.5);
        drawPolyline(gc, pts, w, h);

        // Draw core layer
        gc.setStroke(TRACK_CORE);
        gc.setLineWidth(TRACK_WIDTH);
        drawPolyline(gc, pts, w, h);
    }

    private void drawPolyline(GraphicsContext gc, List<TrackPoint> pts, double w, double h) {
        gc.beginPath();
        for (int i = 0; i < pts.size(); i++) {
            TrackPoint p = pts.get(i);
            double x = p.getX() * scale + offsetX;
            double y = p.getY() * scale + offsetY;
            if (i == 0) gc.moveTo(x, y);
            else gc.lineTo(x, y);
        }
        // Close the loop
        gc.lineTo(pts.get(0).getX() * scale + offsetX, pts.get(0).getY() * scale + offsetY);
        gc.stroke();
    }

    private void drawCars(GraphicsContext gc, RaceSession session, double w, double h) {
        if (session.getDrivers() == null) return;

        gc.setFont(Font.font("Inter", FontWeight.BOLD, 10));

        for (Driver d : session.getDrivers()) {
            double lapDist = d.getCar().getLapDistanceMeters(trackProfile.getTrackLengthMeters());
            Point2D pt = trackProfile.getXYForDistance(lapDist);

            if (pt != null) {
                double x = pt.getX() * scale + offsetX;
                double y = pt.getY() * scale + offsetY;

                // Draw red dot
                gc.setFill(DRIVER_DOT);
                gc.fillOval(x - DOT_RADIUS, y - DOT_RADIUS, DOT_RADIUS * 2, DOT_RADIUS * 2);

                // Draw shortcode above dot
                gc.setFill(Color.WHITE);
                gc.fillText(d.getShortCode(), x - 10, y - 12);
            }
        }
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }
}
