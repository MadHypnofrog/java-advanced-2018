package ru.ifmo.rain.kurilenko.walk;

import java.io.*;

public class Walk {
    public static void main(String[] args) {
        try (BufferedReader in = new BufferedReader(new FileReader (new File(args[0]))); BufferedWriter w = new BufferedWriter (new FileWriter(new File(args[1])))) {
            String path;
            while ((path = in.readLine()) != null) {
                try (FileInputStream r = new FileInputStream(new File(path))) {
                    int x = 0x811c9dc5, p = 0x01000193, i;
                    byte [] buf = new byte [1024];
                    while ((i = r.read(buf)) >= 0) {
                        for (int tmp = 0; tmp < i; tmp++) x = (x * p) ^ (buf[tmp] & 0xff);
                    }
                    w.write (String.format("%08x", x) + " " + path + System.lineSeparator());
                } catch (FileNotFoundException e) {
                    w.write ("00000000 " + path + System.lineSeparator());
                    System.err.println ("Error: file " + path + " not found");
                }
                catch (IOException e) {
                    w.write ("00000000 " + path + System.lineSeparator());
                    System.err.println ("Error: can't read from " + path);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println ("Error: file " + args[0] + " not found");
        } catch (IOException e) {
            System.err.println ("Error: can't read from " + args[0]);
        }
    }
}
