package ru.ifmo.rain.kurilenko.impler;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;


public class Implementor implements Impler {
    private static final String tab = "     ";
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null) {
            throw new ImplerException("Error: token is null");
        }
        if (root == null) {
            throw new ImplerException("Error: root is null");
        }
        if (!token.isInterface()) {
            throw new ImplerException("Error: token is not an interface");
        }

        String classname = token.getSimpleName() + "Impl";
        Path path = root.resolve(token.getPackage().getName().replace('.', File.separatorChar));

        try {
            Files.createDirectories(path);
        } catch (Exception e) {
            throw new ImplerException("Error: can't create directories");
        }

        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(path.toString() + File.separatorChar + classname + ".java"))) {
            //Package
            if (token.getPackage() != null && !token.getPackage().getName().equals("")) {
                w.write ("package " + token.getPackage().getName() + ";" + System.lineSeparator());
            }

            //Class name
            int mods = token.getModifiers();
            if (Modifier.isAbstract(mods)) {
                mods -= Modifier.ABSTRACT;
            }
            if (Modifier.isInterface(mods)) {
                mods -= Modifier.INTERFACE;
            }
            w.write (Modifier.toString(mods) + " class " + classname + " implements " + token.getCanonicalName() + " {" + System.lineSeparator());

            //Methods
            for (Method m: token.getMethods()) {
                if (m.isDefault()) {
                    continue;
                }
                int mmods = m.getModifiers();
                if (Modifier.isTransient(mmods)) {
                    mmods -= Modifier.TRANSIENT;
                }
                if (Modifier.isAbstract(mmods)) {
                    mmods -= Modifier.ABSTRACT;
                }
                w.write (tab + Modifier.toString(mmods) + " " + m.getReturnType().getCanonicalName() + " " + m.getName() + " (");

                //Method arguments
                w.write(Arrays.stream(m.getParameters())
                        .map(p -> p.getType().getCanonicalName() + " " + p.getName())
                        .collect(Collectors.joining(", ")));
                w.write (")");

                //Method exceptions
                if (m.getExceptionTypes().length != 0) {
                    w.write (" throws ");
                    w.write (Arrays.stream(m.getExceptionTypes())
                            .map(Class::getCanonicalName)
                            .collect(Collectors.joining (", ")));
                }

                //Method body
                w.write (" {" + System.lineSeparator());
                if (m.getReturnType() != void.class) {
                    w.write (tab + tab + "return ");
                    if (m.getReturnType() == boolean.class) {
                        w.write ("false;");
                    }
                    else if (m.getReturnType().isPrimitive()) {
                        w.write ("0;");
                    }
                    else {
                        w.write ("null;");
                    }
                }
                w.write (System.lineSeparator() + tab + "}" + System.lineSeparator());
            }
            w.write ("}");
        } catch (Exception e) {
            throw new ImplerException(e.getMessage());
        }
    }
}
