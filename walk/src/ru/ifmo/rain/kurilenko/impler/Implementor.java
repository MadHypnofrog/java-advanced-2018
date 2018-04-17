package ru.ifmo.rain.kurilenko.impler;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Implementation class for {@link JarImpler} interface.
 */
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
        if (token.isPrimitive() || token.isArray() || Modifier.isFinal(token.getModifiers()) || token == Enum.class) {
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


            //Constructors
            if (!token.isInterface()) {
                writeConstructors(token, w);
            }

            //Methods
            HashSet<MethodWrapper> methods = new HashSet<>();
            methods = getMethods(methods, token.getMethods());
            while (token != null) {
                methods = getMethods(methods, token.getDeclaredMethods());
                token = token.getSuperclass();
            }
            writeMethods(methods, w);

            w.write("}");
        } catch (Exception e) {
            throw new ImplerException(e.getMessage());
        }
    }

    /**
     * Adds {@link Method}s to the given {@link HashSet} that are not present yet and returns a resulting {@link HashSet}.
     * <p>
     *
     * @param methods a {@link HashSet} of current {@link Method}s
     * @param toAdd   an array of {@link Method}s to add
     * @return a {@link HashSet} containing all the methods from <code>methods</code> and <code>toAdd</code>, excluding duplicates
     */
    private HashSet<MethodWrapper> getMethods(HashSet<MethodWrapper> methods, Method[] toAdd) {
        return Arrays.stream(toAdd)
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(MethodWrapper::new)
                .collect(Collectors.toCollection(() -> methods));
    }

    /**
     * Writes a header for a specified {@link Class} to a specified {@link Writer}.
     * <p>
     *
     * @param token type token to write a header for
     * @param w     {@link Writer} to write in
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
        w.write(Modifier.toString(mods) + " class " + token.getSimpleName() + "Impl " +
                (token.isInterface() ? "implements " : "extends ") + token.getCanonicalName() + " {" + System.lineSeparator());
    }

    /**
     * Writes all non-private constructors for a specified {@link Class} to a specified {@link Writer}.
     * <p>
     *
     * @param token type token to write constructors for
     * @param w     {@link Writer} to write in
     * @throws IOException     if an error has occurred during writing
     * @throws ImplerException if the class has no non-private constructors and thus cannot be extended
     */
    private void writeConstructors(Class<?> token, Writer w) throws ImplerException, IOException {
        Constructor<?>[] constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .toArray(Constructor[]::new);
        if (constructors.length == 0) {
            throw new ImplerException("Error: class " + token.getSimpleName() + " has no non-private constructors");
        }
        for (Constructor<?> constructor : constructors) {
            w.write(Modifier.toString(constructor.getModifiers() & ~Modifier.TRANSIENT) + " " + token.getSimpleName() + "Impl (");
            writeMethodArguments(constructor, w, true);
            writeMethodExceptions(constructor, w);
            w.write(" {" + System.lineSeparator() + tab);
            writeMethodBody(constructor, w);
            w.write(System.lineSeparator() + "}" + System.lineSeparator());
        }
    }


    /**
     * Writes all methods for a specified {@link Class} to a specified {@link Writer}.
     * <p>
     *
     * @param methods a {@link HashSet} of unique {@link Method}s to write
     * @param w       {@link Writer} to write in
     * @throws IOException if an error has occurred during writing
     */
    private void writeMethods(HashSet<MethodWrapper> methods, Writer w) throws IOException {
        for (MethodWrapper mm : methods) {
            Method m = mm.getMethod();
            if (m.isDefault()) {
                continue;
            }

            //Annotations
            writeAnnotations(m, w);

            //Signature
            writeMethodSignature(m, w);

            //Method arguments
            writeMethodArguments(m, w, true);

            //Method exceptions
            writeMethodExceptions(m, w);

            w.write(" {" + System.lineSeparator());

            //Method body
            writeMethodBody(m, w);

            w.write(System.lineSeparator() + tab + "}" + System.lineSeparator());
        }
    }

    /**
     * Writes all annotations for a specified {@link Executable} to a specified {@link Writer}.
     * <p>
     *
     * @param e {@link Executable} to write annotations for
     * @param w {@link Writer} to write in
     * @throws IOException if an error has occurred during writing
     */
    private void writeAnnotations(Executable e, Writer w) throws IOException {
        for (Annotation a : e.getAnnotations()) {
            w.write("@" + a.annotationType().getCanonicalName() + System.lineSeparator());
        }
    }

    /**
     * Writes a signature for a specified {@link Executable} to a specified {@link Writer}.
     * <p>
     *
     * @param e {@link Executable} to write signature for
     * @param w {@link Writer} to write in
     * @throws IOException if an error has occurred during writing
     */
    private void writeMethodSignature(Executable e, Writer w) throws IOException {
        int mmods = e.getModifiers() & ~Modifier.TRANSIENT & ~Modifier.ABSTRACT;
        String ret = "";
        if (e instanceof Method) {
            ret = ((Method) e).getReturnType().getCanonicalName();
        }
        w.write(tab + Modifier.toString(mmods) + " " + ret + " " + e.getName() + " (");

    }

    /**
     * Writes arguments for a specified {@link Executable} to a specified {@link Writer}.
     * <p>
     *
     * @param e    {@link Executable} to write arguments for
     * @param w    {@link Writer} to write in
     * @param type a {@link Boolean} that describes if a type signature is needed for variables
     * @throws IOException if an error has occurred during writing
     */
    private void writeMethodArguments(Executable e, Writer w, boolean type) throws IOException {
        w.write(Arrays.stream(e.getParameters())
                .map(p -> (type ? (p.getType().getCanonicalName() + " ") : "") + p.getName())
                .collect(Collectors.joining(", ")));
        w.write(")");
    }

    /**
     * Writes all exceptions for a specified {@link Executable} to a specified {@link Writer}.
     * <p>
     *
     * @param e {@link Executable} to write exceptions for
     * @param w {@link Writer} to write in
     * @throws IOException if an error has occurred during writing
     */
    private void writeMethodExceptions(Executable e, Writer w) throws IOException {
        if (e.getExceptionTypes().length != 0) {
            w.write(" throws ");
            w.write(Arrays.stream(e.getExceptionTypes())
                    .map(Class::getCanonicalName)
                    .collect(Collectors.joining(", ")));
        }
    }

    /**
     * Writes the body of a specified {@link Executable} to a specified {@link Writer}.
     * <p>
     *
     * @param e {@link Executable} to write the body for
     * @param w {@link Writer} to write in
     * @throws IOException if an error has occurred during writing
     */
    private void writeMethodBody(Executable e, Writer w) throws IOException {
        if (e instanceof Method) {
            Method mm = (Method) e;
            if (mm.getReturnType() != void.class) {
                w.write(tab + tab + "return ");
                if (mm.getReturnType() == boolean.class) {
                    w.write("false;");
                } else if (mm.getReturnType().isPrimitive()) {
                    w.write("0;");
                } else {
                    w.write("null;");
                }
            }
        } else {
            w.write("super (");
            writeMethodArguments(e, w, false);
            w.write(";");
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


    /**
     * Recursively deletes all files, starting from the given {@link File}.
     *
     * @param file {@link File} to begin deleting from
     */
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


    /**
     * Takes 2 or 3 arguments and either creates an implementation of a specified class or a <tt>.jar</tt> file containing this implementation.
     * For 2 arguments, the first argument should be a class token and the second one is a root directory.
     * For 3 arguments, the first one should be "-jar", second one is a class token and third one is a path to the desired <tt>.jar</tt> file.
     *
     * @param args an array of {@link String} with arguments
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)) {
            System.out.println("Error: wrong number of arguments, should be 2 or 3");
            return;
        }
        Implementor imp = new Implementor();
        try {
            if (args.length == 2) {
                imp.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                if (args[0].equals("-jar")) {
                    imp.implementJar(Class.forName(args[1]), Paths.get(args[2]));
                } else {
                    System.out.println("Error: first argument should be -jar when there are 3 arguments");
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found: " + e.getMessage());
        } catch (ImplerException e) {
            System.out.println("An error occured while implementing: " + e.getMessage());
        }
    }
}