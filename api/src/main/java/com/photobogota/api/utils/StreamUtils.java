package com.photobogota.api.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collector;

public class StreamUtils {

    /**
     * Colector personalizado para obtener los últimos N elementos de un Stream
     */
    public static <T> Collector<T, ?, List<T>> lastN(int n) {
        return Collector.of(
                LinkedList::new,
                (list, item) -> {
                    list.add(item);
                    if (list.size() > n) {
                        list.poll();
                    }
                },
                (l1, l2) -> {
                    while (l1.size() + l2.size() > n && !l1.isEmpty()) {
                        l1.poll();
                    }
                    l1.addAll(l2);
                    return l1;
                },
                (LinkedList<T> list) -> new ArrayList<T>(list));
    }
}