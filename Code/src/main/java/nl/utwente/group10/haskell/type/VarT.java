package nl.utwente.group10.haskell.type;

import com.google.common.collect.ImmutableList;

/**
 * Variable type. Validates checked types against constraints. Holds identifier
 */
public class VarT extends Type {
    /**
     * Identifier for type checking within {@code FuncT} and {@code TupleT} types.
     */
    private final String name;

    /**
     * The types that form the constraints of this variable type.
     */
    private final ImmutableList<Type> types;

    /**
     * @param name Identifier for type checking within {@code FuncT} and {@code TupleT} types. Identifiers are converted
     *           to lower case.
     * @param types Constraints for accepted types. This {@code VarT} can be replaced with any type matching one of
     *              these types.
     */
    public VarT(final String name, final Type ... types) {
        this.name = name.toLowerCase();
        this.types = ImmutableList.of(types);
    }

    @Override
    public final boolean compatibleWith(final Type other) {
        boolean compatible = false;

        for (final Type type : this.types) {
            if (type.compatibleWith(other)) {
                compatible = true;
                break;
            }
        }

        return compatible;
    }

    /**
     * @return The name of this variable type.
     */
    public final String getName() {
        return this.name;
    }

    @Override
    public final String toHaskellType() {
        return this.name;
    }

    @Override
    public final String toString() {
        return "VarT{" +
                "name='" + this.name + "'" +
                ", types=" + this.types +
                '}';
    }
}
