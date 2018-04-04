package ru.ifmo.rain.kurilenko.iterativeparallelism;

import java.util.function.Function;

public class MapperTask<T, R> {
    private Function<? super T, ? extends R> f;
    private T t;
    private boolean finished = false, err = false;
    private R res;
    MapperTask (Function<? super T, ? extends R> fun, T obj) {
        t = obj;
        f = fun;
    }

    private synchronized void waitTask() throws InterruptedException {
        while (!finished) {
            try {
                wait();
            } catch (InterruptedException e) {
                if (err) {
                    throw e;
                }
            }
        }
    }

    synchronized void interrupt() {
        err = true;
        notifyAll();
    }

    synchronized void done() {
        finished = true;
        notifyAll();
    }

    void apply() {
        res = f.apply(t);
        done();
    }

    public R getResult() throws InterruptedException {
        waitTask();
        return res;
    }
}
