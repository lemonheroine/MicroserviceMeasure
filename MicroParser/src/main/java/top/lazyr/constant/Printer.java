package top.lazyr.constant;

import org.apache.poi.ss.formula.functions.T;
import top.lazyr.validator.VarValidator;

import java.util.List;
import java.util.Map;

/**
 * @author lazyr
 * @created 2022/2/20
 */
public interface Printer {
    String SEPARATOR = "=========================";
    static void printTitle(Object title) {
        System.out.println(SEPARATOR + title + SEPARATOR);
    }

    static <T> void printList(List<T> values) {
        if (VarValidator.empty(values)) {
            System.out.println("List is empty.");
        }
        for (T value : values) {
            System.out.println(value);
        }
    }

    static <K,V> void printMap(Map<K, V> map) {
        if (VarValidator.empty(map)) {
            System.out.println("Map is empty.");
        }
        for (K k : map.keySet()) {
            System.out.println(k + " => " + map.get(k));
        }
    }
}
