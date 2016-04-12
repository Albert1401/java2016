package ru.ifmo.ctddev.gafarov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Provides simple implementations of class inheriting or implementing class or interface.
 */
public class Implementor implements JarImpler {

    /**
     * Creating implementation of given class compressed in JAR.
     * <p/>
     * Creates implementation of class, then tries to compile it.
     * If compilator's exit code was 0 and classfile exist makes JAR file from it with default manifest
     *
     * @param aClass class or interface to implement or inherent
     * @param path   JAR file location
     * @throws ImplerException if inheriting impossible
     *                         javac was not found
     *                         javac exit code != 0
     *                         IOException occurred
     * @see #implement(Class, Path)
     */
    @Override
    public void implementJar(Class<?> aClass, Path path) throws ImplerException {
        implement(aClass, Paths.get("./"));
        Path sourceFile = Paths.get(aClass.getPackage().getName().replaceAll("\\.", File.separator) + File.separator + aClass.getSimpleName() + "Impl.java");
        Path classFile = compile(sourceFile);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(path), manifest)) {
            jarOutputStream.putNextEntry(new ZipEntry(classFile.toString()));
            Files.copy(classFile, jarOutputStream);
            jarOutputStream.closeEntry();
            Files.delete(sourceFile);
            Files.delete(classFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compiles java file.
     * <p/>
     * Resulted classfile lies in the same directory as source java file
     *
     * @param source java file location
     * @return resulting classfile location
     * @throws ImplerException      if javac was not found,
     *                              javac exit code != 0,
     *                              or resulting classfile was not found
     * @throws NullPointerException if {@code source} == null
     */
    private Path compile(Path source) throws ImplerException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Couldn't find java compiler");
        }
        if (compiler.run(null, null, null, source.toString()) != 0) {
            throw new ImplerException("");
        }
        Path classFile;
        try {
            classFile = Paths.get(source.toString().replaceAll("\\.java", "\\.class"));
        } catch (InvalidPathException e) {
            throw new ImplerException("Compiler error");
        }
        return classFile;
    }

    /**
     * Class designed to creating simple implementations
     */
    private class ClassDescriber {
        private Class aClass;
        private static final String TAB = "    ";
        private static final String SPACE = " ";
        private static final String ARGS_SPACER = ", ";
        private StringBuilder builder = new StringBuilder();

        /**
         * Creates an instace.
         *
         * @param aClass class or interface token to extend or implement
         */
        public ClassDescriber(Class aClass) {
            this.aClass = aClass;
        }


        /**
         * Returns implementation.
         *
         * @return String simple implementation
         * @throws ImplerException if inheriting impossible
         */
        public String emptyImplemented() throws ImplerException {
            if (!aClass.getPackage().getName().equals("")) {
                write("package ", aClass.getPackage().getName(), ";", System.lineSeparator(), System.lineSeparator());
            }
            String simpleName = aClass.getSimpleName() + "Impl";
            write("public class ", simpleName, SPACE);
            write(aClass.isInterface() ? "implements " : "extends ", aClass.getCanonicalName());
            write(SPACE, "{");

            for (Constructor constructor : aClass.getDeclaredConstructors()) {
                if (!Modifier.isPrivate(constructor.getModifiers())) {
                    printConstructor(constructor, simpleName);
                }
            }

            for (Method method : getAbstractMethods(aClass)) {
                printMethod(method);
            }
            write(System.lineSeparator(), "}");
            return builder.toString();
        }

        /**
         * Returns list of methods that must be implemented.
         * <p/>
         * Returns only abstract methods of superclasses
         *
         * @param aClass {@code Class} to get methods from
         * @return {@code List} of not implemented methods
         * @see #getAllHierarchyMethods(Map, Class)
         */
        private List<Method> getAbstractMethods(Class aClass) {
            Map<String, Method> map = new HashMap<>();
            getAllHierarchyMethods(map, aClass);
            return map.values().stream().filter(method -> Modifier.isAbstract(method.getModifiers())).collect(Collectors.toList());
        }

        /**
         * Puts methods of superclass and interfaces into the map.
         * <p/>
         * Recursive method to look through superclasses and interfaces of {@code Class aClass}.
         * Uses #getHashString(Method) to keep from adding already defined method.
         *
         * @param map    Map to put elements
         * @param aClass Class to walk hierarchy through
         * @see #getHashString(Method)
         */
        private void getAllHierarchyMethods(Map<String, Method> map, Class aClass) {
            if (aClass == null) {
                return;
            }
            for (Method method : aClass.getDeclaredMethods()) {
                map.putIfAbsent(getHashString(method), method);
            }
            getAllHierarchyMethods(map, aClass.getSuperclass());
            for (Class interf : aClass.getInterfaces()) {
                getAllHierarchyMethods(map, interf);
            }
        }

        /**
         * Decries method by string.
         *
         * @param method Method to describe
         * @return method''s String representation
         */
        private String getHashString(Method method) {
            return method.getName() + Arrays.toString(method.getParameterTypes());
        }

        /**
         * Appends constructor's implementation to #builder.
         *
         * @param constructor constructor to implement
         * @param simpleName  SimpleName of class's constructor
         */
        private void printConstructor(Constructor constructor, String simpleName) {
            write(System.lineSeparator(), System.lineSeparator());
            write(TAB, "public ", simpleName, "(");
            printArgs(constructor.getParameterTypes());
            write(")");
            printExceptions(constructor.getExceptionTypes());
            write("{", System.lineSeparator(), TAB, TAB, "super(");

            int n = constructor.getParameterTypes().length;
            for (int i = 0; i < n; i++) {
                write("arg", String.valueOf(i));
                if (i != n - 1) {
                    write(ARGS_SPACER);
                }
            }
            write(");", System.lineSeparator(), TAB, "}");
        }

        /**
         * Appends method's implementation to #builder
         *
         * @param method to implement
         */
        private void printMethod(Method method) {
            write(System.lineSeparator(), System.lineSeparator(), TAB);
            write(Modifier.toString(~Modifier.ABSTRACT & method.getModifiers() & Modifier.methodModifiers()));
            write(SPACE, method.getReturnType().getCanonicalName(), SPACE, method.getName());
            write("(");
            printArgs(method.getParameterTypes());
            write(") {", System.lineSeparator());
            Class retClass = method.getReturnType();
            if (!retClass.equals(void.class)) {
                write(TAB, TAB, "return ");
                String retDefault;
                if (retClass.isPrimitive()) {
                    retDefault = retClass.equals(boolean.class) ? "false" : "0";
                } else {
                    retDefault = "null";
                }
                write(retDefault, ";");
            }
            write(System.lineSeparator(), TAB, "}");
        }

        /**
         * Appends method or constructor arguments
         * @param args to implement
         */
        private void printArgs(Class[] args) {
            for (int i = 0; i < args.length; i++) {
                write(args[i].getCanonicalName(), SPACE, "arg", String.valueOf(i));
                if (i != args.length - 1) {
                    write(ARGS_SPACER);
                }
            }
        }

        /**
         *
         * @param exceptions
         */
        public void printExceptions(Class[] exceptions) {
            if (exceptions.length != 0) {
                write(SPACE, "throws ");
            }
            for (int i = 0; i < exceptions.length; i++) {
                write(exceptions[i].getCanonicalName());
                if (i != exceptions.length - 1) {
                    write(ARGS_SPACER);
                }
            }
            write(SPACE);
        }

        private void write(String... strings) {
            for (String string : strings) {
                builder.append(string);
            }
        }
    }

    /**
     * @param aClass
     * @param root
     * @throws ImplerException
     */
    @Override
    public void implement(Class<?> aClass, Path root) throws ImplerException {
        if (aClass.isPrimitive()) {
            throw new ImplerException("Cant inherent from primitive type");
        }
        if (Modifier.isFinal(Modifier.classModifiers() & aClass.getModifiers())) {
            throw new ImplerException("Cant inherent from primitive type");
        }
        boolean bConstr = aClass.isInterface();
        for (Constructor constructor : aClass.getDeclaredConstructors()) {
            if (!Modifier.isPrivate(constructor.getModifiers())) {
                bConstr = true;
            }
        }
        if (!bConstr) {
            throw new ImplerException("No constructors");
        }
        Path path;
        try {
            path = Files.createDirectories(root.resolve(Paths.get(aClass.getPackage().getName().replaceAll("\\.", File.separator) + File.separator)));
        } catch (IOException e) {
            throw new ImplerException();
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path.resolve(aClass.getSimpleName() + "Impl.java"), Charset.defaultCharset())) {
            ClassDescriber classDescriber = new ClassDescriber(aClass);
            writer.write(classDescriber.emptyImplemented());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
