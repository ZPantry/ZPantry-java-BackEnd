package com.zpantry.integration.ai;

/** Performs a shallow liveness request only; it must never invoke a billable AI operation. */
public interface AiServiceHealthProbe {
    void probe();
}
