package io.github.manoelcampos.dtogen;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNullElse;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Utility functions for tests.
 * @author Manoel Campos
 */
public class TestUtil {
    public static void assertCodeEquals(String expectedCode, String actualCode) {
        expectedCode = removeDoubleEmptyLines(expectedCode);
        actualCode = removeDoubleEmptyLines(actualCode);

        /* Uses an "if" before the assertEquals to ensure the comparison ignores spaces,
         * but the error message when the assertion fails will include the spaces. */
        if (!removeSpaces(expectedCode).equals(removeSpaces(actualCode)))
            assertEquals(expectedCode, actualCode, "The generated code is not as expected");
    }

    /**
     * Removes spaces anywhere into a String
     *
     * @param text the String to remove the spaces
     * @return the String with no spaces at all
     */
    private static String removeSpaces(final String text) {
        return requireNonNullElse(text, "").replaceAll("\\s+", "");
    }

    /**
     * Replaces double empty lines (even the ones that are formed just by spaces) by one.
     *
     * @param text the text to remove double empty lines
     * @return the new text
     */
    private static String removeDoubleEmptyLines(String text) {
        text = requireNonNullElse(text, "");
        final String emptySpacesLineRegex = "^\\s+$";
        final var pattern = Pattern.compile(emptySpacesLineRegex, Pattern.MULTILINE);
        final var matcher = pattern.matcher(text);
        return matcher.replaceAll("").replaceAll("\\n{2,}", "\n\n");
    }

    /**
     * Finds a field in a class.
     * @param elements {@link Elements} instance that provides utility methods to find elements in a class
     * @param clazz the get to get its fields
     * @param elementName the name of the field to find
     * @return the field element found or null if not found
     */
    static VariableElement findField(final Elements elements, final Class<?> clazz, final String elementName) {
        final var classType = elements.getTypeElement(clazz.getCanonicalName());
        return (VariableElement) findElement(elements, classType, elementName, e -> e.getKind().isField());
    }

    private static Element findElement(final Elements elements, final TypeElement classType, final String elementName, final Predicate<Element> elementFilter) {
        return elements
                .getAllMembers(classType)
                .stream()
                .filter(elementFilter)
                .filter(e -> e.getSimpleName().toString().equals(elementName))
                .findFirst()
                .orElseThrow();
    }
}
