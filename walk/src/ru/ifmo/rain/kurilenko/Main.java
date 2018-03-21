package ru.ifmo.rain.kurilenko;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class Main {
    public static void main (String[] args) throws Exception {
        JarOutputStream jos = new JarOutputStream(new FileOutputStream("test.jar"));
        FileInputStream in = new FileInputStream("script.txt");
        jos.putNextEntry(new ZipEntry("asd\\script.txt"));
        byte[] buffer = new byte[1024];
        int len;
        while((len = in.read(buffer)) > 0) {
            jos.write(buffer, 0, len);
        }
        jos.closeEntry();
        jos.close();
    }
}
