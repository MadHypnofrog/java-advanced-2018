package ru.ifmo.rain.kurilenko.iterativeparallelism;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class IterativeParallelism implements ScalarIP {

    private <T> ArrayList<List<? extends T>> split(int numThreads, List<? extends T> values) {
        ArrayList<List<? extends T>> res = new ArrayList<>();
        float num = values.size() / numThreads;
        for (int i = 0; i < numThreads - 1; i++) {
            res.add(values.subList((int)(i * num), (int)((i + 1) * num)));
        }
        res.add(values.subList((int)((numThreads - 1) * num), values.size()));
        return res;
    }

    private <T, R> ArrayList<Worker<T, R>> createWorkers (ArrayList<List<? extends T>> list, BiFunction<Result<R>, T, Integer> ch,
                                                          BiFunction<Result<R>, T, R> fun, Result<R> res, boolean common) {
        ArrayList<Worker<T, R>> workers = new ArrayList<>();
        for (List<? extends T> l: list) {
            workers.add(new Worker<>(common ? res : new Result<>(res), l, ch, fun));
        }
        for (Worker w: workers) {
            w.start();
        }
        return workers;
    }

    private <T, R> R foldResult (ArrayList<Worker<T, R>> workers, BiFunction<R, R, R> fun, R def) throws InterruptedException {
        for (Worker w: workers) {
            w.join();
        }
        R res = def;
        boolean d = false;
        for (Worker<T, R> w: workers) {
            if (!d) {
                res = w.result.getValue();
                d = true;
            } else {
                res = fun.apply(res, w.result.getValue());
            }
        }
        return res;
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        threads = Math.min (values.size(), threads);
        ArrayList<List<? extends T>> lists = split (threads, values);
        ArrayList<Worker<T, T>> workers = createWorkers(lists, (res, t) -> Math.max(0, comparator.compare(t, res.getValue())), (res, t) -> t,
                new Result<T> (values.get(0)), false);
        return foldResult(workers, (t1, t2) -> comparator.compare(t1, t2) < 0 ? t2 : t1, null);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        threads = Math.min (values.size(), threads);
        ArrayList<List<? extends T>> lists = split (threads, values);
        ArrayList<Worker<T, T>> workers = createWorkers(lists, (res, t) -> Math.max(0, comparator.compare(res.getValue(), t)), (res, t) -> t,
                new Result<T> (values.get(0)), false);
        return foldResult(workers, (t1, t2) -> comparator.compare(t1, t2) > 0 ? t2 : t1, null);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        threads = Math.min (values.size(), threads);
        ArrayList<List<? extends T>> lists = split (threads, values);
        ArrayList<Worker<T, Boolean>> workers = createWorkers(lists, (Result<Boolean> res, T t) -> {
                    if (!res.getValue()) {
                        return -1;
                    }
                    if (predicate.test(t)) {
                        return 0;
                    } else {
                        return 1;
                    }
                }, (res, t) -> res.getValue() && predicate.test(t),
                new Result<> (Boolean.TRUE), true);
        return foldResult(workers, (t1, t2) -> t1 && t2, true);
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        threads = Math.min (values.size(), threads);
        ArrayList<List<? extends T>> lists = split (threads, values);
        ArrayList<Worker<T, Boolean>> workers = createWorkers(lists, (Result<Boolean> res, T t) -> {
                    if (res.getValue()) {
                        return -1;
                    }
                    if (predicate.test(t)) {
                        return 1;
                    } else {
                        return 0;
                    }
                }, (res, t) -> res.getValue() || predicate.test(t),
                new Result<> (Boolean.FALSE), true);
        return foldResult(workers, (t1, t2) -> t1 || t2, false);
    }
}
