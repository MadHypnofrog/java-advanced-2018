package ru.ifmo.rain.kurilenko.walk;

import java.io.*;
import java.nio.file.NoSuchFileException;

public class Walk {
    public static void main(String[] args) {
        try (BufferedReader in = new BufferedReader(new FileReader (new File(args[0]))); BufferedWriter w = new BufferedWriter (new FileWriter(new File(args[1])))) {
            String path, hash;
            while ((path = in.readLine()) != null) {
                try (FileInputStream r = new FileInputStream(new File(path))) {
                    int x = 0x811c9dc5, p = 0x01000193, i;
                    byte [] buf = new byte [1024];
                    while ((i = r.read(buf)) >= 0) {
                        for (int tmp = 0; tmp < i; tmp++) x = (x * p) ^ (buf[tmp] & 0xff);
                    }
                    hash = String.format("%08x", x);
                } catch (FileNotFoundException e) {
                    hash = "00000000";
                    System.err.println ("Error: file " + path + " not found");
                }
                catch (IOException e) {
                    hash = "00000000";
                    System.err.println ("Error: can't read from " + path);
                }
                try {
                    w.write (hash + " " + path + System.lineSeparator());
                } catch (IOException e) {
                    System.err.println ("Error: output error");
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println ("Error: input file " + args[0] + " not found");
        } catch (IOException e) {
            System.err.println ("Error: can't read from input file " + args[0]);
        }
    }
}
