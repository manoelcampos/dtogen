package io.github.manoelcampos.dtogen;

import com.karuslabs.elementary.junit.Tools;
import com.karuslabs.elementary.junit.ToolsExtension;
import com.karuslabs.elementary.junit.annotations.Processors;
import com.karuslabs.utilitary.type.TypeMirrors;
import io.github.manoelcampos.dtogen.samples.*;
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
class RecordGeneratorTest {
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

    private RecordGenerator newRecordGenerator(final Class<?> modelClass) {
        final var sampleClassTypeElement = typeMirrors.asTypeElement(typeMirrors.type(modelClass));
        return new RecordGenerator(processor, sampleClassTypeElement);
    }

    /**
     * Checks the generation of a DTO record for the {@link SampleClass} model class
     * which doesn't have any association with other classes/records.
     */
    @Test
    void generateFromSimpleClass() {
        final var instance = newRecordGenerator(SampleClass.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode =
                 """
                 package io.github.manoelcampos.dtogen.samples;
                
                 import java.util.List;
                 import javax.annotation.processing.Generated;
                
                 @Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
                 public record SampleClassDTO (String str,  boolean bool,  List<String> genericList,  List nonGenericList)  {
                     @Override
                     public SampleClass toModel(){
                         final var model = new SampleClass();
                         model.setStr(this.str);
                         model.setBool(this.bool);
                         model.setGenericList(this.genericList);
                         model.setNonGenericList(this.nonGenericList);
                
                         return model;
                     }
                
                     @Override
                     public SampleClassDTO fromModel(final SampleClass model){
                         final var dto = new SampleClassDTO(
                           model.getStr(),
                           model.isBool(),
                           model.getGenericList(),
                           model.getNonGenericList()
                         );
                
                         return dto;
                     }
                
                     public SampleClassDTO() {
                         this("", false, null, null);
                     }
                 }
                 """;

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    /**
     * Checks the generation of a DTO record from a simple model {@link Record1}
     * which doesn't have any associations with other classes/records.
     */
    @Test
    void generateFromModelRecord() {
        final var instance = newRecordGenerator(Record1.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode =
                """
                package io.github.manoelcampos.dtogen.samples;

                import java.time.LocalDate;
                import javax.annotation.processing.Generated;
                
                @Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
                public record Record1DTO (Long id, String name, LocalDate date)  {
                    @Override
                    public Record1 toModel(){
                        final var model = new Record1(id, name, date);
                        return model;
                    }
                
                    @Override
                    public Record1DTO fromModel(final Record1 model){
                        final var dto = new Record1DTO(
                          model.id(),
                          model.name(),
                          model.date()
                        );
                
                        return dto;
                    }
                
                    public Record1DTO() {
                        this(0L, "", null);
                    }
                }
                """;

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    /**
     * Checks the generation of a DTO record for the {@link ExcludedFieldSampleClass}
     * expecting that a field annotated with {@link DTO.Exclude} is not included in the generated DTO.
     */
    @Test
    void generateWithDtoExclude() {
        final var instance = newRecordGenerator(ExcludedFieldSampleClass.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode =
                """
                package io.github.manoelcampos.dtogen.samples;
                
                import javax.annotation.processing.Generated;
                
                @Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
                public record ExcludedFieldSampleClassDTO (boolean included)  {
                    @Override
                    public ExcludedFieldSampleClass toModel(){
                        final var model = new ExcludedFieldSampleClass();
                        model.setIncluded(this.included);
                
                        return model;
                    }
                
                    @Override
                    public ExcludedFieldSampleClassDTO fromModel(final ExcludedFieldSampleClass model){
                        final var dto = new ExcludedFieldSampleClassDTO(model.isIncluded());
                        return dto;
                    }
                
                    public ExcludedFieldSampleClassDTO() {
                        this(false);
                    }
                }
                """;

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    /**
     * Checks the generation of a DTO record when there is a regular association (without the {@link DTO.MapToId} annotation)
     * between model classes {@link Class1} and {@link Class2}.
     */
    @Test
    void generateClassAssociation() {
        final var instance = newRecordGenerator(Class1.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode =
                """
                package io.github.manoelcampos.dtogen.samples;

                import javax.annotation.processing.Generated;
                
                @Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
                public record Class1DTO (long id,  Class2 class2)  {
                    @Override
                    public Class1 toModel(){
                        final var model = new Class1();
                        model.setId(this.id);
                        model.setClass2(this.class2);
                
                        return model;
                    }
                
                    @Override
                    public Class1DTO fromModel(final Class1 model){
                        final var dto = new Class1DTO(
                          model.getId(),
                          model.getClass2()
                        );
                
                        return dto;
                    }
                
                    public Class1DTO() {
                        this(0, null);
                    }
                }
                """;

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    /**
     * Checks the generation of a DTO record when there is an association between model classes
     * {@link Class2} and {@link Class3} annotated with {@link DTO.MapToId},
     * where the DTO must include just the ID of the {@link Class3} as a "class3Id" attribute.
     */
    @Test
    void generateMapToIdAssociation() {
        final var instance = newRecordGenerator(Class2.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode =
                """
                package io.github.manoelcampos.dtogen.samples;

                import javax.annotation.processing.Generated;
                
                @Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
                public record Class2DTO (long id, int class3Id)  {
                    @Override
                    public Class2 toModel(){
                        final var model = new Class2();
                        model.setId(this.id);
                        model.setClass3(newObject(class3Id, () -> { var o = new Class3(); o.setId(class3Id); return o; }));
                
                        return model;
                    }
                
                    @Override
                    public Class2DTO fromModel(final Class2 model){
                        final var dto = new Class2DTO(
                          model.getId(),
                          model.getClass3() == null ? 0 : model.getClass3().getId()
                        );
                
                        return dto;
                    }
                
                    public Class2DTO() {
                        this(0, 0);
                    }
                }
                """;

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    /**
     * Checks the generation of a DTO record for {@link Record2} when there is an association between the model records
     * {@link Record2} and {@link Record3} annotated with {@link DTO.MapToId},
     * where the DTO must include just the ID of the {@link Record3} as a "record3Id" attribute.
     */
    @Test
    void generateMapToIdRecordsAssociation() {
        final var instance = newRecordGenerator(Record2.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode =
                """
                package io.github.manoelcampos.dtogen.samples;

                import java.time.LocalTime;
                import javax.annotation.processing.Generated;
                
                @Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
                public record Record2DTO ( Long id,  LocalTime time,  double value,  Long record3Id)  {
                    @Override
                    public Record2 toModel(){
                        final var model = new Record2(id, time, value, new Record3(record3Id, ''));
                        return model;
                    }
                
                    @Override
                    public Record2DTO fromModel(final Record2 model){
                        final var dto = new Record2DTO(
                          model.id(),
                          model.time(),
                          model.value(),
                          model.record3() == null ? 0L : model.record3().id()
                        );
                
                        return dto;
                    }
                
                    public Record2DTO() {
                        this(0L, null, 0, 0L);
                    }
                }
                """;

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    /**
     * Checks the generation of a DTO record for {@link Record4} when there is an association with the model class
     * {@link Class1} annotated with {@link DTO.MapToId},
     * where the DTO must include just the ID of the {@link Class1} as a "class1Id" attribute.
     */
    @Test
    void generateMapToIdFromAssociationBetweenRecordAndClass() {
        final var instance = newRecordGenerator(Record4.class);
        final String generatedRecordCode = instance.generate();
        final String expectedRecordCode =
                """
                package io.github.manoelcampos.dtogen.samples;

                import javax.annotation.processing.Generated;
                
                @Generated(value = "io.github.manoelcampos.dtogen.DTOProcessor", comments = "DTO generated using DTOGen Annotation Processor")
                public record Record4DTO (Long id,  double width,  long class1Id)  {
                    @Override
                    public Record4 toModel(){
                        final var model = new Record4(
                             id, width,
                             newObject(class1Id, () -> { var o = new Class1(); o.setId(class1Id); return o; })
                        );
                
                        return model;
                    }
                
                    @Override
                    public Record4DTO fromModel(final Record4 model){
                        final var dto = new Record4DTO(
                          model.id(),
                          model.width(),
                          model.class1() == null ? 0 : model.class1().getId()
                        );
                
                        return dto;
                    }
                
                    public Record4DTO() {
                        this(0L, 0, 0);
                    }
                }
                """;

        assertCodeEquals(expectedRecordCode, generatedRecordCode);
    }

    @Test
    void isBooleanType() {
        final var clazz = SampleClass.class;
        final var instance = newRecordGenerator(clazz);
        assertTrue(instance.isBooleanType(fieldField(clazz, "bool")));
        assertFalse(instance.isBooleanType(fieldField(clazz, "str")));
    }

    @Test
    void getTypeName() {
        final var clazz = SampleClass.class;
        final var instance = newRecordGenerator(clazz);
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

    @Test
    void getAsDeclaredType() {
        final var instance = newRecordGenerator(SampleClass.class);
        // Gets a TypeMirror from a primitive (non-declared) type
        final TypeMirror primitiveTypeMirror = typeMirrors.type(long.class);
        assertNull(instance.getAsDeclaredType(primitiveTypeMirror));

        // Gets a TypeMirror from reference (declared) types
        final TypeMirror declaredTypeMirror1 = typeMirrors.type(String.class);
        final TypeMirror declaredTypeMirror2 = typeMirrors.type(Long.class);
        assertNotNull(instance.getAsDeclaredType(declaredTypeMirror1));
        assertNotNull(instance.getAsDeclaredType(declaredTypeMirror2));
    }

    private VariableElement fieldField(Class<?> clazz, final String elementName) {
        return TestUtil.findField(elements, clazz, elementName);
    }
}
