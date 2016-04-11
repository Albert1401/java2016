package ru.ifmo.ctddev.gafarov.mapper;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Class designed to parallel calculations on lists.
 *
 * @see info.kgeorgiy.java.advanced.concurrent.ListIP
 */
public class IterativeParallelism implements ListIP {
    private static class RunOnList<T, R> implements Runnable {
        private Function<List<? extends T>, R> function;
        private List<? extends T> list;
        private R result;

        public RunOnList(List<? extends T> list, Function<List<? extends T>, R> function) {
            this.function = function;
            this.list = list;
        }

        @Override
        public void run() {
            result = function.apply(list);
        }

        public R getResult() {
            return result;
        }
    }

    private <T> List<List<? extends T>> split(List<? extends T> list, int n) {
        List<List<? extends T>> pieces = new ArrayList<>(n);
        double step = Math.max(list.size() / (double) n, 1);

        int from = 0;
        for (double to = step; from < list.size(); to += step) {
            int ito = (int) Math.floor(to);
            pieces.add(list.subList(from, Math.min(ito, list.size())));
            from = ito;
        }
        return pieces;
    }

    private ParallelMapper parallelMapper;

    /**
     * Create instance.
     * All methods of ListIP will use given amount of threads
     */
    public IterativeParallelism() {
    }

    /**
     * Create instance of IterativeParallelism that will use {@code parallelMapper}
     * All methods of ListIP will ignore given amount of threads
     *
     * @param parallelMapper parallelMapper to use
     * @throws NullPointerException if {@code parallelMapper == null}
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    private <T, R> List<R> runPieces(List<? extends T> list, int n, Function<List<? extends T>, R> function) throws InterruptedException {
        if (parallelMapper == null) {
            List<RunOnList<T, R>> runOnLists = split(list, n).stream().map(t -> new RunOnList<>(t, function)).collect(Collectors.toList());
            List<Thread> threads = runOnLists.stream().map(Thread::new).collect(Collectors.toList());
            threads.forEach(Thread::start);
            for (Thread t : threads) {
                t.join();
            }
            return runOnLists.stream().map(RunOnList::getResult).collect(Collectors.toList());
        }
        return parallelMapper.map(function, split(list, n));
    }

    /**
     * Joins string representations of list
     * If this instance was created by #IterativeParallelism(ParallelMapper), it will use parallelMapper.
     * Otherwise joining occurs simultaneously in {@code i} threads
     *
     * @param i    number of threads
     * @param list list of elements to describe
     * @return joining result
     * @throws InterruptedException if any of threads has interrupted
     * @see java.util.List
     */
    @Override
    public String join(int i, List<?> list) throws InterruptedException {
        Function<List<?>, StringBuilder> toStringList = t -> {
            StringBuilder builder = new StringBuilder();
            t.stream().forEach(el -> builder.append(el.toString()));
            return builder;
        };
        return runPieces(list, i, toStringList).stream().reduce((n1, n2) -> n1.append(n2)).get().toString();
    }

    /**
     * Filters list by given predicate.
     * If this instance was created by #IterativeParallelism(ParallelMapper), it will use parallelMapper.
     * Otherwise filtering occurs simultaneously in {@code i} threads
     *
     * @param i         number of threads
     * @param list      list to filter
     * @param predicate predicate to filter to
     * @param <T>       type that describes elements of given list
     * @return list of elements satisfying given predicate
     * @throws InterruptedException if any of threads has interrupted
     * @see java.util.function.Predicate
     * @see java.util.List
     */
    @Override
    public <T> List<T> filter(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        Function<List<? extends T>, List<? extends T>> filterList = t -> t.stream().filter(predicate).collect(Collectors.toList());
        return runPieces(list, i, filterList).stream().flatMap(piece -> piece.stream()).collect(Collectors.toList());
    }

    /**
     * Maps list by given function.
     * If this instance was created by #IterativeParallelism(ParallelMapper), it will use parallelMapper.
     * Otherwise mapping occurs simultaneously in {@code i} threads
     *
     * @param i        number of threads
     * @param list     list of elements to map
     * @param function function to map each element
     * @param <T>      type that describes elements of given list
     * @param <U>      type that describes elements of resulting list
     * @return list containing results of map to each element
     * @throws InterruptedException if any of threads has interrupted
     * @see java.util.function.Function
     * @see java.util.List
     */
    @Override
    public <T, U> List<U> map(int i, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        Function<List<? extends T>, List<? extends U>> mapList = t -> t.stream().map(function).collect(Collectors.toList());
        return runPieces(list, i, mapList).stream().flatMap(piece -> piece.stream()).collect(Collectors.toList());
    }

    /**
     * Returns maximum of given list.
     * If this instance was created by #IterativeParallelism(ParallelMapper), it will use parallelMapper.
     * Otherwise searching occurs simultaneously in {@code i} threads
     *
     * @param i          number of threads
     * @param list       list of elements to find maximum
     * @param comparator comparator that helps to compare elements
     * @param <T>        type that describes elements of given list
     * @return maximum element in the list
     * @throws InterruptedException if any of threads has interrupted
     * @see java.util.Comparator
     * @see java.util.List
     */
    @Override
    public <T> T maximum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        Function<List<? extends T>, T> function = maxList -> maxList.stream().max(comparator).get();
        return function.apply(runPieces(list, i, function));
    }

    /**
     * Returns minimum of given list.
     * If this instance was created by #IterativeParallelism(ParallelMapper), it will use parallelMapper.
     * Otherwise searching occurs simultaneously in {@code i} threads
     *
     * @param i          number of threads
     * @param list       list of elements to find minimum
     * @param comparator comparator that helps to compare elements
     * @param <T>        type that describes elements of given list
     * @return minimum element in the list
     * @throws InterruptedException if any of threads has interrupted
     * @see java.util.Comparator
     * @see java.util.List
     */
    @Override
    public <T> T minimum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(i, list, comparator == null ? Collections.reverseOrder() : comparator.reversed());
    }

    /**
     * Checks that all elements of list satisfies given predicate.
     * If this instance was created by #IterativeParallelism(ParallelMapper), it will use parallelMapper.
     * Otherwise checking occurs simultaneously in {@code i} threads
     *
     * @param i         number of threads
     * @param list      list of elements to check
     * @param predicate predicate to check to
     * @param <T>       type that describes elements of given list
     * @return {@code boolean} if all elements satisfying predicate; false otherwise
     * @throws InterruptedException if any of threads has interrupted
     * @see java.util.function.Predicate
     * @see java.util.List
     */
    @Override
    public <T> boolean all(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return runPieces(list, i, t -> t.stream().allMatch(predicate)).stream().allMatch(t1 -> t1);
    }

    /**
     * Checks that any element of list satisfies given predicate.
     * If this instance was created by #IterativeParallelism(ParallelMapper), it will use parallelMapper.
     * Otherwise checking occurs simultaneously in {@code i} threads
     *
     * @param i         number of threads
     * @param list      list of elements to check
     * @param predicate predicate to check to
     * @param <T>       type that describes elements of given list
     * @return {@code boolean} if any element satisfying predicate; false otherwise
     * @throws InterruptedException if any of threads has interrupted
     * @see java.util.function.Predicate
     * @see java.util.List
     */
    @Override
    public <T> boolean any(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return runPieces(list, i, t -> t.stream().anyMatch(predicate)).stream().anyMatch(t1 -> t1);
    }
}