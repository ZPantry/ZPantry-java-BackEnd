package com.zpantry.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zpantry.analytics.api.AnalyticsDtos.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public AnalyticsService(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }

    @Transactional
    public void track(UUID userId, TrackEventRequest request) {
        Map<String, Object> properties = request.properties() == null ? Map.of() : request.properties();
        if (properties.size() > 20) throw new IllegalArgumentException("At most 20 event properties are allowed.");
        String payload;
        try { payload = json.writeValueAsString(properties); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Event properties are invalid."); }
        if (payload.length() > 4_000) throw new IllegalArgumentException("Event properties are too large.");
        jdbc.update("INSERT INTO analytics_events(id,created_at,user_id,session_id,event_name,screen_name,properties) VALUES(?,?,?,?,?,?,CAST(? AS jsonb))",
                UUID.randomUUID(), Timestamp.from(Instant.now()), userId, request.sessionId(), request.eventName(), request.screenName(), payload);
    }

    @Transactional
    public RecognitionFeedbackResponse feedback(UUID userId, UUID analysisId, RecognitionFeedbackRequest request) {
        try {
            jdbc.update("INSERT INTO ai_recognition_feedbacks(id,created_at,user_id,analysis_id,feedback,reason,correction) VALUES(?,?,?,?,?,?,?)",
                    UUID.randomUUID(), Timestamp.from(Instant.now()), userId, analysisId, request.feedback(), request.reason(), request.correction());
            return new RecognitionFeedbackResponse(analysisId, request.feedback(), true);
        } catch (DuplicateKeyException duplicate) {
            String existing = jdbc.queryForObject("SELECT feedback FROM ai_recognition_feedbacks WHERE user_id=? AND analysis_id=?", String.class, userId, analysisId);
            return new RecognitionFeedbackResponse(analysisId, existing, false);
        }
    }
}
