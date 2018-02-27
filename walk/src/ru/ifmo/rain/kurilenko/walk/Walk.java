package ru.ifmo.rain.kurilenko.walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Walk {

    public static void main(String[] args) {
        if (args == null) {
            System.err.println("Error: args is null");
        } else if (args.length != 2) {
            System.err.println("Error: wrong number of arguments");
        } else if (args[0] == null) {
            System.err.println ("Error: input file is null");
        } else if (args[1] == null) {
            System.err.println ("Error: output file is null");
        } else try (BufferedReader in = Files.newBufferedReader(Paths.get(args[0]))) {
            try (BufferedWriter w = Files.newBufferedWriter(Paths.get(args[1]))) {
                String path, hash;
                while ((path = in.readLine()) != null) {
                    hash = "00000000";
                    try (InputStream r = Files.newInputStream(Paths.get(path))) {
                        int x = 0x811c9dc5, p = 0x01000193, i;
                        byte[] buf = new byte[1024];
                        while ((i = r.read(buf)) >= 0) {
                            for (int tmp = 0; tmp < i; tmp++) x = (x * p) ^ (buf[tmp] & 0xff);
                        }
                        hash = String.format("%08x", x);
                    } catch (InvalidPathException e) {
                        System.err.println("Error: path " + path + " not found");
                    } catch (IOException e) {
                        System.err.println("Error: I/O exception in file " + path);
                    } catch (SecurityException e) {
                        System.err.println("Error: security violation in file " + path);
                    }
                    try {
                        w.write(hash + " " + path + System.lineSeparator());
                    } catch (IOException e) {
                        System.err.println("Error: output error");
                    }
                }
            } catch (InvalidPathException e) {
                System.err.println("Error: output path " + args[1] + " is invalid");
            } catch (IOException e) {
                System.err.println("Error: I/O exception in output file"  + args[1]);
            } catch (SecurityException e) {
                System.err.println("Error: security violation on output file " + args[1]);
            }
        } catch (InvalidPathException e) {
            System.err.println("Error: input path " + args[0] + " is invalid");
        } catch (IOException e) {
            System.err.println("Error: I/O exception in input file"  + args[0]);
        } catch (SecurityException e) {
            System.err.println("Error: security violation on input file " + args[0]);
        }
    }
}
