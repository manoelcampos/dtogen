package io.github.manoelcampos.dtogen.util;

import com.karuslabs.elementary.junit.Tools;
import com.karuslabs.elementary.junit.ToolsExtension;
import io.github.manoelcampos.dtogen.DTOProcessor;
import io.github.manoelcampos.dtogen.samples.Class1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

import static io.github.manoelcampos.dtogen.TestUtil.findField;
import static io.github.manoelcampos.dtogen.util.TypeUtil.*;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ToolsExtension.class)
class TypeUtilTest {
    private final Elements elements = Tools.elements();
    private final Types types = Tools.types();

    @Test
    void testGetUpCaseFieldName() {
        assertEquals("Name", FieldUtil.getUpCaseFieldName("name"));
        assertEquals("Name", FieldUtil.getUpCaseFieldName("Name"));
        assertEquals("NAME", FieldUtil.getUpCaseFieldName("nAME"));
        assertEquals("NamE", FieldUtil.getUpCaseFieldName("namE"));
    }

    @Test
    void testGetPackageName() {
        final var typeElement = elements.getTypeElement(TypeUtil.class.getName());
        assertEquals("io.github.manoelcampos.dtogen.util", getPackageName(typeElement));
    }

    @Test
    void testGetPackageNameString() {
        assertEquals("io.github.manoelcampos.dtogen.util", getPackageName(TypeUtil.class.getName()));
        assertEquals("", getPackageName("DTOProcessor"));
    }

    @Test
    void testGetSimpleClassName() {
        assertEquals("ClassUtil", getSimpleClassName("io.github.manoelcampos.dtogen.util.ClassUtil"));
        assertEquals("DTOProcessor", getSimpleClassName("DTOProcessor"));
    }

    @Test
    void testHasSuperClass() {
        assertFalse(hasSuperClass(getClassTypeMirror(TypeUtil.class)));
        assertTrue(hasSuperClass(getClassTypeMirror(DTOProcessor.class)));
        assertTrue(hasSuperClass(getClassTypeMirror(Integer.class)));
    }

    @Test
    void testIsInstanceField() {
        final var classType = getClassTypeMirror(Class1.class);
        final var fieldsMap = classType.getEnclosedElements().stream()
                .filter(e -> e.getKind().isField())
                .collect(toMap(e -> e.getSimpleName().toString(), e -> (VariableElement) e));

        assertFalse(FieldUtil.isInstanceField(fieldsMap.get("MAX")));
        assertTrue(FieldUtil.isInstanceField(fieldsMap.get("id")));
        assertTrue(FieldUtil.isInstanceField(fieldsMap.get("class2")));
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

        assertTrue(FieldUtil.isPrimitive(idField));
        assertFalse(FieldUtil.isPrimitive(class2Field));
    }

    @Test
    void testIsNotThreeSlashesComment() {
        assertTrue(isNotThreeSlashesComment("/** Block Comment */"));
        assertTrue(isNotThreeSlashesComment("// Line Comment "));
        assertTrue(isNotThreeSlashesComment("// / Line Comment "));
    }

    @Test
    void testIsThreeSlashesComment() {
        assertFalse(isNotThreeSlashesComment("/// Tree Slashes Comment "));
        assertFalse(isNotThreeSlashesComment("///Tree Slashes Comment "));
        assertFalse(isNotThreeSlashesComment("////// Tree Slashes Comment "));
    }

    private TypeElement getClassTypeMirror(final Class<?> clazz) {
        return elements.getTypeElement(clazz.getName());
    }
}
