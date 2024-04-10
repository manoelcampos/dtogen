package io.github.manoelcampos.dtogen;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.stream.Collectors.partitioningBy;

/**
 * Processes dtogen annotations to dynamically generate DTO records.
 * @author Manoel Campos
 * @see <a href="https://www.baeldung.com/java-annotation-processing-builder">Java Annotation Processing with Builder</a>
 */
@SupportedAnnotationTypes("io.github.manoelcampos.dtogen.DTO")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class DTOProcessor extends AbstractProcessor {
    private static final String DTO_INTERFACE_NAME = "DTORecord";
    private Types typeUtils;
    private JavaFileWriter javaFileWriter;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.typeUtils = processingEnv.getTypeUtils();
        this.javaFileWriter = new JavaFileWriter(this);
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        for (final var annotation : annotations) {
            final var annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

            final var annotatedElementsMap = annotatedElements.stream().collect(partitioningBy(el -> el.getKind().isClass()));
            //Gets only the classes annotated with @DTO
            final var classElements = annotatedElementsMap.get(true);
            classElements.stream().findFirst().ifPresent(this::createDtoInterface);
            classElements
                    .stream()
                    .map(classElement ->
                        new RecordGenerator(this,
                            classElement,
                            Predicate.not(this::isExcludedAnnotation),
                            DTOProcessor::isNotFieldExcluded,
                            DTO_INTERFACE_NAME)
                    ).forEach(RecordGenerator::generate);

            showInvalidAnnotationLocation(annotation, annotatedElementsMap.get(false));
        }

        return true;
    }

    private void createDtoInterface(final Element sourceClass) {
        final var classElement = (TypeElement) sourceClass;
        final var packageName = ClassUtil.getPackageName(classElement);
        final var dtoInterfaceCode =
                """
                package %1$s;
                public interface %2$s<T> {
                    T toModel();
                    %2$s<T> fromModel(T model);
                }
                """
                .formatted(packageName, DTO_INTERFACE_NAME);
        javaFileWriter.write(packageName, DTO_INTERFACE_NAME, dtoInterfaceCode);
    }

    /**
     * Annotations to be excluded from the DTO fields.
     * Check if an annotation is DTO one or a JPA/Hibernation annotation
     * that has only effect on database tables and should not be included in the DTO record.
     * @param annotation the annotation to check
     * @return true if the annotation is a JPA/Hibernation annotation, false otherwise.
     */
    private boolean isExcludedAnnotation(final AnnotationData annotation) {
        final var annotationNameList = List.of(
            DTOProcessor.class.getPackageName(),
            "jakarta.persistence.Id", "jakarta.persistence.GeneratedValue", "jakarta.persistence.Enumerated",
            "jakarta.persistence.OneToMany", "jakarta.persistence.ManyToOne",
            "jakarta.persistence.OneToOne", "jakarta.persistence.ManyToMany",
            "jakarta.persistence.Column", "jakarta.persistence.Lob", "jakarta.persistence.Column",
            "org.hibernate.annotations.JdbcTypeCode", "org.hibernate.annotations.ColumnDefault",
            "javax.annotation.meta.When", "lombok", "JsonIgnore"
        );

        return annotationNameList.stream().anyMatch(annotation.name()::contains);
    }

    static boolean isNotFieldExcluded(final VariableElement field) {
        return field.getAnnotation(DTO.Exclude.class) == null;
    }

    /**
     * Shows an error message for each element that is not a class and was annotated with the @DTO.
     * @param annotation the annotation being    processed.
     * @param nonClassTypes the list of elements that are not classes.
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
}
