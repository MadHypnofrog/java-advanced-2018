package ru.ifmo.rain.kurilenko.iterativeparallelism;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private final LinkedList<MapperTask> queue = new LinkedList<>();
    private ArrayList<Thread> threads = new ArrayList<>();
    private boolean isRunning = true;

    public ParallelMapperImpl() {}

    public ParallelMapperImpl (int t) {
        for (int i = 0; i < t; i++) {
            threads.add (new Thread(() -> {
                while (isRunning) {
                    MapperTask task;
                    synchronized (queue) {
                        while (isRunning && queue.isEmpty()) {
                            try {
                                queue.wait();
                            } catch (InterruptedException ignored) {}
                        }
                        if (!isRunning) {
                            return;
                        }
                        task = queue.removeFirst();
                    }
                    task.apply();
                }
            }));
        }
        for (Thread th: threads) {
            th.start();
        }
    }

    private void addTask(MapperTask t) {
        synchronized (queue) {
            if (!isRunning) {
                return;
            }
            queue.add(t);
            queue.notify();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        ArrayList<R> res = new ArrayList<>();
        ArrayList<MapperTask<T, R>> tasks = new ArrayList<>();
        for (T t: args) {
            MapperTask<T, R> task = new MapperTask<>(f, t);
            tasks.add (task);
            addTask (task);
        }
        for (MapperTask<T, R> t: tasks) {
            res.add(t.getResult());
        }
        return res;
    }

    @Override
    public void close() {
        synchronized (queue) {
            isRunning = false;
            queue.notifyAll();
        }
        for (MapperTask t : queue) {
            t.interrupt();
        }
        for (Thread t : threads) {
            t.interrupt();
            try {
                t.join();
            } catch (InterruptedException ignored) {}
        }
    }
}
