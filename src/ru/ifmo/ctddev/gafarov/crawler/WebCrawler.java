package ru.ifmo.ctddev.gafarov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.stream.Collectors;

public class WebCrawler implements Crawler {
    private Downloader downloader;
    ExecutorService downServ;
    ExecutorService extrServ;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        downServ = Executors.newFixedThreadPool(downloaders);
        extrServ = Executors.newFixedThreadPool(extractors);
    }

    @Override
    public Result download(String url, int depth) {
        Phaser phaser = new Phaser(1);
        Set<String> regdDown = ConcurrentHashMap.newKeySet();
        Set<String> regdExtr = ConcurrentHashMap.newKeySet();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        down(phaser, url, depth, regdDown, regdExtr, errors);

        phaser.arriveAndAwaitAdvance();
        regdDown.removeAll(errors.keySet());
        return new Result(regdDown.stream().collect(Collectors.toList()), errors);
    }

    private void down(Phaser phaser,
                      String url, int depth,
                      Set<String> regdDown, Set<String> regdExtr,
                      Map<String, IOException> errors) {
        if (depth > 0 && regdDown.add(url)) {
            phaser.register();
            downServ.submit(() -> {
                try {
                    Document doc = downloader.download(url);
                    extr(phaser, doc, url, depth, regdDown, regdExtr, errors);
                } catch (IOException e) {
                    errors.put(url, e);
                } finally {
                    phaser.arrive();
                }
            });
        }
    }

    private void extr(Phaser phaser,
                      Document doc, String url, int depth,
                      Set<String> regdDown, Set<String> regdExtr,
                      Map<String, IOException> errors) {
        if (depth > 0 && regdExtr.add(url)) {
            phaser.register();
            extrServ.submit(() -> {
                try {
                    doc.extractLinks().forEach(t -> down(phaser, t, depth - 1, regdDown, regdExtr, errors));
                } catch (IOException e) {
                    errors.put(url, e);
                } finally {
                    phaser.arrive();
                }
            });
        }
    }

    @Override
    public void close() {
        downServ.shutdown();
        extrServ.shutdown();
    }
}