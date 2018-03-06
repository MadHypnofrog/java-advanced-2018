package ru.ifmo.rain.kurilenko.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {
    private List<E> a;
    private Comparator<? super E> comp = null;

    public ArraySet() {
        a = new ArrayList<>();
    }

    public ArraySet(Collection<? extends E> c) {
        this(c, null);
    }

    public ArraySet(Collection<? extends E> c, Comparator<? super E> com) {
        comp = com;
        try {
            TreeSet<E> tree = new TreeSet<>(comp);
            tree.addAll(c);
            a = new ArrayList<>(tree);
        } catch (ClassCastException e) {
            System.err.println ("Error: trying to create an array of non-comparable objects using default comparator");
        }
    }

    private ArraySet(List<E> c, Comparator<? super E> com) {
        comp = com;
        a = c;
    }

    public E first() {
        if (size() != 0) return a.get(0);
        else throw new NoSuchElementException("Error: list is empty");
    }

    public E last() {
        if (size() != 0) return a.get(size() - 1);
        else throw new NoSuchElementException("Error: list is empty");
    }

    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(a, (E) o, comp) >= 0;
    }

    public ArraySet<E> headSet(E e) {
        int to = check(Collections.binarySearch(a, e, comp));
        return new ArraySet<>(a.subList(0, to), comp);
    }

    public ArraySet<E> tailSet(E e) {
        int from = check(Collections.binarySearch(a, e, comp));
        return new ArraySet<>(a.subList(from, size()), comp);
    }

    public Iterator<E> iterator() {
        return Collections.unmodifiableList(a).iterator();
    }

    public Comparator<? super E> comparator() {
        return comp;
    }

    public int size() {
        return a.size();
    }

    public ArraySet<E> subSet(E from, E to) {
        return tailSet(from).headSet(to);
    }

    private int check(int t) {
        if (t < 0) return -t - 1;
        else return t;
    }
}
