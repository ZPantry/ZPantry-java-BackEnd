package com.zpantry.recommendation.domain;

import com.zpantry.common.persistence.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "recommendation_feedbacks")
public class RecommendationFeedbackEntity extends BaseEntity {
    @Column(name = "user_id")
    public UUID userId;
    @Column(name = "meal_recommendation_id")
    public UUID mealRecommendationId;
    @Column(name = "recipe_id")
    public UUID recipeId;
    public Integer rating;
    @Column(name = "feedback_type")
    public String feedbackType;
    public String comment;

    protected RecommendationFeedbackEntity() {
    }

    public RecommendationFeedbackEntity(UUID u, UUID m, UUID r, Integer rating, String type, String comment) {
        userId = u;
        mealRecommendationId = m;
        recipeId = r;
        this.rating = rating;
        feedbackType = type;
        this.comment = comment;
    }
}
