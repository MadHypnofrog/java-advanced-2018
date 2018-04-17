package ru.ifmo.rain.kurilenko.impler;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A wrapper for {@link Method}.
 */
public class MethodWrapper {

    /**
     * A method that this MethodWrapper holds.
     */
    private Method method;

    /**
     * Returns the {@link Method} this MethodWrapper holds.
     * <p>
     *
     * @return the {@link Method} this MethodWrapper holds
     */
    public Method getMethod () {
        return method;
    }

    /**
     * Creates a new MethodWrapper with the specified {@link Method} inside.
     * <p>
     *
     * @param m a {@link Method} for this MethodWrapper to hold
     */
    MethodWrapper(Method m) {
        method = m;
    }

    /**
     * Checks if this MethodWrapper is equal to another {@link Object}.
     * <p>
     * @param obj an {@link Object} to compare this MethodWrapper to
     * @return <code>true</code> if this MethodWrapper is equal to <code>obj</code>, <code>false</code> otherwise
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof MethodWrapper) {
            MethodWrapper other = (MethodWrapper) obj;
            return Arrays.equals(method.getParameterTypes(), other.method.getParameterTypes())
                    && method.getReturnType().equals(other.method.getReturnType())
                    && method.getName().equals(other.method.getName());
        }
        return false;
    }

    /**
     * Calculates the hash for this MethodWrapper using polynomial hash algorithm.
     * @return the hash code for this MethodWrapper
     */
    public int hashCode() {
        return ((Arrays.hashCode(method.getParameterTypes())
                + 61 * method.getReturnType().hashCode()) % ((int)1e9 + 7)
                + method.getName().hashCode() * 61 * 61) % ((int)1e9 + 7);
    }
}
