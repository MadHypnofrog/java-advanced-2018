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
    /**
     * 4 spaces in a row.
     */
    private static final String tab = "     ";

    /**
     * Produces code implementing interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name is the same as full name of the type token with <tt>Impl</tt> suffix
     * added. Generated source code is placed in the subdirectory of the specified <tt>root</tt> directory
     * corresponding to interface's package and has the correct filename. For example, the implementation of the
     * interface {@link java.util.List} goes to <tt>$root/java/util/ListImpl.java</tt>
     *
     * @param token type token to create implementation for
     * @param root  root directory
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

        Path path = root.resolve(token.getPackage().getName().replace('.', File.separatorChar));

        try {
            Files.createDirectories(path);
        } catch (Exception e) {
            throw new ImplerException("Error: can't create directories");
        }

        try (Writer w = new FilterWriter(new FileWriter(path.toString() + File.separatorChar + token.getSimpleName() + "Impl.java")) {
            @Override
            public void write(String str, int off, int len) throws IOException {
                for (int i = off; i < off + len; i++) {
                    char c = str.charAt(i);
                    if (c >= 128) {
                        out.write("\\u" + String.format("%04X", (int) c));
                        //System.out.println(c);
                    } else {
                        out.write(c);
                    }
                }
            }
        }) {
            //Package
            if (token.getPackage() != null && !token.getPackage().getName().equals("")) {
                w.write("package " + token.getPackage().getName() + ";" + System.lineSeparator() + System.lineSeparator());
            }

            //Class name
            writeClass(token, w);

            //Methods
            writeMethods(token.getMethods(), w);

            w.write("}");
        } catch (Exception e) {
            throw new ImplerException(e.getMessage());
        }
    }

    /**
     * Produces code of a class implementing interface specified by provided <tt>token</tt>.
     * <p>
     *
     * @param token type token to write a class for
     * @param w     Writer to write in
     * @throws IOException if an error has occurred during writing
     */
    private void writeClass(Class<?> token, Writer w) throws IOException {
        int mods = token.getModifiers();
        if (Modifier.isAbstract(mods)) {
            mods -= Modifier.ABSTRACT;
        }
        if (Modifier.isInterface(mods)) {
            mods -= Modifier.INTERFACE;
        }
        w.write(Modifier.toString(mods) + " class " + token.getSimpleName() + "Impl implements " + token.getCanonicalName() + " {" + System.lineSeparator());
    }

    /**
     * Writes all methods from <tt>methods</tt> to a specified Writer.
     * <p>
     *
     * @param methods an array of methods
     * @param w       Writer to write in
     * @throws IOException if an error has occurred during writing
     */
    private void writeMethods(Method[] methods, Writer w) throws IOException {
        for (Method m : methods) {
            if (m.isDefault()) {
                continue;
            }

            //Annotations
            writeAnnotations(m, w);

            //Signature
            writeMethodSignature(m, w);

            //Method arguments
            writeMethodArguments(m, w);

            //Method exceptions
            writeMethodExceptions(m, w);

            w.write(" {" + System.lineSeparator());

            //Method body
            writeMethodBody(m, w);

            w.write(System.lineSeparator() + tab + "}" + System.lineSeparator());
        }
    }

    /**
     * Writes annotations for a specified method to a Writer.
     * <p>
     *
     * @param m method to write annotations for
     * @param w Writer to write in
     * @throws IOException if an error has occurred during writing
     */
    private void writeAnnotations(Method m, Writer w) throws IOException {
        for (Annotation a : m.getAnnotations()) {
            w.write("@" + a.annotationType().getCanonicalName() + System.lineSeparator());
        }
    }

    /**
     * Writes method signature for a specified method to a Writer.
     * <p>
     *
     * @param m method to write method signature for
     * @param w Writer to write in
     * @throws IOException if an error has occurred during writing
     */
    private void writeMethodSignature(Method m, Writer w) throws IOException {
        int mmods = m.getModifiers();
        if (Modifier.isTransient(mmods)) {
            mmods -= Modifier.TRANSIENT;
        }
        if (Modifier.isAbstract(mmods)) {
            mmods -= Modifier.ABSTRACT;
        }
        w.write(tab + Modifier.toString(mmods) + " " + m.getReturnType().getCanonicalName() + " " + m.getName() + " (");

    }

    /**
     * Writes method arguments for a specified method to a Writer.
     * <p>
     *
     * @param m method to write arguments for
     * @param w Writer to write in
     * @throws IOException if an error has occurred during writing
     */
    private void writeMethodArguments(Method m, Writer w) throws IOException {
        w.write(Arrays.stream(m.getParameters())
                .map(p -> p.getType().getCanonicalName() + " " + p.getName())
                .collect(Collectors.joining(", ")));
        w.write(")");
    }

    /**
     * Writes method exceptions for a specified method to a Writer.
     * <p>
     *
     * @param m method to write exceptions for
     * @param w Writer to write in
     * @throws IOException if an error has occurred during writing
     */
    private void writeMethodExceptions(Method m, Writer w) throws IOException {
        if (m.getExceptionTypes().length != 0) {
            w.write(" throws ");
            w.write(Arrays.stream(m.getExceptionTypes())
                    .map(Class::getCanonicalName)
                    .collect(Collectors.joining(", ")));
        }
    }

    /**
     * Writes method body for a specified method to a Writer.
     * <p>
     *
     * @param m method to write body for
     * @param w Writer to write in
     * @throws IOException if an error has occurred during writing
     */
    private void writeMethodBody(Method m, Writer w) throws IOException {
        if (m.getReturnType() != void.class) {
            w.write(tab + tab + "return ");
            if (m.getReturnType() == boolean.class) {
                w.write("false;");
            } else if (m.getReturnType().isPrimitive()) {
                w.write("0;");
            } else {
                w.write("null;");
            }
        }
    }

    /**
     * Produces <tt>.jar</tt> file implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name is the same as full name of the type token with <tt>Impl</tt> suffix
     * added.
     *
     * @param token   type token to create implementation for
     * @param jarFile target <tt>.jar</tt> file
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException when implementation cannot be generated
     */
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        implement(token, Paths.get(".\\tmp"));
        String tokenPath = token.getPackage().getName().replace('.', '/') + '/' + token.getSimpleName() + "Impl";
        String classToCompile = Paths.get(".\\tmp").resolve(tokenPath).toString();
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
            clearFiles(Paths.get(".\\tmp").toFile());
            throw new ImplerException(e.getMessage());
        }
        clearFiles(Paths.get(".\\tmp").toFile());
    }

    private void clearFiles(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] list = file.listFiles();
            if (list != null) {
                for (File f : list) {
                    clearFiles(f);
                }
            }
        }
        file.delete();
    }

    public static void main (String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)) {
            System.out.println("Error: wrong number of arguments");
            return;
        }
        Implementor imp = new Implementor();
        try {
            if (args.length == 2) {
                imp.implement(Class.forName(args[0]), Paths.get(args[1]));
            }
            if (args.length == 3) {
                imp.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (Exception e) {
            System.out.println("An error occured: " + e.getMessage());
        }
    }
}