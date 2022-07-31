package top.lazyr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.constant.Printer;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * @author lazyr
 * @created 2021/11/4
 */
public class FileUtil {
    private static Logger logger = LoggerFactory.getLogger(FileUtil.class);


    /**
     * 获取传入路径 catalog 下的所有后缀为 suffix 文件的绝对路径
     * @param catalog
     * @return
     */
    public static List<String> getFilesAbsolutePath(String catalog, String suffix) {
        List<String> filesAbsolutePath = new ArrayList<>();
        getFilesAbsolutePath(catalog, suffix,filesAbsolutePath);
        return filesAbsolutePath;
    }


    private static void getFilesAbsolutePath(String path, String suffix, List<String> filesAbsolutePath) {
        File file = new File(path);
        if (!file.exists()) {
            logger.info(path + "不存在");
            return;
        }

        if (file.isDirectory()) { // 若是文件夹
            for (File subFile : file.listFiles()) {
                getFilesAbsolutePath(subFile.getAbsolutePath(), suffix, filesAbsolutePath);
            }
        } else if (file.isFile() && file.getName().contains(suffix)) { // TODO: 优化文件名后缀判断逻辑
            filesAbsolutePath.add(file.getAbsolutePath());
        }
    }

    public static void append2File(String filePath, String content) {
        write2File(filePath, content, true);
    }

    public static void write2File(String filePath, String content) {
        write2File(filePath, content, false);
    }

    private static void write2File(String filePath, String content, boolean append) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filePath, append));
            out.write(content);
            out.close();
            System.out.println(filePath + "写入成功！");
        } catch (IOException e) {
        }
    }

    public static List<String> readFileByLine(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        List<String> content = new ArrayList<>();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                content.add(tempStr);
            }
            reader.close();
            return content;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return content;
    }

    public static File[] getSubCatalogs(String catalogPath) {
        File catalog = new File(catalogPath);
        if (!catalog.isDirectory()) {
            return null;
        }
        return catalog.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
    }

    public static List<String> getSubCatalogPaths(String catalogPath) {
        File[] subCatalogs = getSubCatalogs(catalogPath);
        if (subCatalogs == null) {
            return null;
        }
        List<String> subCatalogPaths = new ArrayList<>();
        for (File subCatalog : subCatalogs) {
            if (PathUtil.getCurrentCatalog(subCatalog.getAbsolutePath()).indexOf(".") != 0) {
                subCatalogPaths.add(subCatalog.getAbsolutePath());
            }

        }
        return subCatalogPaths;
    }

    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
    }


    public static void deleteFileOrDirectory(String path) {
        File file = new File(path);
        if (null != file) {
            if (!file.exists()) {
                return;
            }
            int i;
            // file 是文件
            if (file.isFile()) {
                boolean result = file.delete();
                // 限制循环次数，避免死循环
                for(i = 0; !result && i++ < 10; result = file.delete()) {
                    // 垃圾回收
                    System.gc();
                }
                return;
            }
            // file 是目录
            File[] files = file.listFiles();
            if (null != files) {
                for(i = 0; i < files.length; ++i) {
                    deleteFileOrDirectory(files[i].getPath());
                }
            }
            file.delete();
        }
    }







}
