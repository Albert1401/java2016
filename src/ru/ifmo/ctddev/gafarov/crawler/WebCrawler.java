package ru.ifmo.ctddev.gafarov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.stream.Collectors;

/**
 * Class that downloads websites recursively in parallel with the specified depth and returns all the files and links
 * that are downloaded.
 * <p/>
 * User needs to provide {@link Downloader}
 *
 * @see info.kgeorgiy.java.advanced.crawler.Crawler
 */
public class WebCrawler implements Crawler {
    private Downloader downloader;
    ExecutorService downServ;
    ExecutorService extrServ;

    /**
     * Creates instance of class with specified parameters.
     *
     * @param downloader  instance of class that implement {@link Downloader}
     * @param downloaders maximum number of simultaneous downloading
     * @param extractors  maximum number of simultaneous extracting links
     * @param perHost     ignored
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        downServ = Executors.newFixedThreadPool(downloaders);
        extrServ = Executors.newFixedThreadPool(extractors);
    }

    /**
     * Download pages starting with the specified url.
     *
     * @param url   url to start
     * @param depth maximum depth to go to
     * @return list of visited websites
     */
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

    /**
     * Main method to execute, walks websites according depth
     *
     * @param args {url, depth, downloaders max, exctractors max, perhosts max}
     */
    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Usage: <url> <depth> <downloaders max> <exctractors max> <perhosts max>");
        }
        try {
            new WebCrawler(new CachingDownloader(), Integer.valueOf(args[2]), Integer.valueOf(args[3]), Integer.valueOf(args[4]))
                    .download(args[0], Integer.valueOf(args[1]));
        } catch (IOException e) {
            System.out.println("Couldn't detch https page" + e.getMessage());
        }
    }

    /**
     * Closes class, stops all threads
     */
    @Override
    public void close() {
        downServ.shutdown();
        extrServ.shutdown();
    }
}