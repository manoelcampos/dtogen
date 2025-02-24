package io.github.manoelcampos.dtogen;

import io.github.manoelcampos.dtogen.util.TypeUtil;

import java.io.*;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

/**
 * An utility class to read Java source files.
 * @author Manoel Campos
 */
public class JavaFileReader {
    /** A private constructor to avoid class instantiation. */
    private JavaFileReader() {/**/}


    /**
     * Reads a Java source file from the resources folder inside the DTOGen jar.
     * Since this file is further copied to the application that uses the DTOGen,
     * the package name inside the source file is replaced
     * by the package where the application's model class is located.
     *
     * @param sourceFileName source file simple name (including .java extension)
     * @param packageName name of the package to replace inside the source file code
     * @return the source file code
     * @throws UncheckedIOException if the file is not found inside the jar resources dir or cannot be read
     */
    public static String readFromResources(final String sourceFileName, final String packageName){
        // The file to be read must be inside the same package of the DTOProcessor class
        final var inputStream = DTOProcessor.class.getResourceAsStream(sourceFileName);
        if(inputStream == null)
            throw new UncheckedIOException(new FileNotFoundException("Resource not found inside the DTOGen jar: " + sourceFileName));

        final String newPackageName = "package %s;%n".formatted(packageName);
        final Function<String, String> packageReplacer = line -> line.startsWith("package") ? newPackageName : line;

        try (var fileReader = new BufferedReader(new InputStreamReader(inputStream))){
            return fileReader.lines()
                             .filter(TypeUtil::isNotThreeSlashesComment)
                             .map(packageReplacer)
                             .collect(joining(System.lineSeparator()));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
