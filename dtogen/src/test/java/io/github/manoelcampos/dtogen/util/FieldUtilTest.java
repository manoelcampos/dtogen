package io.github.manoelcampos.dtogen.util;

import com.karuslabs.elementary.junit.Tools;
import com.karuslabs.elementary.junit.ToolsExtension;
import io.github.manoelcampos.dtogen.TestUtil;
import io.github.manoelcampos.dtogen.samples.Class1;
import io.github.manoelcampos.dtogen.samples.ExcludedFieldSampleClass;
import io.github.manoelcampos.dtogen.samples.Record1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import static io.github.manoelcampos.dtogen.TestUtil.findField;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ToolsExtension.class)
class FieldUtilTest {
    private final Elements elements = Tools.elements();

    @Test
    void testGetUpCaseFieldName() {
        assertEquals("Name", FieldUtil.getUpCaseFieldName("name"));
        assertEquals("Name", FieldUtil.getUpCaseFieldName("Name"));
        assertEquals("NAME", FieldUtil.getUpCaseFieldName("nAME"));
        assertEquals("NamE", FieldUtil.getUpCaseFieldName("namE"));
    }

    @Test
    void testIsInstanceField() {
        final var classType = getClassTypeElement(Class1.class);
        final var fieldsMap = classType.getEnclosedElements().stream()
                                       .filter(e -> e.getKind().isField())
                                       .collect(toMap(e -> e.getSimpleName().toString(), e -> (VariableElement) e));

        assertFalse(FieldUtil.isInstanceField(fieldsMap.get("MAX")));
        assertTrue(FieldUtil.isInstanceField(fieldsMap.get("id")));
        assertTrue(FieldUtil.isInstanceField(fieldsMap.get("class2")));
    }

    @Test
    void testIsPrimitive() {
        final var idField = findField(elements, Class1.class, "id");
        final var class2Field = findField(elements, Class1.class, "class2");

        assertTrue(FieldUtil.isPrimitive(idField));
        assertFalse(FieldUtil.isPrimitive(class2Field));
    }

    @Test
    void testGetFieldName() {
        final var fieldName = "id";
        final var idField = findField(elements, Class1.class, fieldName);
        assertEquals(fieldName, FieldUtil.getFieldName(idField));
    }

    @Test
    void testIsNotFieldExcluded() {
        final var includedField = findField(elements, ExcludedFieldSampleClass.class, "included");
        assertTrue(FieldUtil.isNotFieldExcluded(includedField));

        final var excludedField = findField(elements, ExcludedFieldSampleClass.class, "excluded");
        assertFalse(FieldUtil.isNotFieldExcluded(excludedField));
    }

    @Test
    void isNotIdField() {
        final var dateField = findField(elements, Record1.class, "date");
        assertTrue(FieldUtil.isNotIdField(dateField));

        final var idField = findField(elements, Record1.class, "id");
        assertFalse(FieldUtil.isNotIdField(idField));
    }

    private TypeElement getClassTypeElement(final Class<?> clazz) {
        return TestUtil.getClassTypeElement(elements, clazz);
    }
}
