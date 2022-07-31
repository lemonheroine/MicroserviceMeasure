package top.lazyr.microserviceName_structure;

import top.lazyr.microserviceName_structure.fieldcall.Table47Writer;
import top.lazyr.microserviceName_structure.classinfo.Table125Writer;
import top.lazyr.microserviceName_structure.methodcall.Table368Writer;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class Main {
    public static void main(String[] args) {
        String msPath = "/Users/lazyr/School/laboratory/ms_result/123-madao_service";
        writeStructure(msPath);
    }

    public static void writeStructure(String msPath) {
        Table125Writer.writeTable125(msPath);
        Table368Writer.writeTable3689(msPath);
        Table47Writer.writeTable47(msPath);
    }
}
