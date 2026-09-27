package io.github.manoelcampos.dtogen.samples;

/**
 * A plain class declaring a public {@code setId} method, several levels up the hierarchy
 * of {@link AccessorShadowChild}.
 * Used to check that {@link io.github.manoelcampos.dtogen.util.TypeUtil#getPublicMethod}
 * keeps searching up the superclass chain when a same-named but non-public method
 * is found in a closer superclass (see {@link AccessorShadowParent}).
 * @author Manoel Campos
 */
@SuppressWarnings("unused")
public class AccessorShadowGrandParent {
    public void setId(final long id) {
    }
}
