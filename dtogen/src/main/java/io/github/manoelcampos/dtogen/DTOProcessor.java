package io.github.manoelcampos.dtogen;

import com.google.auto.service.AutoService;
import io.github.manoelcampos.dtogen.util.TypeUtil;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.partitioningBy;

/**
 * Processes DTOGen annotations to dynamically generate DTO records.
 * @author Manoel Campos
 * @see <a href="https://www.baeldung.com/java-annotation-processing-builder">Java Annotation Processing with Builder</a>
 */
@SupportedAnnotationTypes("io.github.manoelcampos.dtogen.DTO")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class DTOProcessor extends AbstractProcessor {
    private Types types;
    private final TypeUtil typeUtil;

    private JavaFileWriter javaFileWriter;

    /** Default constructor called during the application compilation process,
     * to further execute the processor. */
    public DTOProcessor() {
        this.typeUtil = new TypeUtil(this);
    }

    /**
     * Creates and initializes a DTOProcessor for testing purposes
     * @param processingEnv processing environment to initialize the processor
     */
    public DTOProcessor(final ProcessingEnvironment processingEnv) {
        this();
        init(processingEnv);
    }

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.types = processingEnv.getTypeUtils();
        this.javaFileWriter = new JavaFileWriter(this);
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> processAnnotation(roundEnv, annotation));
        return true;
    }

    /**
     * Process the {@link DTO} annotation found on a class.
     * @param roundEnv
     * @param annotation the {@link DTO} annotation to be processed
     */
    private void processAnnotation(final RoundEnvironment roundEnv, final TypeElement annotation) {
        final var annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

        final var annotatedElementsMap = getAnnotatedElementsMap(annotatedElements);
        //Gets only classes which are annotated with @DTO
        final var classElements = annotatedElementsMap.get(true);
        classElements.stream().findFirst().ifPresent(this::createDtoInterface);

        classElements.stream().map(this::newRecordGenerator).forEach(RecordGenerator::write);

        //Gets only non-classes which are annotated with @DTO
        showInvalidAnnotationLocation(annotation, annotatedElementsMap.get(false));
    }

    /**
     * {@return a new object to generate a DTO record}
     * @param classElement the model/entity class to generate a DTO record for
     */
    private RecordGenerator newRecordGenerator(final Element classElement) {
        return new RecordGenerator(this, classElement);
    }

    /**
     * Gets a Map with the elements with the {@link DTO} annotation.
     * The map has a boolean key where:
     * - "true" key contains a list of annotated elements which are classes;
     * - "false" key contains a list of annotated elements which aren't classes.
     *
     * @param annotatedElements list of elements with the {@link DTO} annotation
     * @return the created map
     */
    private static Map<Boolean, List<Element>> getAnnotatedElementsMap(final Set<? extends Element> annotatedElements) {
        return annotatedElements.stream().collect(partitioningBy(el -> el.getKind().isClass()));
    }

    void error(final Element element, final String msg){
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element);
    }

    /**
     * Gets the {@link DTORecord} source code from the interface copied to the
     * DTOGen jar resources dir and writes it to the generated-source dir inside
     * the application using DTOGen.
     * Check the interface documentation for more details.
     * @param sourceClass a class with the {@link DTO} annotation to get its package name,
     *                    so that the DTO interface is created in the same package.
     */
    private void createDtoInterface(final Element sourceClass) {
        final var classElement = (TypeElement) sourceClass;
        final var packageName = TypeUtil.getPackageName(classElement);

        final var dtoRecordName = DTORecord.class.getSimpleName();
        final var dtoInterfaceCode = JavaFileReader.readFromResources(dtoRecordName + ".java", packageName);
        javaFileWriter.write(packageName, dtoRecordName, dtoInterfaceCode);
    }

    /**
     * Shows an error message for each element that is not a class and have the {@link DTO} annotation.
     * @param annotation the annotation being processed.
     * @param nonClassTypes the list of elements with the {@link DTO} annotation that are not classes.
     */
    private void showInvalidAnnotationLocation(final TypeElement annotation, final List<? extends Element> nonClassTypes) {
        final var msg = annotation.getQualifiedName() + " must be applied to a class";
        nonClassTypes.forEach(el -> error(el, msg));
    }

    ProcessingEnvironment processingEnv() {
        return processingEnv;
    }

    public Types types() {
        return types;
    }

    public TypeUtil typeUtil() {
        return typeUtil;
    }

}
