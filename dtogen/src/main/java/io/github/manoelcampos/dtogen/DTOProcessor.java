package io.github.manoelcampos.dtogen;

import com.google.auto.service.AutoService;

import javax.annotation.Nullable;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.manoelcampos.dtogen.ClassUtil.readJavaSourceFileFromResources;
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

    private Types typeUtils;
    private JavaFileWriter javaFileWriter;

    /** Default constructor called during the application compilation process,
     * to further execute the processor. */
    public DTOProcessor() {/**/}

    /**
     * Creates and initializes a DTOProcessor for testing purposes
     * @param processingEnv processing environment to initialize the processor
     */
    DTOProcessor(final ProcessingEnvironment processingEnv) {
        init(processingEnv);
    }

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.typeUtils = processingEnv.getTypeUtils();
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
     * Creates a base interface to be implemented by all DTO generated records.
     * @param sourceClass a class with the {@link DTO} annotation to get its package name,
     *                    so that the DTO interface is created in the same package.
     */
    private void createDtoInterface(final Element sourceClass) {
        final var classElement = (TypeElement) sourceClass;
        final var packageName = ClassUtil.getPackageName(classElement);

        final var dtoRecordName = DTORecord.class.getSimpleName();
        final var dtoInterfaceCode = readJavaSourceFileFromResources(dtoRecordName + ".java", packageName);
        javaFileWriter.write(packageName, dtoRecordName, dtoInterfaceCode);
    }

    /**
     * Checks if a given {@link Element} (such as a {@link TypeElement} or {@link VariableElement})
     * has a specific annotation.
     * Generic types usually are the ones that accept annotations,
     * such as {@code List<@NonNull String>}.
     * @param element element to check
     * @param annotation annotation to look for in the given element
     * @return true if the annotation is present on the element, false otherwise
     */
    static boolean hasAnnotation(final Element element, final Class<? extends Annotation> annotation) {
        return element.getAnnotation(annotation) != null;
    }

    /**
     * Shows an error message for each element that is not a class and have the {@link DTO} annotation.
     * @param annotation the annotation being processed.
     * @param nonClassTypes the list of elements with the {@link DTO} annotation that are not classes.
     */
    private void showInvalidAnnotationLocation(final TypeElement annotation, final List<? extends Element> nonClassTypes) {
        final var msg = annotation.getQualifiedName() + " must be applied to a class";
        nonClassTypes.forEach(el -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, el));
    }

    ProcessingEnvironment processingEnv() {
        return processingEnv;
    }

    Types typeUtils() {
        return typeUtils;
    }

    /**
     * {@return the class that represents the field type, or null if the field is primitive}
     * @param fieldElement the element representing the field.
     */
    @Nullable TypeElement getClassTypeElement(final VariableElement fieldElement) {
        return getTypeMirrorAsElement(fieldElement.asType());
    }

    TypeElement getTypeMirrorAsElement(final TypeMirror genericType) {
        return (TypeElement) typeUtils().asElement(genericType);
    }
}
