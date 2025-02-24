package io.github.manoelcampos.dtogen;

import com.karuslabs.elementary.junit.Tools;
import com.karuslabs.elementary.junit.annotations.Processors;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * An abstract base class for tests that use the {@link DTOProcessor} class.
 * @author Manoel Campos
 */
@Processors({DTOProcessor.class})
public abstract class AbstractProcessorTest extends AbstractToolsExtensionTest {
    private final ProcessingEnvironment env = Mockito.mock(ProcessingEnvironment.class);
    protected DTOProcessor processor;

    @BeforeEach
    void setUp() {
        Mockito.when(env.getTypeUtils()).thenReturn(Tools.types());
        this.processor = new DTOProcessor(env);
    }
}
