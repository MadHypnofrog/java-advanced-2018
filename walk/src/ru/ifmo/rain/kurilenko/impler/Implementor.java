package ru.ifmo.rain.kurilenko.impler;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;


public class Implementor implements Impler, JarImpler {
    private static final String tab = "     ";

    /**
     * Produces code implementing interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name is the same as full name of the type token with <tt>Impl</tt> suffix
     * added. Generated source code is placed in the subdirectory of the specified <tt>root</tt> directory
     * corresponding to interface's package and has the correct filename. For example, the implementation of the
     * interface {@link java.util.List} goes to <tt>$root/java/util/ListImpl.java</tt>
     *
     *
     * @param token type token to create implementation for
     * @param root root directory
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException when implementation cannot be generated
     */
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
                w.write ("package " + token.getPackage().getName() + ";" + System.lineSeparator() + System.lineSeparator());
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
                for (Annotation a: m.getAnnotations()) {
                    w.write("@" + a.annotationType().getCanonicalName() + System.lineSeparator());
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

    /**
     * Produces <tt>.jar</tt> file implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name is the same as full name of the type token with <tt>Impl</tt> suffix
     * added.
     *
     * @param token type token to create implementation for
     * @param jarFile target <tt>.jar</tt> file
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException when implementation cannot be generated
     */
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        implement(token, jarFile.getParent());
        String tokenPath = token.getPackage().getName().replace('.', '/') + '/' + token.getSimpleName() + "Impl";
        String classToCompile = jarFile.getParent().resolve(tokenPath).toString();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler.run(null, null, null, classToCompile + ".java") != 0) {
            throw new ImplerException("Error: cannot compile .java file");
        }

        try {
            if (jarFile.getParent() != null) {
                Files.createDirectories(jarFile.getParent());
            }
        } catch (Exception e) {
            throw new ImplerException("Error: can't create directories for .jar file");
        }

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream os = new JarOutputStream(new FileOutputStream(jarFile.toString()), manifest)) {
            os.putNextEntry(new ZipEntry(tokenPath + ".class"));
            Files.copy(Paths.get(classToCompile + ".class"), os);
            os.closeEntry();
        } catch (Exception e) {
            throw new ImplerException(e.getMessage());
        }
    }
}