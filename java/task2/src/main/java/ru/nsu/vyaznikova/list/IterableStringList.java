package ru.nsu.vyaznikova.list;

import java.util.Iterator;
import java.util.List;

public interface IterableStringList extends Iterable<String> {
    void addFirst(String value);
    int size();
    @Override
    Iterator<String> iterator();
    List<String> snapshot();
}