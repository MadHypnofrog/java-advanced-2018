package ru.ifmo.rain.kurilenko.iterativeparallelism;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class IterativeParallelism implements ListIP {

    public ParallelMapper map = null;

    public IterativeParallelism() {
    }

    public IterativeParallelism(ParallelMapper mapper) {
        map = mapper;
    }

    private <T> ArrayList<List<? extends T>> split(int numThreads, List<? extends T> values) {
        numThreads = Math.min(values.size(), numThreads);
        ArrayList<List<? extends T>> res = new ArrayList<>();
        float num = values.size() / numThreads;
        for (int i = 0; i < numThreads - 1; i++) {
            res.add(values.subList((int) (i * num), (int) ((i + 1) * num)));
        }
        res.add(values.subList((int) ((numThreads - 1) * num), values.size()));
        return res;
    }

    private <T, R> ArrayList<Worker<T, R>> createWorkers(ArrayList<List<? extends T>> list,
                                                         BiFunction<Result<R>, T, Boolean> check,
                                                         BiFunction<Result<R>, T, R> fun,
                                                         Supplier<Result<R>> def) {
        ArrayList<Worker<T, R>> workers = new ArrayList<>();
        for (List<? extends T> l : list) {
            workers.add(new Worker<>(def.get(), l, check, fun));
        }
        for (Worker w : workers) {
            w.start();
        }
        return workers;
    }

    private <T, R> List<R> getResults(ArrayList<List<? extends T>> list,
                                      BiFunction<Result<R>, T, Boolean> check,
                                      BiFunction<Result<R>, T, R> fun,
                                      Supplier<Result<R>> def) throws InterruptedException {
        if (map == null) {
            ArrayList<Worker<T, R>> workers = createWorkers(list, check, fun, def);
            ArrayList<R> results = new ArrayList<>();
            for (Worker<T, R> w : workers) {
                w.join();
                results.add(w.result.getValue());
            }
            return results;
        } else {
            return map.map(t -> {
                Result<R> res = def.get();
                for (T elem : t) {
                    if (check.apply(res, elem)) {
                        res.setValue(fun.apply(res, elem));
                    }
                }
                return res.getValue();
            }, list);
        }
    }

    private <R> R foldResult(List<R> results, BiFunction<R, R, R> fun, R def) throws InterruptedException {
        R res = def;
        boolean d = false;
        for (R r : results) {
            if (!d) {
                res = r;
                d = true;
            } else {
                res = fun.apply(res, r);
            }
        }
        return res;
    }

    private <T> T minMax(int threads,
                         List<? extends T> values,
                         Comparator<? super T> comp,
                         Predicate<Integer> check) throws InterruptedException {
        ArrayList<List<? extends T>> lists = split(threads, values);
        return foldResult(getResults(lists,
                                    (Result<T> res, T t) -> check.test(comp.compare(t, res.getValue())),
                                    (res, t) -> t,
                                    () -> new Result<>(values.get(0))),
                         (t1, t2) -> check.test(comp.compare(t1, t2)) ? t1 : t2,
                        null);
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return minMax(threads, values, comparator, a -> a > 0);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return minMax(threads, values, comparator, a -> a < 0);
    }

    private <T> boolean allAny(int threads,
                               List<? extends T> values,
                               Predicate<? super T> predicate,
                               Predicate<Boolean> check,
                               Boolean def,
                               BiFunction<Boolean, Boolean, Boolean> fold) throws InterruptedException {
        ArrayList<List<? extends T>> lists = split(threads, values);
        return foldResult(getResults(lists,
                                    (Result<Boolean> res, T t) -> check.test(predicate.test(t)),
                                    (res, t) -> !def,
                                    () -> new Result<>(def)),
                          fold, def);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return allAny(threads, values, predicate, a -> !a, true, (t1, t2) -> t1 && t2);
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return allAny(threads, values, predicate, a -> a, false, (t1, t2) -> t1 || t2);
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        ArrayList<List<?>> lists = split(threads, values);
        return foldResult(getResults(lists,
                                    (res, t) -> true,
                                    (Result<StringBuilder> res, Object t) -> {
                                        res.getValue().append(t.toString());
                                        return res.getValue();
                                    },
                                    () -> new Result<>(new StringBuilder())),
                          (t1, t2) -> t1.append(t2.toString()),
                           new StringBuilder("")).toString();
    }

    private <T, U> List<U> filterMap(int threads,
                                     List<? extends T> values,
                                     Predicate<? super T> predicate,
                                     Function<? super T, ? extends U> f) throws InterruptedException {
        ArrayList<List<? extends T>> lists = split(threads, values);
        return foldResult(getResults(lists,
                                    (res, t) -> predicate.test(t), (res, t) -> {
                                        res.getValue().add(f.apply(t));
                                        return res.getValue();
                                    },
                                    () -> new Result<>(new ArrayList<U>())),
                         (t1, t2) -> {
                              t1.addAll(t2);
                              return t1;
                         }, new ArrayList<>());
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return filterMap(threads, values, predicate, a -> a);
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return filterMap(threads, values, a -> true, f);
    }
}
