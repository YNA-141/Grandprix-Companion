package com.gpcompanion.app;

/**
 * A workaround launcher to bypass the Java 11+ module system checks for JavaFX.
 * Run this class's main method instead of GrandPrixCompanionApp.
 */
public class Launcher {
    public static void main(String[] args) {
        GrandPrixCompanionApp.main(args);
    }
}
