package top.lazyr.util;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class ClsUtil {

    /**
     * 若className包含$，则返回所在的文件名
     * 若className不包含$，则不做处理直接返回className
     * @param className
     * @return
     */
    public static String extractFileName(String className) {
        if (!className.contains("$")) {
            return className;
        }
        return className.substring(0, className.indexOf("$"));
    }

    /**
     * a.b.c.func(java.lang.String) -> func(java.lang.String)
     * @param longMethodName
     * @return
     */
    public static String extractMethodName(String longMethodName) {
        String[] info = longMethodName.split("\\(");
        int lastIndex = info[0].lastIndexOf(".");
        String methodName = info[0].substring(lastIndex + 1);
        return methodName + "(" + info[1];
    }

    /**
     * 将修饰符数字转换为类的暴露方式
     * - public
     * - private
     * - abstract
     * - default
     * @param modifier
     * @return
     */
    public static String toModifierStr(int modifier) {
        if ((modifier & 1) != 0) {
            return  "public";
        }

        if ((modifier & 4) != 0) {
            return "protected";
        }

        if ((modifier & 2) != 0) {
            return "private";
        }

        return "default";
    }

    /**
     * 将修饰符数字转换为类的类型
     * - interface
     * - abstract
     * - normal
     * @param modifier
     * @return
     */
    public static String toTypeStr(int modifier) {
        if ((modifier & 512) != 0) {
            return "interface";
        }

        if ((modifier & 1024) != 0) {
            return "abstract";
        }

        return "normal";
    }
}
