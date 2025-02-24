package io.github.manoelcampos.dtogen.util;

import com.karuslabs.elementary.junit.Tools;
import com.karuslabs.elementary.junit.ToolsExtension;
import com.karuslabs.elementary.junit.annotations.Processors;
import io.github.manoelcampos.dtogen.DTOProcessor;
import io.github.manoelcampos.dtogen.samples.Class1;
import io.github.manoelcampos.dtogen.samples.SampleClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.annotation.processing.ProcessingEnvironment;
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
@Processors({DTOProcessor.class})
class TypeUtilTest {
    private final ProcessingEnvironment env = Mockito.mock(ProcessingEnvironment.class);
    private DTOProcessor processor;

    private final Elements elements = Tools.elements();
    private final Types types = Tools.types();

    @BeforeEach
    void setUp() {
        Mockito.when(env.getTypeUtils()).thenReturn(Tools.types());
        this.processor = new DTOProcessor(env);
    }

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
        assertFalse(hasSuperClass(getClassTypeElement(TypeUtil.class)));
        assertTrue(hasSuperClass(getClassTypeElement(DTOProcessor.class)));
        assertTrue(hasSuperClass(getClassTypeElement(Integer.class)));
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
    void testGetClassFields() {
        final var fieldNames = getClassFields(types, getClassTypeElement(Class1.class))
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

    @Test
    void testGetTypeNameFullQualifiedAndWithGenericArgs() {
        final VariableElement genericListType = findField(elements, SampleClass.class, "genericList");
        final String actualType = new TypeUtil(processor).getTypeName(genericListType);
        assertEquals("java.util.List<java.lang.String>", actualType);
    }

    @Test
    void testGetTypeNameFullQualifiedAndNoGenericArgs() {
        final VariableElement genericListType = findField(elements, SampleClass.class, "genericList");
        final String actualType = new TypeUtil(processor).getTypeName(genericListType, true, false);
        assertEquals("java.util.List", actualType);
    }

    @Test
    void testGetTypeNameSimpleNameAndWithGenericArgs() {
        final VariableElement genericListType = findField(elements, SampleClass.class, "genericList");
        final String actualType = new TypeUtil(processor).getTypeName(genericListType, false, true);
        assertEquals("List<java.lang.String>", actualType);
    }

    @Test
    void testGetTypeNameSimpleNameAndNoGenericArgs() {
        final VariableElement genericListType = findField(elements, SampleClass.class, "genericList");
        final String actualType = new TypeUtil(processor).getTypeName(genericListType, false, false);
        assertEquals("List", actualType);
    }

    @Test
    void testFindIdField() {
        final var typeUtil = new TypeUtil(processor);

        final TypeElement primitiveTypeElement = null; // primitive types don't have a TypeElement
        assertTrue(typeUtil.findIdField(primitiveTypeElement).isEmpty());

        final var sampleClassElement = getClassTypeElement(SampleClass.class);
        assertTrue(typeUtil.findIdField(sampleClassElement).isEmpty());

        final var class1Element = getClassTypeElement(Class1.class);
        assertFalse(typeUtil.findIdField(class1Element).isEmpty());
    }

    private TypeElement getClassTypeElement(final Class<?> clazz) {
        return elements.getTypeElement(clazz.getName());
    }
}
