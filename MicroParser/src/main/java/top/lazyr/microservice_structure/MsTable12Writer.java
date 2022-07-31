package top.lazyr.microservice_structure;

import javassist.CtClass;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.manager.CtClassManager;
import top.lazyr.microservice_structure.model.MS;
import top.lazyr.microservice_structure.model.OpCallEdge;
import top.lazyr.microservice_structure.model.Svc;
import top.lazyr.util.ExcelUtil;
import top.lazyr.util.PathUtil;
import top.lazyr.util.SCUtil;
import top.lazyr.validator.VarValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class MsTable12Writer {
    private static Logger logger = LoggerFactory.getLogger(MsTable12Writer.class);

    private static CtClassManager ctClassManager = CtClassManager.getCtClassManager();


    public static void writeTable12(String msPath) {
        writeTable1(msPath);
        writeTable2(msPath);
    }

    public static void writeTable1(String msPath) {
        MsParser parser = new MsParser();
        MS ms = parser.parse(msPath);

        String msName = PathUtil.getCurrentCatalog(msPath);
        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("源服务名", "源类名", "源方法签名", "目标微服务名", "目标接口名", "目标操作签名", "权重"));
        List<Svc> svcs = ms.getSvcs();
        if (VarValidator.notEmpty(svcs)) {
            for (Svc svc : svcs) {
//                Printer.printTitle(svc.getName());
//                for (Operation op : svc.getOps()) {
//                    System.out.println(op);
//                }
                List<OpCallEdge> callEdges = svc.getCallEdges();
                if (VarValidator.empty(callEdges)) {
                    continue;
                }
                for (OpCallEdge callEdge : callEdges) {
                    List<String> info = new ArrayList<>();
                    info.add(callEdge.getInSvcName());
                    info.add(callEdge.getInClassName());
                    info.add(callEdge.getInCompleteFuncName());
                    info.add(callEdge.getOutSvcName());
                    info.add(callEdge.getOutClassName());
                    info.add(callEdge.getOutCompleteFuncName());
                    info.add(callEdge.getWeight() + "");
                    infos.add(info);
                }
            }
        } else {
            logger.info("{} 目录下无微服务项目", msPath);
        }

        ExcelUtil.write2Excel(msName + "/" + msName + "_structure.xlsx", "data1", infos);

    }

    /**
     * ⼦表2间记录两个微服务间的接⼝调⽤接⼝对应的结构化依赖关系：
     * 源微服务名（source_microservice）；源接⼝名（source_class）；源操作签名（source_method）；⽬标微服务名（destination_microservice）；⽬标接⼝签名（destination_interface）；⽬标字段名（destination_operation）；权重（weight)
     * @param msPath
     */
    public static void writeTable2(String msPath) {
        MsParser parser = new MsParser();
        MS ms = parser.parse(msPath);

        String msName = PathUtil.getCurrentCatalog(msPath);
        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("源服务名", "源类名", "源方法签名", "目标微服务名", "目标接口名", "目标操作签名", "权重"));
        List<Svc> svcs = ms.getSvcs();
        if (VarValidator.notEmpty(svcs)) {
            for (Svc svc : svcs) {
//                Printer.printTitle(svc.getName());
//                for (Operation op : svc.getOps()) {
//                    System.out.println(op);
//                }

                List<OpCallEdge> callEdges = svc.getCallEdges();
                if (VarValidator.empty(callEdges)) {
                    continue;
                }
                for (OpCallEdge callEdge : callEdges) {
                    if (!isApiClassAndApiFunc(callEdge.getInClassName(), callEdge.getInCompleteFuncName())) {
                        continue;
                    }
                    List<String> info = new ArrayList<>();
                    info.add(callEdge.getInSvcName());
                    info.add(callEdge.getInClassName());
                    info.add(callEdge.getInCompleteFuncName());
                    info.add(callEdge.getOutSvcName());
                    info.add(callEdge.getOutClassName());
                    info.add(callEdge.getOutCompleteFuncName());
                    info.add(callEdge.getWeight() + "");
                    infos.add(info);
                }
            }
        } else {
            logger.info("{} 目录下无微服务项目", msPath);
        }

        ExcelUtil.append2Excel(msName + "/" + msName + "_structure.xlsx", "data2", infos);

    }

    private static boolean isApiClassAndApiFunc(String className, String completeFuncName) {
        CtClass ctClass = ctClassManager.getCtClass(className);
        if (ctClass == null) { // 若获取不到className对应的CtClass，说明该类未被加载，则该类一定不是接口
            return false;
        }

        if (!SCUtil.isApiClass(ctClass)) { // 若不是接口类，则直接返回false
            return false;
        }

        CtMethod[] funcs = ctClass.getMethods();
        if (VarValidator.empty(funcs)) { // 若接口中无方法，则返回false
            return false;
        }

        for (CtMethod func : funcs) {
            if (CtClassManager.buildCompleteMethodName(func).equals(completeFuncName)) { // 匹配到对应的CtMethod，则返回该方法是否为操作
                return SCUtil.isApiFunc(func);
            }
        }

        // 若未匹配到，则说明completeFuncName不存在于className
        logger.info("类({})中无方法({})", className, completeFuncName);
        return false;
    }

    public static void main(String[] args) {
        String msPath = "/Users/lazyr/Work/projects/devops/test/data/dop";
        writeTable2(msPath);
    }
}
