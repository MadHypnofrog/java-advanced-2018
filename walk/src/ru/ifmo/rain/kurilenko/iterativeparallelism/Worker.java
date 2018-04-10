package ru.ifmo.rain.kurilenko.iterativeparallelism;

import java.util.List;
import java.util.function.BiFunction;

class Worker<T, R> {
    final Result<R> result;
    private Thread thread;

    Worker(Result<R> def, List<? extends T> data, BiFunction<Result<R>, T, Boolean> ch, BiFunction<Result<R>, T, R> fun) {
        result = def;

        thread = new Thread(() -> {
            for (T t : data) {
                if (ch.apply(result, t)) {
                    result.setValue(fun.apply(result, t));
                }
            }
        });
    }

    void start() {
        thread.start();
    }

    void join() throws InterruptedException {
        thread.join();
    }

}
