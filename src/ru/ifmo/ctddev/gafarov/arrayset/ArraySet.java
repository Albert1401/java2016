package ru.ifmo.ctddev.gafarov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private List<T> list;
    private Comparator<? super T> comparator;
    private boolean isReversed = false;

    public ArraySet() {
        this(null);
    }

    public ArraySet(Collection<T> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<T> collection, Comparator<? super T> comparator) {
        if (collection != null) {
            Set<T> set = new TreeSet<>(comparator);
            set.addAll(collection);
            list = Collections.unmodifiableList(new ArrayList<>(set));
        } else {
            list = Collections.unmodifiableList(Collections.emptyList());
        }
        this.comparator = comparator;
    }


    @Override
    public T lower(T t) {
        Objects.requireNonNull(t);
        int index = Collections.binarySearch(list, t, comparator);
        if (isReversed) {
            if (index == -size() - 1 || index == size() - 1) {
                return null;
            }
            return list.get(index < 0 ? -index - 1 : index + 1);
        } else {
            if (index == 0 || index == -1) {
                return null;
            }
            return list.get(index < 0 ? -index - 2 : index - 1);
        }
    }

    @Override
    public T floor(T t) {
        Objects.requireNonNull(t);
        int index = Collections.binarySearch(list, t, comparator);
        if (isReversed) {
            if (index == -size() - 1) {
                return null;
            }
            return list.get(index < 0 ? -index - 1 : index);
        } else {
            if (index == -1) {
                return null;
            }
            return list.get(index < 0 ? -index - 2 : index);
        }
    }

    private void reverse() {
        isReversed = !isReversed;
    }

    @Override
    public T ceiling(T t) {
        reverse();
        T res = floor(t);
        reverse();
        return res;
    }

    @Override
    public T higher(T t) {
        reverse();
        T res = lower(t);
        reverse();
        return res;
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return list.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(list, (T) o, comparator) >= 0;
    }

    private Iterator<T> getIterator(boolean descending) {
        if (isReversed ^ descending) {
            return list.listIterator(size() - 1);
        }
        return list.iterator();
    }

    @Override
    public Iterator<T> iterator() {
        return getIterator(false);
    }

    @Override
    public NavigableSet<T> descendingSet() {
        ArraySet<T> set = new ArraySet<>();
        set.list = list;
        set.comparator = comparator;
        set.reverse();
        return set;
    }

    @Override
    public Iterator<T> descendingIterator() {
        return getIterator(true);
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        Objects.requireNonNull(fromElement);
        Objects.requireNonNull(toElement);

        if (isReversed) {
            T swap = fromElement;
            fromElement = toElement;
            toElement = swap;
        }
        int from = Collections.binarySearch(list, fromElement, comparator);
        int to = Collections.binarySearch(list, toElement, comparator);
        if (from == to && !fromInclusive) {
            ArraySet<T> set = new ArraySet<>(null, comparator);
            set.isReversed = isReversed;
            return set;
        }
        if (from < 0) {
            from = -from - 1;
        } else {
            from = fromInclusive ? from : from + 1;
        }

        if (to < 0) {
            to = -to - 1;
        } else {
            to = toInclusive ? to + 1 : to;
        }

        ArraySet<T> set = new ArraySet<>();
        try {
            set.list = list.subList(from, to);
            set.isReversed = isReversed;
            set.comparator = comparator;
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException();
        }
        return set;
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (isEmpty()) {
            return new ArraySet<>();
        }
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (isEmpty()) {
            return new ArraySet<>();
        }
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        if (comparator == null) {
            return isReversed ? Collections.reverseOrder() : null;
        }
        return isReversed ? comparator.reversed() : comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        if (isReversed) {
            return list.get(size() - 1);
        }
        return list.get(0);
    }

    @Override
    public T last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        if (isReversed) {
            return list.get(0);
        }
        return list.get(size() - 1);
    }
}
