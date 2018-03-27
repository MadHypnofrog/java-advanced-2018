package ru.ifmo.rain.kurilenko.iterativeparallelism;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

class Worker<T, R> {
    private List<? extends T> values;
    final Result<R> result;
    private BiFunction<Result<R>, T, Integer> check; //return 1 if the result should be modified with the current element, 0 if not, -1 if it's possible to stop the thread
    private BiFunction<Result<R>, T, R> func;
    private Thread thread;

    Worker(Result<R> def, List<? extends T> data, BiFunction<Result<R>, T, Integer> ch, BiFunction<Result<R>, T, R> fun) {
        result = def;
        values = data;
        check = ch;
        func = fun;
        thread = new Thread(() -> {
            for (T t : values) {
                int res = check.apply(result, t);
                if (res == 1) {
                    synchronized (result) {
                        if (check.apply(result, t) == 1) {
                            result.setValue(func.apply(result, t));
                        }
                    }
                } else if (res == -1) {
                    return;
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
