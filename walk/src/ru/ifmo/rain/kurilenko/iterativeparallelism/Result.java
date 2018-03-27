package ru.ifmo.rain.kurilenko.iterativeparallelism;


import java.util.function.Consumer;

class Result<R> {
    private R value;
    Result (R val) {
        value = val;
    }
    Result (Result<R> another) {
        value = another.value;
    }
    void setValue(R r) {
        value = r;
    }
    R getValue() {
        return value;
    }
}
