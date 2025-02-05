package io.github.manoelcampos.dtogen;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Enables the creation of a Java file from a given String containing the Java code to be written.
 * The class is used to write the generated DTO record code to a Java file.
 * @author Manoel Campos
 */
public class JavaFileWriter {
    /**
     * The {@link DTOProcessor} which is being running to generate the DTO records.
     */
    private final DTOProcessor processor;

    /**
     * Creates a new JavaFileWriter object.
     * @param processor the DTO {@link #processor} instance.
     */
    public JavaFileWriter(final DTOProcessor processor) {
        this.processor = processor;
    }

    /**
     * Writes the code of the generated DTO record to a Java file
     * @param packageName the package the DTO record will be placed in
     * @param recordName the name of the DTO record generated, which is used as the file name
     * @param classContent the code for the Java file to be created
     */
    public void write(final String packageName, final String recordName, final String classContent) {
        try (final var out = newJavaFileWriter(packageName, recordName)) {
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
     * @return the new {@link PrintWriter} object
     */
    private PrintWriter newJavaFileWriter(final String packageName, final String dtoRecordName) throws IOException {
        final var point = packageName.isBlank() ? "" : ".";
        final var javaFileObj = processor.processingEnv().getFiler().createSourceFile(packageName + point + dtoRecordName);
        return new PrintWriter(javaFileObj.openWriter());
    }
}
