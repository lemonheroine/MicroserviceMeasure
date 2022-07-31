package top.lazyr.microserviceName_attribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.microserviceName_attribute.model.FuncCallSelfFieldInfo;
import top.lazyr.microserviceName_structure.classinfo.ClassInfoParser;
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
public class Table1Writer {
    private static Logger logger = LoggerFactory.getLogger(Table1Writer.class);

    public static void writeTable(String msPath) {
        List<String> svcPaths = FileUtil.getSubCatalogPaths(msPath);
        if (VarValidator.empty(svcPaths)) {
            logger.info("微服务项目目录 {} 下无服务项目目录", msPath);
            return;
        }
        for (String svcPath : svcPaths) {
            writeTable1OfSvc(svcPath, PathUtil.getCurrentCatalog(msPath));
            writeTable2OfSvc(svcPath, PathUtil.getCurrentCatalog(msPath));
        }
    }

    /**
     * @param svcPath
     */
    private static void writeTable1OfSvc(String svcPath, String prefix) {
        FuncCallSelfFieldParser parser = new FuncCallSelfFieldParser();
        List<FuncCallSelfFieldInfo> funcCallSelfFieldInfos = parser.parseFunc(svcPath, false);
        String svcName = PathUtil.getCurrentCatalog(svcPath);
        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("类名", "方法签名", "参数类型列表", "返回值类型", "方法使用的字段类型列表"));
        if (VarValidator.notEmpty(funcCallSelfFieldInfos)) {
            for (FuncCallSelfFieldInfo funcCallSelfFieldInfo : funcCallSelfFieldInfos) {
                List<String> info = new ArrayList<>();
                info.add(funcCallSelfFieldInfo.getBelongClassName());
                info.add(funcCallSelfFieldInfo.getMethodName() + "(" + StrUtil.list2Csv(funcCallSelfFieldInfo.getParamClassNames()) + ")");
                info.add(StrUtil.list2Csv(funcCallSelfFieldInfo.getParamClassNames()));
                info.add(funcCallSelfFieldInfo.getReturnClassName());
                info.add(StrUtil.list2Csv(funcCallSelfFieldInfo.getSelfFields()));
                infos.add(info);
            }
        } else {
            logger.info("{} 中实体类无方法", svcName);
        }

        ExcelUtil.write2Excel(prefix + "/" + svcName + "_attribute.xlsx", "data1", infos);
    }

    public static void writeTable2OfSvc(String svcPath, String prefix) {
        ClassInfoParser parser = new ClassInfoParser();
        List<MethodInfo> methodInfos = parser.parseMethodInfos(svcPath, true);
        String svcName = PathUtil.getCurrentCatalog(svcPath);

        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("接口名", "操作签名", "操作参数列表", "操作修饰符", "返回值类型"));
        if (VarValidator.notEmpty(methodInfos)) {
            for (MethodInfo methodInfo : methodInfos) {
                List<String> info = new ArrayList<>();
                info.add(methodInfo.getBelongClassName());
                info.add(methodInfo.getMethodName() + "(" + StrUtil.list2Csv(methodInfo.getParamClassNames()) + ")");
                info.add(StrUtil.list2Csv(methodInfo.getParamClassNames()));
//                info.add(methodInfo.getMethodModifier().equals("public") ? "true" : "false");
                info.add(methodInfo.getMethodModifier());
                info.add(methodInfo.getReturnClassName());
                infos.add(info);
            }
        } else {
            logger.info("服务{}中的接口类无操作信息");
        }

        ExcelUtil.append2Excel( prefix + "/" + svcName + "_attribute.xlsx", "data2", infos);
    }
}
