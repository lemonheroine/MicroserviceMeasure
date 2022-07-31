package top.lazyr.util;

import org.springframework.beans.BeanUtils;
import org.springframework.lang.NonNull;
import top.lazyr.exception.BeanUtilsException;
import top.lazyr.validator.VarValidator;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author lazyr
 * @created 2021/4/27
 */
public class ObjUtils {


    private ObjUtils() {
    }

    /**
     * 将 source 转换为 targetClass的类对象(只复制相同名字的成员变量)
     * 注意：source中为null的属性也不复制
     * @param source
     * @param targetClass
     * @param <S>
     * @param <T>
     * @return
     */
    public static <S, T> T cast(@NonNull S source, @NonNull Class<T> targetClass) {
        if (source == null || targetClass == null) {
            return null;
        }

        // Init the instance
        try {
            // New instance for the target class
            T targetInstance = targetClass.newInstance();
            // Copy properties
            BeanUtils.copyProperties(source, targetInstance);
            // Return the target instance
            return targetInstance;
        } catch (Exception e) {
            throw new BeanUtilsException(
                    "Failed to new " + targetClass.getName() + " instance or copy properties", e);
        }
    }

    /**
     * 批量将 source 转换为 targetClass(只复制相同的属性)
     * 注意：source中为null的属性也不复制
     * @param sources
     * @param targetClass
     * @param <T>
     * @param <R>
     * @return
     */
    public static <T, R> List<R> cast(@NonNull List<T> sources, @NonNull Class<R> targetClass) {
        if (sources == null || targetClass == null) {
            return null;
        }


        List<R> targets = new ArrayList<>();
        sources.forEach(source->{
            targets.add(cast(source, targetClass));
        });
        return targets;
    }

    /**
     * 将 source 属性转换为 Map（key为属性名，value为属性值）
     * - 若convertNull为true，则转换全部属性（值为null的属性，则直接赋值为null）
     * - 若convertNull为false，则只转换非空属性
     * @param source
     * @param convertNull
     * @param <T>
     * @return
     */
    private static <T> Map<String, Object> objToMap(@NonNull T source, boolean convertNull) {
        if (source == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();

        Field[] fields = source.getClass().getDeclaredFields();
        if (VarValidator.empty(fields)) {
            return map;
        }

        for (Field field : fields) {
            try {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object value = field.get(source);
                if (convertNull) {
                    map.put(fieldName, value);
                } else if (value != null) { // 不包含取值为null属性 且 属性值不为null
                    map.put(fieldName, value);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return map;
    }

    /**
     * 将 map 转换为 T 对象（key为属性名，value为属性值）
     * @param map
     * @param <T>
     * @return
     */
    public static <T> T mapToObj(@NonNull Map<String, Object> map, @NonNull T targetObj) {
        if (map == null ||targetObj == null) {
            return null;
        }

        Field[] fields = targetObj.getClass().getDeclaredFields();
        if (VarValidator.empty(map) || VarValidator.empty(fields)) {
            return targetObj;
        }
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                String name = field.getName();
                Object value = map.get(name);
                if (value != null && value.getClass() == field.getType()) {
                    field.set(targetObj, map.get(name));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return targetObj;
    }

    /**
     * 获取source的变量名Set
     * - containNull为true，则返回所有变量名
     * - containNull为false，则返回所有值不为null的变量名
     * @param source
     * @param <T>
     * @return
     */
    private static <T> Set<String> getPropertiesNameSet(@NonNull T source, boolean containNull) {
        if (source == null) {
            return null;
        }

        Set<String> notNullFieldNames = new HashSet<>();

        Class<?> sourceClass = source.getClass();
        Field[] fields = sourceClass.getDeclaredFields();

        if (!VarValidator.notEmpty(fields)) {
            return notNullFieldNames;
        }

        for (Field field : fields){
            try {
                field.setAccessible(true);
                Object value = field.get(source);
                if (containNull) {
                    notNullFieldNames.add(field.getName());
                } else if (value != null) { // 不获取值为null属性 且 属性值不为null
                    notNullFieldNames.add(field.getName());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return notNullFieldNames;
    }

    /**
     * 获取source所有值不为null的变量名
     * @param source
     * @param <T>
     * @return
     */
    public static <T> String[] getNotNullPropertiesNames(@NonNull T source) {
        return getPropertiesNameSet(source, false).toArray(new String[0]);
    }

    /**
     * 获取source所有的变量名
     * @param source
     * @param <T>
     * @return
     */
    public static <T> String[] getAllPropertiesNames(@NonNull T source) {
        return getPropertiesNameSet(source, true).toArray(new String[0]);
    }

    /**
     * 获取source所有值为null的变量名
     * @param source
     * @param <T>
     * @return
     */
    public static <T> String[] getNullPropertiesNames(@NonNull T source) {
        Set<String> allPropertiesNameSet = getPropertiesNameSet(source, true);
        Set<String> notNullPropertiesNameSet = getPropertiesNameSet(source, false);
        allPropertiesNameSet.removeAll(notNullPropertiesNameSet);
        return allPropertiesNameSet.toArray(new String[0]);
    }

}
