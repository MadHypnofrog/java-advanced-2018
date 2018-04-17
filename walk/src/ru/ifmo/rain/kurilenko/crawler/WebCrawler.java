package ru.ifmo.rain.kurilenko.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private Downloader downloader;
    private ExecutorService downloading, extracting;
    private int perHost;
    private boolean working = true;
    private final ArrayList<Counter> counters = new ArrayList<>();
    private final ConcurrentHashMap<String, Semaphore> semaphoresPerHost = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        downloading = Executors.newFixedThreadPool(downloaders);
        extracting = Executors.newFixedThreadPool(extractors);
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0 || args.length > 4 || args[0] == null) {
            System.out.println("USAGE: WebCrawler url [downloads [extractors [perHost]]]");
            return;
        }
        String url = args[0];
        int downloaders = 5;
        int extractors = 5;
        int perHost = 5;
        try {
            if (args.length > 1) {
                downloaders = Integer.parseInt(args[1]);
            }
            if (args.length > 2) {
                extractors = Integer.parseInt(args[2]);
            }
            if (args.length > 3) {
                perHost = Integer.parseInt(args[3]);
            }
        } catch (NumberFormatException e) {
            System.err.println(e.getMessage());
            System.out.println("USAGE: WebCrawler url [downloads [extractors [perHost]]]");
            return;
        }
        try (WebCrawler crawler = new WebCrawler(new CachingDownloader(Paths.get(".\\tmp")), downloaders, extractors, perHost)) {
            crawler.download(url, 1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void downloadLinks (String what, int depth, Counter count, List<String> success,
                                ConcurrentHashMap<String, IOException> errors, ConcurrentHashMap<String, Boolean> visited) {
        try {
            if (visited.putIfAbsent(what, Boolean.TRUE) == null) {
                Semaphore s = semaphoresPerHost.get(URLUtils.getHost(what));
                s.acquire();
                try {
                    Document d = downloader.download(what);
                    success.add(what);
                    if (depth > 1) {
                        count.up();
                        extracting.submit(() -> {
                            extractLinks(d, depth, count, success, errors, visited);
                            count.down();
                        });
                    }
                } finally {
                    s.release();
                }
            }
        } catch (IOException e) {
            errors.put(what, e);
        } catch (InterruptedException ignored) {
        }
    }

    private void extractLinks(Document doc, int depth, Counter count, List<String> success,
                                ConcurrentHashMap<String, IOException> errors, ConcurrentHashMap<String, Boolean> visited) {
        try {
            List<String> list = doc.extractLinks();
            for (String s : list) {
                String host = URLUtils.getHost(s);
                semaphoresPerHost.putIfAbsent(host, new Semaphore(perHost));
                count.up();
                downloading.submit(() -> {
                    downloadLinks(s, depth - 1, count, success, errors, visited);
                    count.down();
                });
            }
        } catch (IOException ignored) {
        }
    }

    public Result download(String what, int depth) {
        List<String> success = Collections.synchronizedList(new ArrayList<String>());
        ConcurrentHashMap<String, IOException> errors = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Boolean> visited = new ConcurrentHashMap<>();
        try {
            Counter count = new Counter();
            synchronized (lock) {
                if (!working) {
                    throw new InterruptedException();
                }
                counters.add(count);
                count.up();
                semaphoresPerHost.putIfAbsent(URLUtils.getHost(what), new Semaphore(perHost));
                downloading.submit(() -> {
                    downloadLinks (what, depth, count, success, errors, visited);
                    count.down();
                });
            }
            count.waitCounter();
            counters.remove(count);
        } catch (InterruptedException ignored) {
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return new Result(success, errors);
    }

    public void close() {
        synchronized (lock) {
            downloading.shutdownNow();
            extracting.shutdownNow();
            for (Counter c : counters) {
                c.interrupt();
            }
            working = false;
        }
    }
}
