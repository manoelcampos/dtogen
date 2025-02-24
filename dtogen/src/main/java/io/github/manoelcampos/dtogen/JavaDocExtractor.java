package io.github.manoelcampos.dtogen;

import com.sun.source.tree.*;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;

import javax.lang.model.element.TypeElement;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A class to extract the JavaDoc comments from a Java source file.
 * @author Manoel Campos
 */
public class JavaDocExtractor {
    /**
     * A map where the key is the name of a field and the value the JavaDoc comment for that field.
     */
    private final Map<String, String> fieldCommentsMap = new HashMap<>();

    /**
     * A type element representing a Java class, record, interface, etc.
     */
    private final TypeElement typeElement;


    /**
     * Indicates if the Java fiel is inside the test dir or not.
     */
    private final boolean testDir;

    /**
     * Creates an instance and extracts JavaDoc comments from the given Java element.
     * @param typeElement see {@link #typeElement}
     */
    public JavaDocExtractor(final TypeElement typeElement) {
        this(typeElement, false);
    }

    /**
     * Creates an instance and extracts JavaDoc comments from the given Java element.
     * @param typeElement see {@link #typeElement}
     * @param testDir see {@link #testDir}
     */
    JavaDocExtractor(final TypeElement typeElement, final boolean testDir) {
        this.typeElement = typeElement;
        this.testDir = testDir;
        final var javaFilePath = getJavaFilePathFromQualifiedName();
        System.out.println(javaFilePath);
        extract(javaFilePath);
    }

    /**
     * {@return the path of the Java source file for the given qualified name}
     */
    String getJavaFilePathFromQualifiedName() {
        final String path = typeElement.getQualifiedName().toString().replace(".", "/");
        final String dir = testDir ? "test" : "main";
        return "src/%s/java/%s.java".formatted(dir, path);
    }

    /**
     * Extracts the JavaDoc comments from a Java source file.
     */
    private void extract(final String javaFilePath) {
        Objects.requireNonNull(javaFilePath);
        try {
            // Step 1: Get Java Compiler
            final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            final var fileManager = compiler.getStandardFileManager(null, null, null);

            // Step 2: Specify the Java File
            final var sourceFile = Paths.get(javaFilePath).toFile();
            final var fileObjectsIterable = fileManager.getJavaFileObjects(sourceFile);

            // Step 3: Parse the Source File
            final var task = (JavacTask) compiler.getTask(null, fileManager, null, null, null, fileObjectsIterable);
            final var docTrees = DocTrees.instance(task);

            // Step 4: Analyze the Compilation Unit (AST)
            for (final CompilationUnitTree unitTree : task.parse()) {
                parseTypeDeclarationTree(unitTree, docTrees);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void parseTypeDeclarationTree(final CompilationUnitTree unitTree, final DocTrees docTrees) {
        for (final var tree : unitTree.getTypeDecls()) {
            parseClassTree(unitTree, docTrees, tree);
        }
    }

    private void parseClassTree(final CompilationUnitTree unitTree, final DocTrees docTrees, final Tree tree) {
        if (tree instanceof ClassTree classTree) {
            for (final Tree member : classTree.getMembers()) {
                parseField(unitTree, docTrees, member);
            }
        }
    }

    private void parseField(final CompilationUnitTree unitTree, final DocTrees docTrees, final Tree member) {
        if (member instanceof VariableTree) {
            final var optional = extractJavaDoc(docTrees, TreePath.getPath(unitTree, member));
            optional.ifPresent(javadoc -> fieldCommentsMap.put(((VariableTree) member).getName().toString(), javadoc));
        }
    }

    private Optional<String> extractJavaDoc(final DocTrees docTrees, final TreePath path) {
        final var docCommentTree = docTrees.getDocCommentTree(path);
        final boolean hasJavaDoc = docCommentTree == null || docCommentTree.toString().isBlank();

        /* Replaces all line brakes that starts the new line with a space by just a line break,
        * since the trim() only removes spaces at begin and end, not between lines. */
        return hasJavaDoc ?
                Optional.empty() :
                Optional.of(docCommentTree.toString().trim().replaceAll("\n ", "\n"));
    }

    public Stream<Entry<String, String>> getFieldCommentsStream() {
        return fieldCommentsMap.entrySet().stream();
    }

    public static String getMemberName(final Tree member) {
        return switch (member.getKind()) {
            case METHOD -> ((MethodTree) member).getName().toString();
            case VARIABLE -> ((VariableTree) member).getName().toString();
            case CLASS -> ((ClassTree) member).getSimpleName().toString();
            default -> "";
        };
    }
}
