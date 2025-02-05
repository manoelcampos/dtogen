package io.github.manoelcampos.dtogen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnnotationDataTest {
    @Test
    void formatAttrValue() {
        assertEquals("true", AnnotationData.formatAttrValue(true));
        assertEquals("false", AnnotationData.formatAttrValue(false));
        assertEquals("1", AnnotationData.formatAttrValue(1));
        assertEquals("1.0", AnnotationData.formatAttrValue(1.0));

        // Adds quotes for text and char
        assertEquals("\"text\"", AnnotationData.formatAttrValue("text"));
        assertEquals("'c'", AnnotationData.formatAttrValue('c'));

        final Character upperCase = 'C';
        assertEquals("'C'", AnnotationData.formatAttrValue(upperCase));
    }

    @Test
    void getName() {
        assertEquals("io.github.manoelcampos.dtogen.AnnotationData", AnnotationData.getName(AnnotationData.class));
        assertEquals("io.github.manoelcampos.dtogen.DTO.MapToId", AnnotationData.getName(DTO.MapToId.class));
    }
}
