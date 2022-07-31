package top.lazyr.util;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class TypeUtil {
    /**
     * 若arrName为数组，则返回数组的类型类名
     * - 若数组的类型为基本数据类型则将其转换为包装类再返回
     * - 若数组的类型为普通类则返回普通类
     * 若arrName为普通类，则不做处理直接返回arrName
     * @param arrName
     * @return
     */
    public static String arr2Class(String arrName) {
        if (!arrName.contains("[]")) {
            return arrName;
        }
        return unboxing(arrName.substring(0, arrName.indexOf("[")));
    }

    /**
     * 若baseType为基本数据类型，则将其转换为包装器类型
     * 若baseType不为基本数据类型，则不做处理
     * @param baseType
     * @return
     */
    public static String unboxing(String baseType) {
        String className = baseType;
        switch (baseType) {
            case "byte":
                className = Byte.class.getName();
                break;
            case "short":
                className = Short.class.getName();
                break;
            case "int":
                className = Integer.class.getName();
                break;
            case "long":
                className = Long.class.getName();
                break;
            case "float":
                className = Float.class.getName();
                break;
            case "double":
                className = Double.class.getName();
                break;
            case "char":
                className = Character.class.getName();
                break;
            case "boolean":
                className = Boolean.class.getName();
                break;
        }
        return className;
    }

    /**
     * 若typeName包含基本类型，则返回对应包装类的类名
     * - typeName = "int", return  "java.lang.Integer"
     * - typeName = "int[]", return "java.lang.Integer[]"
     * - typeName = "top.lazyr.User", return "top.lazyr.User"
     * - typeName = "top.lazyr.User[]", return "top.lazyr.User[]"
     * @param typeName
     * @return
     */
    public static String unbox(String typeName) {
        if (!typeName.contains("[]")) {
            return unboxing(typeName);
        }
        return unboxing(typeName.substring(0, typeName.indexOf("["))) + "[]";
    }
}

