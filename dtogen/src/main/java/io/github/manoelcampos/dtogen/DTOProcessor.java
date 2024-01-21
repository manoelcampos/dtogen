package io.github.manoelcampos.dtogen;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
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
    private Types typeUtils;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        for (final var annotation : annotations) {
            final var annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

            final var annotatedElementsMap = annotatedElements.stream().collect(partitioningBy(el -> el.getKind().isClass()));
            //Gets only the classes annotated with @DTO
            final var classElements = annotatedElementsMap.get(true);
            classElements.forEach(this::generateDtoRecord);

            showInvalidAnnotationLocation(annotation, annotatedElementsMap.get(false));
        }

        return true;
    }

    /**
     * Generate the DTO record file for the annotated class.
     * @param classElement element representing a class where the annotation was applied.
     */
    private void generateDtoRecord(final Element classElement) {
        final var classTypeElement = (TypeElement) classElement;
        final var packageName = getPackageName(classTypeElement);
        final var dtoRecordName = classTypeElement.getSimpleName() + "DTO";
        final JavaFileObject dtoFile = newJavaFileObject(packageName, dtoRecordName);
        if(dtoFile == null)
            return;

        final String fields = dtoFields(classTypeElement);
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, dtoRecordName, classElement);
        try (final var out = new PrintWriter(dtoFile.openWriter())) {
            if(!packageName.isBlank())
                out.printf("package %s;%n%n", packageName);

            out.printf("public record %s (%s) {}%n", dtoRecordName, fields);
        } catch (final IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), classElement);
        }
    }

    private static String getPackageName(final TypeElement classTypeElement) {
        final var qualifiedClassName = classTypeElement.getQualifiedName().toString();
        return qualifiedClassName.substring(0, qualifiedClassName.lastIndexOf("."));
    }

    /**
     * {@return a string with the DTO record fields} It is based on the fields of the annotated class.
     * @param classTypeElement element representing the class where the annotation was applied.
     */
    private String dtoFields(final TypeElement classTypeElement) {
        return getClassFields(classTypeElement)
                        .filter(field -> field.getAnnotation(DTO.Exclude.class) == null)
                        .map(field -> createDtoRecordField(classTypeElement, field))
                        .collect(joining(", "));
    }

    private Stream<VariableElement> getClassFields(final TypeElement classTypeElement) {
        return classTypeElement.getEnclosedElements().stream()
                               .filter(enclosedElement -> enclosedElement.getKind().isField())
                               .map(enclosedElement -> (VariableElement) enclosedElement)
                               .filter(this::isInstanceField);
    }

    private boolean isInstanceField(final VariableElement field) {
        return !field.getModifiers().contains(javax.lang.model.element.Modifier.STATIC);
    }

    private String createDtoRecordField(final TypeElement classTypeElement, final VariableElement field) {
        final var fieldAnnotationMirrors = field.getAnnotationMirrors();
        final var fieldAnnotationsStr = getFieldAnnotationsStr(fieldAnnotationMirrors);
        if(fieldAnnotationMirrors.stream().anyMatch(mirror -> isAnnotationEqualTo(mirror, DTO.MapToId.class))){
            final var fieldType = getClassTypeElement(field);
            final var msg = "Cannot find id field in %s. Since the %s.%s is annotated with %s, it must be a class with an id field."
                              .formatted(fieldType.getSimpleName(), classTypeElement.getSimpleName(), field.getSimpleName(), getAnnotationName(DTO.MapToId.class));

            return findIdField(fieldType)
                    .map(idField -> String.format("%s %s %s", getFieldAnnotationsStr(idField.getAnnotationMirrors()), getFieldType(idField), field.getSimpleName() + "Id"))
                    .orElseGet(() -> {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, field);
                        return "";
                    });
        }

        return String.format("%s %s %s", fieldAnnotationsStr, getFieldType(field), field.getSimpleName());
    }

    /**
     * Checks if an annotation mirror represents a given annotation class,
     * that is, if a given annotation is the one we are looking for.
     * @param mirror the annotation mirro that enables getting annotation metadata
     * @param annotationClass the class of the annotation we are looking for
     * @return
     */
    private static boolean isAnnotationEqualTo(final AnnotationMirror mirror, final Class<?> annotationClass) {
        return mirror.getAnnotationType().toString().endsWith(getAnnotationName(annotationClass));
    }

    /**
     * Gets the fully qualified name of an annotation class, replacing the dollar sign ($) by a dot (.) for annotations
     * defined inside other ones (such as {@link DTO.MapToId}).
     * @param annotationClassClass the annotation class to get its name
     * @return
     */
    private static String getAnnotationName(final Class<?> annotationClassClass) {
        return annotationClassClass.getName().replaceAll("\\$", ".");
    }

    private String getFieldAnnotationsStr(List<? extends AnnotationMirror> fieldAnnotationMirrors) {
        return fieldAnnotationMirrors
                .stream()
                .filter(this::isNotDTOGenAnnotation)
                .map(this::getAnnotation)
                .collect(joining(" "));
    }

    private Optional<VariableElement> findIdField(final TypeElement fieldType) {
        return getClassFields(fieldType).filter(f -> f.getSimpleName().toString().equals("id")).findFirst();
    }

    /**
     * Gets the class that represents the type of a field.
     * @param fieldElement the element representing the field.
     * @return
     */
    private TypeElement getClassTypeElement(final VariableElement fieldElement) {
        final var fieldTypeMirror = fieldElement.asType();
        return (TypeElement) typeUtils.asElement(fieldTypeMirror);
    }

    /**
     * Checks if a field annotation is not from the dtogen package.
     */
    private boolean isNotDTOGenAnnotation(final AnnotationMirror mirror) {
        return !mirror.getAnnotationType().toString().startsWith(this.getClass().getPackageName());
    }

    private String getAnnotation(final AnnotationMirror mirror) {
        final var annotationType = mirror.getAnnotationType().toString();
        final var annotationValues = ElementFilter.methodsIn(mirror.getElementValues().keySet())
                     .stream()
                     .map(element -> getAnnotationAttribute(mirror, element))
                     .collect(joining(", "));

        return String.format("@%s(%s)", annotationType, annotationValues);
    }

    private static String getAnnotationAttribute(final AnnotationMirror mirror, final ExecutableElement annotationAttributeElement) {
        final Object value = mirror.getElementValues().get(annotationAttributeElement).getValue();
        return annotationAttributeElement.getSimpleName() + "=" + formatAnnotationValue(value);
    }

    private static String formatAnnotationValue(final Object value) {
        return value instanceof String ? String.format("\"%s\"", value) : value.toString();
    }

    private String getFieldType(final VariableElement fieldElement) {
        return getSimpleTypeName(fieldElement);
    }

    private String getSimpleTypeName(final VariableElement fieldElement) {
        final var typeMirror = fieldElement.asType();
        if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.getKind().toString().toLowerCase();
        } else if (typeMirror instanceof DeclaredType declaredType) {
            return declaredType.asElement().getSimpleName().toString();
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unsupported type: " + typeMirror, fieldElement);
        return typeMirror.toString();
    }

    /**
     * Creates a new JavaFileObject to write the DTO record file.
     * @param packageName the name of the package where the record file will be placed (ending with a dot if not empty).
     * @param dtoRecordName the name of the DTO record
     * @return
     */
    private JavaFileObject newJavaFileObject(final String packageName, final String dtoRecordName) {
        final var dot = packageName.isBlank() ? "" : ".";
        try {
            return processingEnv.getFiler().createSourceFile(packageName + dot + dtoRecordName);
        } catch (final IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating JavaFileObject to create DTO java file: "+ e.getMessage(), null);
            return null;
        }
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
}
