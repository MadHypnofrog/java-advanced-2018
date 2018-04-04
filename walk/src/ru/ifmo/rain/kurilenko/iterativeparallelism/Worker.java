package ru.ifmo.rain.kurilenko.iterativeparallelism;

import java.util.List;
import java.util.function.BiFunction;
class Worker<T, R> {
    private List<? extends T> values;
    final Result<R> result;
    private BiFunction<Result<R>, T, Boolean> check; //returns true if the result should be modified with the current element, false if not
    private BiFunction<Result<R>, T, R> func;
    private Thread thread;

    Worker(Result<R> def, List<? extends T> data, BiFunction<Result<R>, T, Boolean> ch, BiFunction<Result<R>, T, R> fun) {
        result = def;
        values = data;
        check = ch;
        func = fun;
        thread = new Thread(() -> {
            for (T t : values) {
                if (check.apply(result, t)) {
                    synchronized (result) {
                        if (check.apply(result, t)) {
                            result.setValue(func.apply(result, t));
                        }
                    }
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
