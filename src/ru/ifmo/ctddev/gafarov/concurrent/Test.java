package ru.ifmo.ctddev.gafarov.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by clitcommander on 4/8/16.
 */
public class Test {
    public static void main(String[] args) throws InterruptedException {
        IterativeParallelism iterativeParallelism = new IterativeParallelism();
        List<Integer> list = new ArrayList<>();
        System.out.println(iterativeParallelism.maximum(5, list, null));
    }
}
