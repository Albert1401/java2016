package ru.ifmo.ctddev.gafarov.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *  Class designed to manage parallel functions on lists.
 * @see info.kgeorgiy.java.advanced.mapper.ParallelMapper
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Task<?, ?>> tasksFIFO = new ArrayDeque<>();
    private final ArrayList<Thread> threads = new ArrayList<>();

    /**
     * Creates instance of class working on {@code n_threads}
     * @param n_threads number of threads to work on
     */
    public ParallelMapperImpl(int n_threads) {
        for (int i = 0; i < n_threads; i++) {
            threads.add(new Thread(() -> {
                while (!Thread.interrupted()) {
                    Task<?, ?> task;
                    synchronized (tasksFIFO) {
                        while (tasksFIFO.isEmpty()) {
                            try {
                                tasksFIFO.wait();
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                        task = tasksFIFO.poll();
                    }
                    task.process();
                }
            }));
        }
        threads.forEach(Thread::start);
    }

    private class Task<T, R> {
        private Function<? super T, ? extends R> function;
        private T element;
        private boolean done = false;
        private R result;

        public Task(Function<? super T, ? extends R> function, T element) {
            this.element = element;
            this.function = function;
        }

        public synchronized void process() {
            result = function.apply(element);
            done = true;
            notifyAll();
        }

        public synchronized R getResult() throws InterruptedException {
            while (!done) {
                wait();
            }
            return result;
        }

    }

    /**
     * Creates Task for each element and pass it to worker threads
     * @param function function to apply to each element
     * @param list input data
     * @param <T> type that describes input data
     * @param <R> type that describes output data
     * @return list of results of applying function to each element
     * @throws InterruptedException if any thread worker has interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        List<Task<T, R>> tasksForList = list.stream().map(el -> new Task<T, R>(function, el)).collect(Collectors.toList());
        tasksForList.forEach(tasksFIFO::add);
        synchronized (tasksFIFO) {
            tasksFIFO.notifyAll();
        }

        List<R> answer = new ArrayList<>(tasksForList.size());
        for (Task<T, R> task : tasksForList) {
            answer.add(task.getResult());
        }
        return answer;
    }

    /**
     * Stops all threads
     * @throws InterruptedException if any thread worker has interrupted
     */
    @Override
    public void close() throws InterruptedException {
        synchronized (tasksFIFO){
            tasksFIFO.clear();
            threads.forEach(Thread::interrupt);
            threads.clear();
        }
    }
}
