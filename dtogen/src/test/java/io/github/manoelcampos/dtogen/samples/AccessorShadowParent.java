package io.github.manoelcampos.dtogen.samples;

/**
 * Declares a private, unrelated {@code setId} overload that shadows the name (but not the
 * visibility) of the public {@code setId} method declared in {@link AccessorShadowGrandParent}.
 * @author Manoel Campos
 */
@SuppressWarnings("unused")
public class AccessorShadowParent extends AccessorShadowGrandParent {
    private void setId(final String id) {
    }
}
