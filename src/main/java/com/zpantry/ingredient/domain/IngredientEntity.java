package com.zpantry.ingredient.domain;

import com.zpantry.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
@Entity @Table(name="ingredients") public class IngredientEntity extends BaseEntity{
 @Column(nullable=false,length=200) public String name; @Column(name="normalized_name",nullable=false,length=200) public String normalizedName;
    @Column(length = 100)
    public String category;
    @Column(length = 50)
    public String unit;
    @Column(columnDefinition = "text")
    public String allergens;
 @Column(name="calories_per_unit",precision=18,scale=4) public BigDecimal caloriesPerUnit;@Column(name="protein_per_unit",precision=18,scale=4) public BigDecimal proteinPerUnit;@Column(name="fat_per_unit",precision=18,scale=4) public BigDecimal fatPerUnit;@Column(name="carb_per_unit",precision=18,scale=4) public BigDecimal carbPerUnit;
 @Column(name="default_quantity",precision=18,scale=4) public BigDecimal defaultQuantity;
 @Column(name="image_url",length=500) public String imageUrl;@Column(name="gradient_from",length=32) public String gradientFrom;@Column(name="gradient_to",length=32) public String gradientTo;
 @JdbcTypeCode(SqlTypes.VECTOR) @Column(columnDefinition="vector(1536)") public float[] embedding; protected IngredientEntity(){} public IngredientEntity(String n){name=n.trim();normalizedName=n.trim().toLowerCase();}
}
