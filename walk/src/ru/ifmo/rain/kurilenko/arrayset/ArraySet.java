package ru.ifmo.rain.kurilenko.arrayset;

import java.util.*;

public class ArraySet<E extends Comparable<E>> extends AbstractSet<E> implements SortedSet<E> {
    private List<E> a;
    private Comparator<? super E> comp = null;
    private boolean def;

    public ArraySet() {
        a = new ArrayList<>();
    }
    public ArraySet(Collection<? extends E> c) {
        this (c, new Comparator<E>(){
            public int compare (E e1, E e2) {
                return e1.compareTo(e2);
            }
        });
        def = true;
    }
    public ArraySet (Collection<? extends E> c, Comparator<? super E> com) {
        comp = com;
        def = false;
        TreeSet<E> tree = new TreeSet<>(comp);
        tree.addAll(c);
        a = new ArrayList<>(tree);
    }

    private ArraySet (List<E> c, Comparator<? super E> com) {
        comp = com;
        def = false;
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
    public boolean contains(Object o) {
        return Collections.binarySearch(a, (E) o, comp) >= 0;
    }
    public ArraySet<E> headSet (E e) {
        int to = check(Collections.binarySearch(a, e, comp));
        return new ArraySet<>(a.subList(0, to), comp);
    }
    public ArraySet<E> tailSet (E e) {
        int from = check(Collections.binarySearch(a, e, comp));
        return new ArraySet<>(a.subList(from, size()), comp);
    }
    public Iterator<E> iterator () {
        return Collections.unmodifiableList(a).iterator();
    }
    public Comparator<? super E> comparator () {
        if (def) return null;
        return comp;
    }
    public int size() {
        if (a == null) return 0;
        return a.size();
    }
    public ArraySet<E> subSet (E from, E to) {
        int f = check(Collections.binarySearch(a, from, comp));
        int t = check(Collections.binarySearch(a, to, comp));
        if (f > t) f = t;
        return new ArraySet<E>(a.subList(f, t), comp);
    }
    private int check (int t) {
        if (t < 0) return - t - 1;
        else return t;
    }
}
