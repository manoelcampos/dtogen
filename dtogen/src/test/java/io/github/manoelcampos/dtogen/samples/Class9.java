package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;
import jakarta.validation.constraints.Pattern;

/**
 * A model class with (i) annotation attributes (such as a {@link Pattern} regex) having characters that must be escaped in Java literals
 * and (ii) boxed and primitive numeric fields that can't receive an int literal as default value.
 * The class is used to check if the generated DTO record compiles in such cases.
 * @author Manoel Campos
 */
@SuppressWarnings("unused")
@DTO
public class Class9 {
    @Pattern(regexp = "[A-Z]{3}\\d{4}")
    private String plate;

    @Pattern(regexp = ".*", message = "say \"hi\"\n\ttab")
    private String text;

    private Double price;
    private Float weight;
    private float height;
    private Short stock;
    private Byte rating;

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public Short getStock() {
        return stock;
    }

    public void setStock(Short stock) {
        this.stock = stock;
    }

    public Byte getRating() {
        return rating;
    }

    public void setRating(Byte rating) {
        this.rating = rating;
    }
}
