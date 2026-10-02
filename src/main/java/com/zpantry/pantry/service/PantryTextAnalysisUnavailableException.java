package com.zpantry.pantry.service;

/** Safe boundary exception: parser details are logged server-side, never returned to the client. */
public final class PantryTextAnalysisUnavailableException extends RuntimeException {
    public PantryTextAnalysisUnavailableException() {
        super("Pantry text analysis is currently unavailable.");
    }
}
