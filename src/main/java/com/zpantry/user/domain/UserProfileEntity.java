package com.zpantry.user.domain;

import com.zpantry.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity extends BaseEntity {
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    
    @Column(name = "age")
    private Integer age;
    
    @Column(name = "gender", length = 50)
    private String gender;
    
    @Column(name = "height", precision = 5, scale = 2)
    private BigDecimal height;
    
    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;
    
    @Column(name = "goal", length = 200)
    private String goal;
    
    @Column(name = "diet_preference", length = 500)
    private String dietPreference;
    
    @Column(name = "allergies", columnDefinition = "text")
    private String allergies;

    protected UserProfileEntity() {}

    public UserProfileEntity(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() { return userId; }
    public Integer getAge() { return age; }
    public String getGender() { return gender; }
    public BigDecimal getHeight() { return height; }
    public BigDecimal getWeight() { return weight; }
    public String getGoal() { return goal; }
    public String getDietPreference() { return dietPreference; }
    public String getAllergies() { return allergies; }

    public void update(Integer age, String gender, BigDecimal height, BigDecimal weight, 
            String goal, String dietPreference, String allergies) {
        this.age = age;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.goal = goal;
        this.dietPreference = dietPreference;
        this.allergies = allergies;
        this.touch();
    }
}
