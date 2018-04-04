package ru.ifmo.rain.kurilenko;

import ru.ifmo.rain.kurilenko.iterativeparallelism.IterativeParallelism;
import ru.ifmo.rain.kurilenko.iterativeparallelism.ParallelMapperImpl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class Main {
    public static void main (String[] args) throws Exception {
        IterativeParallelism it = new IterativeParallelism(new ParallelMapperImpl(10));
        List<String> l = new ArrayList<>();
        l.add(":0");
        l.add(" 1444");
        l.add ("THIS IS JUST A DREAM");
        l.add (" a ");
        l.add( " rofleksey   ");
        l.add ("ROFL");
        l.add ("WUT");
        System.out.println(it.join (2, l));
        //System.out.println(it.map(2, l, Object::toString));
    }
}
