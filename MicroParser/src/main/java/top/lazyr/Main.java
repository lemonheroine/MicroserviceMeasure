package top.lazyr;

import top.lazyr.microserviceName_attribute.Table1Writer;
import top.lazyr.microservice_structure.MsTable12Writer;
import top.lazyr.microserviceName_structure.classinfo.Table125Writer;
import top.lazyr.microserviceName_structure.fieldcall.Table47Writer;
import top.lazyr.microserviceName_structure.methodcall.Table368Writer;
import top.lazyr.util.FileUtil;
import top.lazyr.util.PathUtil;

import java.io.File;
import java.util.List;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        // 存放所有微服务项目的目录
        List<String> msPaths = FileUtil.getSubCatalogPaths("/Users/mike/Desktop/compile/compile_0622");
        for (String msPath : msPaths) {
            // System.out.println(msPath);
            writeData(msPath);
        }
        // msPath：具体的一个微服务目录
//        String msPath = "/Users/lazyr/School/laboratory/OSMicroservicesCollection-all/OSMicroservicesCollection-lazyr-fix/190-wanxin-p2p";
//        writeData(msPath);
    }

    /**
     * 读取msPath下所有数据，并输出三张表的结果
     * @param msPath
     */
    public static void writeData(String msPath) {
        String msName = PathUtil.getCurrentCatalog(msPath);
        File file = new File("/Users/mike/Desktop/compile/lazy_ms_detection/src/main/resources/" + msName);
        if (!file.exists()) {
            file.mkdirs();
        }
        writeStructureOfSvc(msPath);
        writeAttributeOfSvc(msPath);
        writeStructureOfMs(msPath);
    }

    /**
     * 写入[microserviceName]_structure.xlsx
     * @param msPath
     */
    public static void writeStructureOfSvc(String msPath) {
        Table125Writer.writeTable125(msPath);
        Table368Writer.writeTable3689(msPath);
        Table47Writer.writeTable47(msPath);
    }

    /**
     * 写入[microserviceName]_attribute.xlsx
     * @param msPath
     */
    public static void writeAttributeOfSvc(String msPath) {
        Table1Writer.writeTable(msPath);
    }

    /**
     * 写入[microservice]_structure.xlsx
     * @param msPath
     */
    public static void writeStructureOfMs(String msPath) {
        MsTable12Writer.writeTable12(msPath);
    }
}
