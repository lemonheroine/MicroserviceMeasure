package top.lazyr.util;

import java.io.File;

/**
 * @author lazyr
 * @created 2021/11/26
 */
public class PathUtil {
    public static String getCurrentCatalog(String path) {
        File file = new File(path);
        return file.getName();
    }

}
