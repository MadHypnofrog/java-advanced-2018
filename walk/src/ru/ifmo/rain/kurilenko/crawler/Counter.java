package ru.ifmo.rain.kurilenko.crawler;

public class Counter {
    private int count = 0;
    private boolean working = true;

    public synchronized void up () {
        count++;
    }
    public synchronized void down () {
        if (--count <= 0) {
            notify();
        }
    }

    public int getCount () {
        return count;
    }

    public synchronized void waitCounter () throws InterruptedException {
        while (working && count > 0) {
            wait();
        }
    }

    public synchronized void interrupt () {
        working = false;
        notify();
    }
}
