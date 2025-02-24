package io.github.manoelcampos.dtogen;

import io.github.manoelcampos.dtogen.samples.Class1;
import io.github.manoelcampos.dtogen.samples.ClassWithJavaDoc;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaDocExtractorTest extends AbstractToolsExtensionTest {

    @Test
    void getJavaFilePathFromQualifiedName() {
        final var instance = newInstance(Class1.class);
        final String expectedPath = "src/test/java/io/github/manoelcampos/dtogen/samples/Class1.java";
        assertEquals(expectedPath, instance.getJavaFilePathFromQualifiedName());
    }

    @Test
    void getFieldCommentsStreamForClassWithNoJavaDocInFields() {
        final var instance = newInstance(Class1.class);
        assertTrue(instance.getFieldCommentsStream().toList().isEmpty());
    }

    @Test
    void getFieldCommentsStreamForClassWithSomeJavaDocInFields() {
        final var instance = newInstance(ClassWithJavaDoc.class);
        final var javadocMap = new HashMap<String, String>();
        instance.getFieldCommentsStream().forEach(entry -> {
            javadocMap.put(entry.getKey(), entry.getValue());
        });

        final int numberOfDocumentedFields = 3;
        assertEquals(numberOfDocumentedFields, javadocMap.size());

        final var expectedValuesList = List.of(
                new AbstractMap.SimpleEntry<>("MAX", "A MAX constant."),
                new AbstractMap.SimpleEntry<>("id", "The id of the object."),
                new AbstractMap.SimpleEntry<>("name",
                    """
                    The name of the object.
                    <p>This is used to provide a user-friendly representation when printing the object.</p>"""
                )
        );

        expectedValuesList.forEach(expectedEntry -> {
            final String actualValue = javadocMap.get(expectedEntry.getKey());
            assertEquals(expectedEntry.getValue(), actualValue);
        });
    }

    private JavaDocExtractor newInstance(final Class<?> clazz) {
        return new JavaDocExtractor(getClassTypeElement(clazz), true);
    }
}
