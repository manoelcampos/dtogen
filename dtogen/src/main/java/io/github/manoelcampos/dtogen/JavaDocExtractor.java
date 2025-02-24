package io.github.manoelcampos.dtogen;

import com.sun.source.tree.*;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to extract the JavaDoc comments from a Java source file.
 * @author Manoel Campos
 */
public class JavaDocExtractor {
    /**
     * A map with the JavaDoc comments for each field into a given java file.
     */
    private final Map<VariableTree, String> fieldCommentsMap = new HashMap<>();

    /**
     * Extracts the JavaDoc comments from a Java source file.
     * @param javaFilePath the path to the Java source file
     */
    public void extract(final String javaFilePath) {
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
                for (final var tree : unitTree.getTypeDecls()) {
                    if (tree instanceof ClassTree classTree) {
                        final var classPath = TreePath.getPath(unitTree, classTree);
                        extractJavaDoc(docTrees, classPath, classTree);

                        // Extract Javadoc for fields and methods
                        for (final Tree member : classTree.getMembers()) {
                            if (member instanceof MethodTree || member instanceof VariableTree) {
                                extractJavaDoc(docTrees, TreePath.getPath(unitTree, member), member);
                            }
                        }
                    }
                }
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void extractJavaDoc(final DocTrees docTrees, final TreePath path, final Tree member) {
        final var docCommentTree = docTrees.getDocCommentTree(path);
        if (docCommentTree != null) {
            System.out.printf("Javadoc for %s %s%n", member.getKind(), getMemberName(member));
            System.out.println(docCommentTree);
            System.out.println("---------------------------------------------------");
        }
    }

    private static String getMemberName(final Tree member) {
        return switch (member.getKind()) {
            case METHOD -> ((MethodTree) member).getName().toString();
            case VARIABLE -> ((VariableTree) member).getName().toString();
            case CLASS -> ((ClassTree) member).getSimpleName().toString();
            default -> "";
        };
    }

    public static void main(String[] args) {
       new JavaDocExtractor().extract("src/main/java/io/github/manoelcampos/dtogen/JavaDocExtractor.java");
    }
}
