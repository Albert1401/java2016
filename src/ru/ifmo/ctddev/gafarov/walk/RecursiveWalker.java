package ru.ifmo.ctddev.gafarov.walk;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RecursiveWalker {
    private static class MD5FileVisitor extends SimpleFileVisitor<Path> {
        private static final String errorCode = "00000000000000000000000000000000";
        private MessageDigest digest;
        private byte[] buffer;
        BufferedWriter out;

        public MD5FileVisitor(BufferedWriter out) throws NoSuchAlgorithmException {
            digest = MessageDigest.getInstance("MD5");
            this.out = out;
            buffer = new byte[8096];
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            InputStream inputStream = Files.newInputStream(file);
            int count = 0;
            digest.reset();
            while ((count = inputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, count);
            }
            out.write(DatatypeConverter.printHexBinary(digest.digest()) + " " + file + System.lineSeparator());
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            out.write(errorCode + " " + file + System.lineSeparator());
            return FileVisitResult.CONTINUE;
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: <input file> <output file>");
            return;
        }
        if (args == null || args[0] == null || args[1] == null){
            System.out.println("args cant be null");
        }
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(args[0]), Charset.forName("UTF8"));
             BufferedWriter writer = Files.newBufferedWriter(Paths.get(args[1]))) {
            FileVisitor<Path> visitor = new MD5FileVisitor(writer);
            String s;
            while ((s = reader.readLine()) != null){
                Files.walkFileTree(Paths.get(s), visitor);
            }
        } catch (IOException e) {
            System.out.println("Coulnd't read from input file or write to output" + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("No MD5 algo " + e.getMessage());
        }
    }
}
