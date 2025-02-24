package io.github.manoelcampos.dtogen;

import com.karuslabs.elementary.junit.Tools;
import com.karuslabs.elementary.junit.ToolsExtension;
import com.karuslabs.utilitary.type.TypeMirrors;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * An abstract base class for tests that use the {@link Tools} class
 * to create objects required by Annotation Processors such as the DTOGen.
 *
 * @author Manoel Campos
 */
@ExtendWith(ToolsExtension.class)
public abstract class AbstractToolsExtensionTest {
    protected final Elements elements = Tools.elements();
    protected final Types types = Tools.types();
    protected final TypeMirrors typeMirrors = Tools.typeMirrors();
}
