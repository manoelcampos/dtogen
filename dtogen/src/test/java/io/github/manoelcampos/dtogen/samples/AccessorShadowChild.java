package io.github.manoelcampos.dtogen.samples;

/**
 * Has no {@code setId} method of its own, relying on the one inherited from
 * {@link AccessorShadowGrandParent} (two levels up, past the unrelated private
 * overload declared in {@link AccessorShadowParent}).
 * @author Manoel Campos
 */
@SuppressWarnings("unused")
public class AccessorShadowChild extends AccessorShadowParent {
}
