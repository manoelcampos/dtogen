package io.github.manoelcampos.dtogen;

import com.karuslabs.elementary.junit.Tools;
import com.karuslabs.elementary.junit.ToolsExtension;
import io.github.manoelcampos.dtogen.samples.Class1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

import static io.github.manoelcampos.dtogen.ClassUtil.*;
import static io.github.manoelcampos.dtogen.TestUtil.findField;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ToolsExtension.class)
class ClassUtilTest {
    private final Elements elements = Tools.elements();
    private final Types types = Tools.types();

    @Test
    void testGetUpCaseFieldName() {
        assertEquals("Name", getUpCaseFieldName("name"));
        assertEquals("Name", getUpCaseFieldName("Name"));
        assertEquals("NAME", getUpCaseFieldName("nAME"));
        assertEquals("NamE", getUpCaseFieldName("namE"));
    }

    @Test
    void testGetPackageName() {
        final var typeElement = elements.getTypeElement(ClassUtil.class.getName());
        assertEquals("io.github.manoelcampos.dtogen", getPackageName(typeElement));
    }

    @Test
    void testGetPackageNameString() {
        assertEquals("io.github.manoelcampos.dtogen", getPackageName("io.github.manoelcampos.dtogen.ClassUtil"));
        assertEquals("", getPackageName("DTOProcessor"));
    }

    @Test
    void testGetSimpleClassName() {
        assertEquals("ClassUtil", getSimpleClassName("io.github.manoelcampos.dtogen.ClassUtil"));
        assertEquals("DTOProcessor", getSimpleClassName("DTOProcessor"));
    }

    @Test
    void testHasSuperClass() {
        assertFalse(hasSuperClass(getClassTypeMirror(ClassUtil.class)));
        assertTrue(hasSuperClass(getClassTypeMirror(DTOProcessor.class)));
        assertTrue(hasSuperClass(getClassTypeMirror(Integer.class)));
    }

    @Test
    void testIsInstanceField() {
        final var classType = getClassTypeMirror(Class1.class);
        final var fieldsMap = classType.getEnclosedElements().stream()
                .filter(e -> e.getKind().isField())
                .collect(toMap(e -> e.getSimpleName().toString(), e -> (VariableElement) e));

        assertFalse(isInstanceField(fieldsMap.get("MAX")));
        assertTrue(isInstanceField(fieldsMap.get("id")));
        assertTrue(isInstanceField(fieldsMap.get("class2")));
    }

    @Test
    void testGetClassFields() {
        final var fieldNames = getClassFields(types, getClassTypeMirror(Class1.class))
                .map(e -> e.getSimpleName().toString())
                .toList();

        assertEquals(List.of("id", "class2"), fieldNames);
    }

    @Test
    void testIsPrimitive() {
        final var idField = findField(elements, Class1.class, "id");
        final var class2Field = findField(elements, Class1.class, "class2");

        assertTrue(isPrimitive(idField));
        assertFalse(isPrimitive(class2Field));
    }

    private TypeElement getClassTypeMirror(final Class<?> clazz) {
        return elements.getTypeElement(clazz.getName());
    }
}
