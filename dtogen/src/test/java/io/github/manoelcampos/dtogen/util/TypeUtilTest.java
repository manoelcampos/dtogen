package io.github.manoelcampos.dtogen.util;

import io.github.manoelcampos.dtogen.AbstractProcessorTest;
import io.github.manoelcampos.dtogen.DTOProcessor;
import io.github.manoelcampos.dtogen.samples.Class1;
import io.github.manoelcampos.dtogen.samples.SampleClass;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.List;

import static io.github.manoelcampos.dtogen.TestUtil.findField;
import static io.github.manoelcampos.dtogen.util.TypeUtil.*;
import static org.junit.jupiter.api.Assertions.*;

class TypeUtilTest extends AbstractProcessorTest {
    @Test
    void isBooleanType() {
        final var clazz = SampleClass.class;
        final var typeUtil = new TypeUtil(this.processor);
        assertTrue(typeUtil.isBooleanType(findField(elements, clazz, "bool")));
        assertFalse(typeUtil.isBooleanType(findField(elements, clazz, "str")));
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
    void testGetClassFields() {
        final var fieldNames = getClassFields(types, getClassTypeElement(Class1.class))
                .map(e -> e.getSimpleName().toString())
                .toList();

        assertEquals(List.of("id", "class2"), fieldNames);
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
}
