package top.lazyr.microserviceName_structure.classinfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.microserviceName_structure.classinfo.model.FieldInfo;
import top.lazyr.microserviceName_structure.classinfo.model.MethodInfo;
import top.lazyr.util.ExcelUtil;
import top.lazyr.util.FileUtil;
import top.lazyr.util.PathUtil;
import top.lazyr.util.StrUtil;
import top.lazyr.validator.VarValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class Table125Writer {
    private static Logger logger = LoggerFactory.getLogger(Table125Writer.class);


    public static void writeTable125(String msPath) {
        List<String> svcPaths = FileUtil.getSubCatalogPaths(msPath);
        if (VarValidator.empty(svcPaths)) {
            logger.info("微服务项目目录 {} 下无服务项目目录", msPath);
            return;
        }
        for (String svcPath : svcPaths) {
            writeTable1OfSvc(svcPath, PathUtil.getCurrentCatalog(msPath));
            writeTable2OfSvc(svcPath, PathUtil.getCurrentCatalog(msPath));
            writeTable5OfSvc(svcPath, PathUtil.getCurrentCatalog(msPath));
        }
    }

    /**
     * ⼦表1记录微服务中的所有⽅法:
     * 类名（class_name）；⽅法签名（method_name(parameter_types)）；⽅法修饰符（modifier）；返回值类型（return_type)
     * @param svcPath
     * @param prefix
     */
    public static void writeTable1OfSvc(String svcPath, String prefix) {
        ClassInfoParser parser = new ClassInfoParser();
        List<MethodInfo> methodInfos = parser.parseMethodInfos(svcPath, false);
        String svcName = PathUtil.getCurrentCatalog(svcPath);

        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("类名", "方法签名", "方法修饰符", "返回值类型"));
        if (VarValidator.notEmpty(methodInfos)) {
            for (MethodInfo methodInfo : methodInfos) {
                List<String> info = new ArrayList<>();
                info.add(methodInfo.getBelongClassName());
                info.add(methodInfo.getMethodName() + "(" + StrUtil.list2Csv(methodInfo.getParamClassNames()) + ")");
                info.add(methodInfo.getMethodModifier());
                info.add(methodInfo.getReturnClassName());
                infos.add(info);
            }
        } else {
            logger.info("服务{}中的实体类无方法信息", svcName);
        }

        ExcelUtil.write2Excel( prefix + "/" + svcName + "_structure.xlsx", "data1", infos);
    }

    /**
     * ⼦表5记录微服务中的所有操作：接⼝名（interface_name)；操作签名（operation_name）；操作修饰符（modifier）；参数类型列表（input_message_types，多个参数类型使⽤“,”分割）； 返回值类型（input_message_types，多个参数类型使⽤“,”分割）
     * @param svcPath
     * @param prefix
     */
    public static void writeTable5OfSvc(String svcPath, String prefix) {
        ClassInfoParser parser = new ClassInfoParser();
        List<MethodInfo> methodInfos = parser.parseMethodInfos(svcPath, true);
        String svcName = PathUtil.getCurrentCatalog(svcPath);

        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("接口名", "操作签名", "操作修饰符", "返回值类型"));
        if (VarValidator.notEmpty(methodInfos)) {
            for (MethodInfo methodInfo : methodInfos) {
                List<String> info = new ArrayList<>();
                info.add(methodInfo.getBelongClassName());
                info.add(methodInfo.getMethodName() + "(" + StrUtil.list2Csv(methodInfo.getParamClassNames()) + ")");
                info.add(methodInfo.getMethodModifier());
                info.add(methodInfo.getReturnClassName());
                infos.add(info);
            }
        } else {
            logger.info("服务{}中的接口类无操作信息", svcName);
        }

        ExcelUtil.append2Excel( prefix + "/" + svcName + "_structure.xlsx", "data5", infos);
    }

    /**
     * ⼦表2记录微服务中的所有字段：
     * 类名（class_name）；字段名（field_name）；字段修饰符（modifier）；字段类型（field_type）
     * @param svcPath
     * @param prefix
     */
    public static void writeTable2OfSvc(String svcPath, String prefix) {
        ClassInfoParser parser = new ClassInfoParser();
        List<FieldInfo> fieldInfos = parser.parseFieldInfos(svcPath, false);
        String svcName = PathUtil.getCurrentCatalog(svcPath);

        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("类名", "字段名", "字段修饰符", "字段类型"));
        if (VarValidator.notEmpty(fieldInfos)) {
            for (FieldInfo fieldInfo : fieldInfos) {
                List<String> info = new ArrayList<>();
                info.add(fieldInfo.getBelongClassName());
                info.add(fieldInfo.getFieldName());
                info.add(fieldInfo.getModifier());
                info.add(fieldInfo.getClassName());
                infos.add(info);
            }
        } else {
            logger.info("服务{}中的实体类无字段信息", svcName);
        }
        ExcelUtil.append2Excel(prefix + "/" + svcName + "_structure.xlsx", "data2", infos);
    }


    public static void main(String[] args) {
        String msPath = "/Users/lazyr/School/laboratory/OSMicroservicesCollection-all/OSMicroservicesCollection-lazyr-fix/131-mall-cloud-alibaba";
        Table125Writer.writeTable125(msPath);
    }
}
