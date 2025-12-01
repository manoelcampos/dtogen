package io.github.manoelcampos.dtogen.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import java.util.Optional;

import static io.github.manoelcampos.dtogen.util.AccessorMethod.AccessorType.GETTER;

/**
 * Get metadata about an accessor method (getter/setter) for a given field.
 * @author Manoel Campos
 */
public final class AccessorMethod {
    private static final String SETTER_PREFIX = "set";
    private final AccessorType type;
    private final Optional<? extends Element> accessorOptional;
    /**
     * Accessor method name (the name of the getter or setter for the given field).
     */
    private final String name;
    private final String sourceFieldName;
    private final Element containingClassRecord;

    public enum AccessorType {
        GETTER,
        SETTER;
    }
    private final TypeUtil typeUtil;

    private final VariableElement sourceField;
    /**
     * @param sourceField the field to get information about its accessors
     *
     */
    public AccessorMethod(final TypeUtil typeUtil, final VariableElement sourceField, final AccessorType type) {
        this.typeUtil = typeUtil;
        this.sourceField = sourceField;
        this.type = type;
        this.sourceFieldName = sourceField.getSimpleName().toString();
        this.containingClassRecord = sourceField.getEnclosingElement();
        final boolean isRecord = TypeUtil.isRecord(containingClassRecord);
        final var formatedFieldName = isRecord ? sourceFieldName : FieldUtil.getUpCaseFieldName(sourceFieldName);

        final var prefix = prefix(isRecord);
        // If there is an accessor, returns it, otherwise, returns the field name to be accessed directly
        this.name = "%s%s".formatted(prefix, formatedFieldName);

        this.accessorOptional = TypeUtil.getPublicMethod(containingClassRecord, name);
    }

    /**
     * Check if the accessor exists or there is a public field
     * to be accessed directly.
     * @throws UnsupportedOperationException if there is no accessor and the field is not public
     */
    public void checkAccess() {
        if(missing() && noPublicField()) {
            final var msg =
                    "There is no public %s %s and no public field %s to be accessed inside %s"
                            .formatted(
                                    type.name().toLowerCase(), name(),
                                    sourceFieldName, containingClassRecord.getSimpleName());
            throw new UnsupportedOperationException(msg);
        }

    }

    public Optional<? extends Element> accessor() {
        return accessorOptional;
    }

    /**
     * @return true if an accessor for the given field was NOT found, false otherwise
     */
    public boolean missing(){
        return accessorOptional.isEmpty();
    }

    /**
     * @return true if an accessor for the given field was found, false otherwise
     */
    public boolean existing(){
        return accessorOptional.isPresent();
    }


    public String name(){ return name; }

    public String sourceFieldName(){ return sourceFieldName; }

    /**
     * {@return the prefix for the accessor}
     * It depends on the {@link AccessorType}.
     * @param isRecord if the containing class is a record or not
     *                 (where there aren't setters and getters have no prefix)
     *
     */
    private String prefix(final boolean isRecord) {
        return type.equals(GETTER) ? getterPrefix(sourceField, isRecord) : SETTER_PREFIX;
    }

    /**
     * {@return the prefix for a getter method inside a class or record}
     * @param sourceField the field to obtain the getter prefix
     *                    (because the field type interfere on the getter prefix)
     * @param isRecord indicate if the enclosing element (where the getter is contained)
     *                 is a record or a class.
     */
    private String getterPrefix(final VariableElement sourceField, final boolean isRecord) {
        final var prefixForGetterInsideClass = typeUtil.isBooleanType(sourceField) ? "is" : "get";
        return isRecord ? "" : prefixForGetterInsideClass;
    }

    /**
     * @return true if no public field was found for the accessor
     *         (there may be a field, but with other visibility),
     *         false otherwise.
     */
    public boolean noPublicField(){
        return !TypeUtil.isPublic(sourceField);
    }
}
