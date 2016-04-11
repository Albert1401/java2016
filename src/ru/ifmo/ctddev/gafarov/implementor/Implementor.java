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

public class Implementor implements JarImpler {

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

    private Path compile(Path source) throws ImplerException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null){
            throw new ImplerException("Couldn't find java compiler");
        }
        compiler.run(null, null, null, source.toString());
        Path classFile;
        try {
            classFile = Paths.get(source.toString().replaceAll("\\.java", "\\.class"));
        } catch (InvalidPathException e){
            throw new ImplerException("Compiler error");
        }
        return classFile;
    }

    private class ClassDescriber {
        private Class aClass;
        private static final String  TAB = "    ";
        private static final String SPACE = " ";
        private static final String ARGS_SPACER = ", ";
        private StringBuilder builder = new StringBuilder();

        public ClassDescriber(Class aClass) {
            this.aClass = aClass;
        }

        public String emptyImplemented() throws ImplerException{
            try {
                if (!aClass.getPackage().getName().equals("")) {
                    write("package ", aClass.getPackage().getName(), ";", System.lineSeparator(), System.lineSeparator());
                }
                String simpleName = aClass.getSimpleName() + "Impl";
                write("public class ", simpleName, SPACE);
                write (aClass.isInterface() ? "implements " :  "extends ", aClass.getCanonicalName());
                write(SPACE, "{");

                for (Constructor constructor : aClass.getDeclaredConstructors()){
                    if (!Modifier.isPrivate(constructor.getModifiers())) {
                        printConstructor(constructor, simpleName);
                    }
                }

                for (Method method : getAbstractMethods(aClass)){
                    printMethod(method);
                }
                write(System.lineSeparator(), "}");
            } catch (IOException e){
                e.printStackTrace();
            }
            return builder.toString();
        }

        private List<Method> getAbstractMethods(Class aClass){
            Map<String, Method> map = new HashMap<>();
            getAllHierarchyMethods(map, aClass);
            return map.values().stream().filter(method -> Modifier.isAbstract(method.getModifiers())).collect(Collectors.toList());
        }

        private void getAllHierarchyMethods(Map<String, Method> map, Class aClass){
            if (aClass == null){
                return;
            }
            for (Method method : aClass.getDeclaredMethods()){
                map.putIfAbsent(getHashString(method), method);
            }
            getAllHierarchyMethods(map, aClass.getSuperclass());
            for (Class interf : aClass.getInterfaces()){
                getAllHierarchyMethods(map, interf);
            }
        }

        private String getHashString(Method method){
            return method.getName() + Arrays.toString(method.getParameterTypes());
        }

        private void printConstructor(Constructor constructor, String simpleName) throws IOException {
            write(System.lineSeparator(), System.lineSeparator());
            write(TAB, "public ", simpleName, "(");
            printArgs(constructor.getParameterTypes());
            write(")");
            printExceptions(constructor.getExceptionTypes());
            write("{", System.lineSeparator(), TAB, TAB, "super(");

            int n = constructor.getParameterTypes().length;
            for (int i = 0; i < n; i++){
                write("arg", String.valueOf(i));
                if (i != n - 1){
                    write(ARGS_SPACER);
                }
            }
            write(");", System.lineSeparator(), TAB, "}");
        }

        private void printMethod(Method method) throws IOException {
            write(System.lineSeparator(), System.lineSeparator(), TAB);
            write(Modifier.toString(~Modifier.ABSTRACT & method.getModifiers() & Modifier.methodModifiers()));
            write(SPACE, method.getReturnType().getCanonicalName(), SPACE, method.getName());
            write("(");
            printArgs(method.getParameterTypes());
            write(") {", System.lineSeparator());
            Class retClass = method.getReturnType();
            if (!retClass.equals(void.class)){
                write(TAB, TAB, "return ");
                String retDefault;
                if (retClass.isPrimitive()){
                    retDefault = retClass.equals(boolean.class ) ? "false" : "0";
                } else {
                    retDefault = "null";
                }
                write(retDefault, ";");
            }
            write(System.lineSeparator(), TAB, "}");
        }

        private void printArgs(Class[] args) throws IOException {
            for (int i = 0; i < args.length; i++){
                    write(args[i].getCanonicalName(), SPACE, "arg", String.valueOf(i));
                    if (i != args.length - 1){
                        write(ARGS_SPACER);
                    }
                }
            }

        public void printExceptions(Class[] exceptions) throws IOException {
            if (exceptions.length != 0){
                write(SPACE, "throws ");
            }
            for (int i = 0; i < exceptions.length; i++){
                write(exceptions[i].getCanonicalName());
                if (i != exceptions.length - 1){
                    write(ARGS_SPACER);
                }
            }
            write(SPACE);
        }

        private void write(String... strings) throws IOException {
            for (String string : strings) {
                builder.append(string);
            }
        }
    }

    @Override
    public void implement(Class<?> aClass, Path root) throws ImplerException {
        if (aClass.isPrimitive()){
            throw new ImplerException("Cant inherent from primitive type");
        }
        if (Modifier.isFinal(Modifier.classModifiers() & aClass.getModifiers())){
            throw new ImplerException("Cant inherent from primitive type");
        }
        boolean bConstr = aClass.isInterface();
        for (Constructor constructor : aClass.getDeclaredConstructors()){
            if (!Modifier.isPrivate(constructor.getModifiers())){
                bConstr = true;
            }
        }
        if (!bConstr){
            throw new ImplerException("No constructors");
        }
        Path path;
        try {
            path = Files.createDirectories(root.resolve(Paths.get(aClass.getPackage().getName().replaceAll("\\.", File.separator) + File.separator)));
        } catch (IOException e) {
            throw new ImplerException();
        }
        try(BufferedWriter writer = Files.newBufferedWriter(path.resolve(aClass.getSimpleName() + "Impl.java"), Charset.defaultCharset())) {
            ClassDescriber classDescriber = new ClassDescriber(aClass);
            writer.write(classDescriber.emptyImplemented());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
