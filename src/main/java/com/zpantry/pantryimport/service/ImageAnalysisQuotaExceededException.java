package com.zpantry.pantryimport.service;

public final class ImageAnalysisQuotaExceededException extends RuntimeException {
    public ImageAnalysisQuotaExceededException() {
        super("Monthly image analysis limit reached.");
    }
}
