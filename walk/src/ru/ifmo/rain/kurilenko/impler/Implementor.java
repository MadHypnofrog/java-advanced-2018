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
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        implement(token, jarFile);
        String toCompile = jarFile/*.getParent()*/
                .resolve(token.getPackage().getName().replace('.', File.separatorChar))
                .toString() + File.separatorChar + token.getSimpleName() + "Impl";
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler.run(null, null, null, toCompile + ".java") != 0) {
            throw new ImplerException("Error: cannot compile .java file");
        }
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0"); //this isn't supposed to work like that
                                                                                         //it's not actually creating a .jar but a folder ending with .jar
        /*try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toString() + "\\"), manifest);
             FileInputStream in = new FileInputStream(toCompile + ".class");
             JarOutputStream jos2 = new JarOutputStream(new FileOutputStream(token.getSimpleName() + ".jar"), manifest)) {
            //System.out.println(jarFile.toString());
            jos.putNextEntry(new ZipEntry((toCompile + ".class").replace(toCompile.split("\\\\")[0] + "\\", "")));
            jos2.putNextEntry(new ZipEntry((toCompile + ".class").replace(toCompile.split("\\\\")[0] + "\\", "")));
            byte[] buffer = new byte[1024];
            int len;
            while((len = in.read(buffer)) > 0) {
                jos.write(buffer, 0, len);
                jos2.write(buffer, 0, len);
            }
            jos.closeEntry();
            jos2.closeEntry();
        } catch (Exception e) {
            throw new ImplerException(e.getMessage());
        }*/
    }
}