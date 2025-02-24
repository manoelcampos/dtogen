package io.github.manoelcampos.dtogen;

import com.karuslabs.elementary.junit.Tools;
import com.karuslabs.elementary.junit.ToolsExtension;
import com.karuslabs.elementary.junit.annotations.Processors;
import com.karuslabs.utilitary.type.TypeMirrors;
import io.github.manoelcampos.dtogen.samples.*;
import io.github.manoelcampos.dtogen.util.TypeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import static io.github.manoelcampos.dtogen.TestUtil.assertCodeEquals;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ToolsExtension.class)
@Processors({DTOProcessor.class})
public class RecordGeneratorTest {
    private final ProcessingEnvironment env = Mockito.mock(ProcessingEnvironment.class);
    private DTOProcessor processor;
    private Elements elements;
    private TypeMirrors typeMirrors;

    @BeforeEach
    void setUp() {
        Mockito.when(env.getTypeUtils()).thenReturn(Tools.types());
        this.processor = new DTOProcessor(env);
        this.typeMirrors = Tools.typeMirrors();
        this.elements = Tools.elements();
    }

    private RecordGenerator newInstance(final Class<?> modelClass) {
        final var sampleClassTypeElement = typeMirrors.asTypeElement(typeMirrors.type(modelClass));
        return new RecordGenerator(processor, sampleClassTypeElement);
    }

    /**
     * Checks the generation of a DTO record for the {@link SampleClass} model class
     * which doesn't have any association with other classes/records.
     */
    @Test
    void generateFromSimpleClass() {
        final var instance = newInstance(SampleClass.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode = TestUtil.loadSampleSourceFile("SampleClassDTO.java");

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    /**
     * Checks the generation of a DTO record from a simple model {@link Record1}
     * which doesn't have any associations with other classes/records.
     */
    @Test
    void generateFromModelRecord() {
        final var instance = newInstance(Record1.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode = TestUtil.loadSampleSourceFile("Record1DTO.java");

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    /**
     * Checks the generation of a DTO record for the {@link ExcludedFieldSampleClass}
     * expecting that a field annotated with {@link DTO.Exclude} is not included in the generated DTO.
     */
    @Test
    void generateWithDtoExclude() {
        final var instance = newInstance(ExcludedFieldSampleClass.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode = TestUtil.loadSampleSourceFile("ExcludedFieldSampleClassDTO.java");

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    /**
     * Checks the generation of a DTO record when there is a regular association (without the {@link DTO.MapToId} annotation)
     * between model classes {@link Class1} and {@link Class2}.
     */
    @Test
    void generateClassAssociation() {
        final var instance = newInstance(Class1.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode = TestUtil.loadSampleSourceFile("Class1DTO.java");

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    /**
     * Checks the generation of a DTO record when there is an association between model classes
     * {@link Class2} and {@link Class3} annotated with {@link DTO.MapToId},
     * where the DTO must include just the ID of the {@link Class3} as a "class3Id" attribute.
     */
    @Test
    void generateMapToIdAssociation() {
        final var instance = newInstance(Class2.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode = TestUtil.loadSampleSourceFile("Class2DTO.java");

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    /**
     * Checks the generation of a DTO record for {@link Record2} when there is an association between the model records
     * {@link Record2} and {@link Record3} annotated with {@link DTO.MapToId},
     * where the DTO must include just the ID of the {@link Record3} as a "record3Id" attribute.
     */
    @Test
    void generateMapToIdRecordsAssociation() {
        final var instance = newInstance(Record2.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode = TestUtil.loadSampleSourceFile("Record2DTO.java");

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    /**
     * Checks the generation of a DTO record for {@link Record4} when there is an association with the model class
     * {@link Class1} annotated with {@link DTO.MapToId},
     * where the DTO must include just the ID of the {@link Class1} as a "class1Id" attribute.
     */
    @Test
    void generateMapToIdFromAssociationBetweenRecordAndClass() {
        final var instance = newInstance(Record4.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode = TestUtil.loadSampleSourceFile("Record4DTO.java");

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    @Test
    void isBooleanType() {
        final var clazz = SampleClass.class;
        final var instance = newInstance(clazz);
        assertTrue(instance.isBooleanType(fieldField(clazz, "bool")));
        assertFalse(instance.isBooleanType(fieldField(clazz, "str")));
    }

    @Test
    void getAsDeclaredType() {
        // Gets a TypeMirror from a primitive (non-declared) type
        final TypeMirror primitiveTypeMirror = typeMirrors.type(long.class);
        assertNull(TypeUtil.getAsDeclaredType(primitiveTypeMirror));

        // Gets a TypeMirror from reference (declared) types
        final TypeMirror declaredTypeMirror1 = typeMirrors.type(String.class);
        final TypeMirror declaredTypeMirror2 = typeMirrors.type(Long.class);
        assertNotNull(TypeUtil.getAsDeclaredType(declaredTypeMirror1));
        assertNotNull(TypeUtil.getAsDeclaredType(declaredTypeMirror2));
    }

    @Test
    void testGetTypeName() {
        final var clazz = SampleClass.class;
        final var instance = new TypeUtil(processor);
        final VariableElement strAttribute = fieldField(clazz, "str");
        final VariableElement genericListAttribute = fieldField(clazz, "genericList");
        final VariableElement nonGenericListAttribute = fieldField(clazz, "nonGenericList");

        assertEquals("java.lang.String", instance.getTypeName(strAttribute));
        assertEquals("java.util.List<java.lang.String>", instance.getTypeName(genericListAttribute));
        assertEquals("java.util.List", instance.getTypeName(nonGenericListAttribute));

        assertEquals("String", instance.getTypeName(strAttribute, false, false));
        assertEquals("List<java.lang.String>", instance.getTypeName(genericListAttribute, false, true));
        assertEquals("List<java.lang.String>", instance.getTypeName(genericListAttribute, false, true));
        assertEquals("List", instance.getTypeName(genericListAttribute, false, false));
        assertEquals("java.util.List", instance.getTypeName(genericListAttribute, true, false));
        assertEquals("java.util.List", instance.getTypeName(nonGenericListAttribute, true, false));
    }

    private VariableElement fieldField(Class<?> clazz, final String elementName) {
        return TestUtil.findField(elements, clazz, elementName);
    }
}
