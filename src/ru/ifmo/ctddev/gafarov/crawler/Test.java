package ru.ifmo.ctddev.gafarov.crawler;

import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Result;

import java.io.IOException;

/**
 * Created by clitcommander on 4/12/16.
 */
public class Test {
    public static void main(String[] args) throws IOException {
        Crawler crawler = new WebCrawler(new CachingDownloader(), 5, 5, 0);
        Result result = crawler.download("http://ifmo.ru/", 1);
        System.out.println(result.getDownloaded());
        crawler.close();
    }
}
