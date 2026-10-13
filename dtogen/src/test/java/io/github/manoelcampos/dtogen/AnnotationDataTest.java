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
    void formatAttrValueEscapesSpecialChars() {
        assertEquals("\"[A-Z]{3}\\\\d{4}\"", AnnotationData.formatAttrValue("[A-Z]{3}\\d{4}"));
        assertEquals("\"say \\\"hi\\\"\\n\\ttab\\r\"", AnnotationData.formatAttrValue("say \"hi\"\n\ttab\r"));
        assertEquals("'\\''", AnnotationData.formatAttrValue('\''));
        assertEquals("'\\\\'", AnnotationData.formatAttrValue('\\'));
        assertEquals("'\"'", AnnotationData.formatAttrValue('"'));
        assertEquals("'\\n'", AnnotationData.formatAttrValue('\n'));

        // Control chars without a specific escape sequence are written as octal escapes
        assertEquals("\"a\\000b\"", AnnotationData.formatAttrValue("a\0b"));
    }

    @Test
    void getName() {
        assertEquals("io.github.manoelcampos.dtogen.AnnotationData", AnnotationData.getName(AnnotationData.class));
        assertEquals("io.github.manoelcampos.dtogen.DTO.MapToId", AnnotationData.getName(DTO.MapToId.class));
    }
}
