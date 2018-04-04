package ru.ifmo.rain.kurilenko.iterativeparallelism;


import java.util.function.Consumer;

class Result<R> {
    private R value;
    Result (R val) {
        value = val;
    }
    void setValue(R r) {
        value = r;
    }
    R getValue() {
        return value;
    }
}
