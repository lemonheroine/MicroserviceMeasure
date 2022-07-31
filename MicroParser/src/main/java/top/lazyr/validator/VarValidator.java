package top.lazyr.validator;

import java.util.Collection;
import java.util.Map;

/**
 * @author lazyr
 * @created 2022/4/30
 */
public class VarValidator {

    private VarValidator() {}

    /**
     * - 若 collection为null 或 collection.size == 0，则返回false
     * - 其他情况，返回true
     * @param coll
     * @return
     */
    public static boolean notEmpty(Collection coll) {
        return !(coll == null || coll.size() == 0);
    }

    /**
     * - 若 arr为null 或 arr.len == 0，则返回false
     * - 其他情况，返回true
     * @param arr
     * @return
     */
    public static <T> boolean notEmpty(T[] arr) {
        return !(arr == null || arr.length == 0);
    }

    /**
     * - 若 collection为null 或 collection.size == 0，则返回true
     * - 其他情况，返回true
     * @param coll
     * @return
     */
    public static boolean empty(Collection coll) {
        return !notEmpty(coll);
    }

    /**
     * - 若 arr为null 或 arr.len == 0，则返回true
     * - 其他情况，返回true
     * @param arr
     * @return
     */
    public static <T> boolean empty(T[] arr) {
        return !notEmpty(arr);
    }

    /**
     * - 若 map为null 或 map.size == 0，则返回true
     * - 其他情况，返回true
     * @param map
     * @return
     */
    public static boolean empty(Map map) {
        return !notEmpty(map);
    }

    /**
     * - 若 map为null 或 map.size == 0，则返回false
     * - 其他情况，返回true
     * @param map
     * @return
     */
    public static boolean notEmpty(Map map) {
        return !(map == null || map.size() == 0);
    }



}
