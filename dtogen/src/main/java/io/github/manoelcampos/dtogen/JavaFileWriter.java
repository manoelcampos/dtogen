package io.github.manoelcampos.dtogen;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Manoel Campos
 */
public class JavaFileWriter {
    private final DTOProcessor processor;

    public JavaFileWriter(final DTOProcessor processor) {
        this.processor = processor;
    }

    public void write(final String packageName, final String className, final String classContent) {
        try (final var out = newJavaFileWriter(packageName, className)) {
            out.printf(classContent);
        } catch (final IOException e) {
            processor.processingEnv().getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Error creating JavaFileObject to write the DTO generated code: " + e.getMessage(), null);
        }
    }

    /**
     * Creates a new {@link PrintWriter} for a {@link JavaFileObject} to write the DTO record file.
     *
     * @param packageName   the name of the package where the record file will be placed (ending with a dot if not empty).
     * @param dtoRecordName the name of the DTO record
     * @return
     */
    private PrintWriter newJavaFileWriter(final String packageName, final String dtoRecordName) throws IOException {
        final var point = packageName.isBlank() ? "" : ".";
        final var javaFileObj = processor.processingEnv().getFiler().createSourceFile(packageName + point + dtoRecordName);
        return new PrintWriter(javaFileObj.openWriter());
    }
}
