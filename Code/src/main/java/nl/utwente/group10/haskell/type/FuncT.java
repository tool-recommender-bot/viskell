package nl.utwente.group10.haskell.type;

import nl.utwente.group10.ghcj.HaskellException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Type of a Haskell function. Consists of multiple types in a fixed order.
 */
public class FuncT extends Type {
    /**
     * The argument types for this function type.
     */
    private final Type[] arguments;

    /**
     * @param arguments The argument types for this function type.
     */
    public FuncT(final Type... arguments) {
        this.arguments = arguments.clone();
    }

    /**
     * @return The number of arguments for this function type, excluding the resulting type.
     */
    public final int getNumArgs() {
        return this.arguments.length - 1;
    }

    /**
     *
     * @param args Arguments to apply. The number of arguments should be less or equal to {@code this.getNumArgs()}.
     * @return The resulting type of the application.
     * @throws AssertionError
     * @throws HaskellException Invalid Haskell operation. See exception message for details.
     */
    public final Type getAppliedType(final Type ... args) throws HaskellException {
        assert args.length < this.arguments.length;

        final Map<String, Type> varTypes = new HashMap<String, Type>();
        final Type[] resultArguments = new Type[this.arguments.length - args.length];

        // Determine resulting type of each argument and build resulting type
        int j = 0;
        for (int i = 0; i < this.arguments.length; i++) {
            Type expectedType = this.arguments[i]; // Expected type

            // Check whether the expected type is a VarT instance, if so, do some extra processing
            if (expectedType instanceof VarT) {
                final String variableName = ((VarT) expectedType).getName();

                if (varTypes.containsKey(variableName)) {
                    // If we already determined the real type of the VarT, set the expected type accordingly.
                    expectedType = varTypes.get(variableName);
                } else if (i < args.length) {
                    // If this is the first occurance of this variable, set its actual type but keep the VarT as
                    // expected type to check compatibility later on.
                    varTypes.put(variableName, args[i]);
                }
            }

            // Check whether the type that will be applied is compatible with the expected type. The expected type can
            // be a VarT or (a VarT replaced by) a non-variable type.
            if (i < args.length && !expectedType.compatibleWith(args[i])) {
                throw new HaskellException(null); // TODO Improve this exception with a message
            } else if (i >= args.length) {
                resultArguments[j] = expectedType;
                j++;
            }
        }

        // Build type
        return resultArguments.length == 1 ? resultArguments[0] : new FuncT(resultArguments);
    }

    @Override
    public final boolean compatibleWith(final Type other) {
        boolean compatible = true;

        if (other instanceof FuncT) {
            // If the other type is a {@code FuncT} instance, compare all overlapping subtypes.
            final Type[] otherArguments = ((FuncT) other).arguments;

            if (this.arguments.length >= otherArguments.length) {
                for (int i = 0; i < otherArguments.length && compatible; i++) {
                    compatible = this.arguments[i].compatibleWith(otherArguments[i]);
                }
            }
        } else if (this.arguments.length > 0) {
            // If not, compare the compatibility of the first argument.
            compatible = this.arguments[0].compatibleWith(other);
        } else {
            // If this {@code FuncT} does not have any arguments, the other type is per definition not compatible.
            compatible = false;
        }

        return compatible;
    }

    @Override
    public final String toHaskellType() {
        final StringBuilder out = new StringBuilder();
        out.append("(");


        for (int i = 0; i < this.arguments.length; i++) {
            out.append(this.arguments[i].toHaskellType());
            if (i + 1 < this.arguments.length) {
                out.append(" -> ");
            }
        }

        out.append(")");
        return out.toString();
    }

    @Override
    public final String toString() {
        return "FuncT{" +
                "arguments=" + Arrays.toString(this.arguments) +
                '}';
    }
}
