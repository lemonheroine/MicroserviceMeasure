package top.lazyr.microserviceName_structure.methodcall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.microserviceName_structure.classinfo.Table125Writer;
import top.lazyr.microserviceName_structure.methodcall.model.MethodCallEdge;
import top.lazyr.microserviceName_structure.methodcall.model.MethodCallGraph;
import top.lazyr.microserviceName_structure.methodcall.model.MethodNode;
import top.lazyr.util.ExcelUtil;
import top.lazyr.util.FileUtil;
import top.lazyr.util.PathUtil;
import top.lazyr.validator.VarValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class Table368Writer {
    private static Logger logger = LoggerFactory.getLogger(Table125Writer.class);

    public static void writeTable3689(String msPath) {
        List<String> svcPaths = FileUtil.getSubCatalogPaths(msPath);
        if (VarValidator.empty(svcPaths)) {
            logger.info("微服务项目目录 {} 下无服务项目目录", msPath);
            return;
        }
        for (String svcPath : svcPaths) {
            writeTable3OfSvc(svcPath, PathUtil.getCurrentCatalog(msPath));
            writeTable6OfSvc(svcPath, PathUtil.getCurrentCatalog(msPath));
            writeTable8OfSvc(svcPath, PathUtil.getCurrentCatalog(msPath));
            writeTable9OfSvc(svcPath, PathUtil.getCurrentCatalog(msPath));
        }
    }

    /**
     * ⼦表3记录微服务中所有⽅法层级的依赖关系：
     * 源类名（source_class）；源⽅法签名（source_method）；⽬标类名（destination_class）；⽬标⽅法签名（destination_method）；权重（weight)
     * @param svcPath
     * @param prefix
     */
    public static void writeTable3OfSvc(String svcPath, String prefix) {
        MethodCallParser parser = new MethodCallParser();
        MethodCallGraph graph = parser.parse(svcPath);
        String svcName = PathUtil.getCurrentCatalog(svcPath);

        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("源类名", "源方法签名", "目标类名", "目标方法签名", "权重"));
        List<MethodNode> systemMethodNodes = graph.getSystemMethodNode();

        if (VarValidator.notEmpty(systemMethodNodes)) {
            for (MethodNode inMethodNode : systemMethodNodes) {
                if (inMethodNode.isApi()) { // 只输出实体类中的方法
                    continue;
                }
                List<MethodCallEdge> callEdges = inMethodNode.getCallEdges();
                if (VarValidator.empty(callEdges)) {
                    continue;
                }
                for (MethodCallEdge callEdge : callEdges) {
                    String outClassName = callEdge.getOutClassName();
                    String outCompleteMethodName = callEdge.getOutCompleteMethodName();
                    MethodNode outMethodNode = graph.findMethodNodeById(outClassName + "." + outCompleteMethodName);
                    if (outMethodNode == null || outMethodNode.isApiClass()) { // 只输出 实体类 -> 实体类 中的调用关系
                        continue;
                    }

//                    if (outClassName.contains("CustomAuthenticationSuccessHandler") && outCompleteMethodName.contains("insertLoginLog")) {
//                        System.out.println("id -> " + outCompleteMethodName);
//                    }

                    List<String> info = new ArrayList<>();
                    info.add(inMethodNode.getBelongClassName());
                    info.add(inMethodNode.getCompleteMethodName());
                    info.add(callEdge.getOutClassName());
                    info.add(callEdge.getOutCompleteMethodName());
                    info.add(callEdge.getWeight() + "");
                    infos.add(info);
                }
            }
        }
        ExcelUtil.append2Excel(prefix + "/" + svcName + "_structure.xlsx", "data3", infos);
    }


    public static void writeTable6OfSvc(String svcPath, String prefix) {
        MethodCallParser parser = new MethodCallParser();
        MethodCallGraph graph = parser.parse(svcPath);
        String svcName = PathUtil.getCurrentCatalog(svcPath);

        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("源接口名", "源操作签名", "目标类名", "目标方法签名", "权重"));
        List<MethodNode> systemMethodNodes = graph.getSystemMethodNode();

        if (VarValidator.notEmpty(systemMethodNodes)) {
            for (MethodNode inMethodNode : systemMethodNodes) {
                if (!(inMethodNode.isApi() && inMethodNode.isApiFunc())) { // 只输出接口类中的操作
                    continue;
                }
                List<MethodCallEdge> callEdges = inMethodNode.getCallEdges();
                if (VarValidator.empty(callEdges)) {
                    continue;
                }
                for (MethodCallEdge callEdge : callEdges) {
                    String outClassName = callEdge.getOutClassName();
                    String outCompleteMethodName = callEdge.getOutCompleteMethodName();
                    MethodNode outMethodNode = graph.findMethodNodeById(outClassName + "." + outCompleteMethodName);
                    if (outMethodNode == null || outMethodNode.isApiClass()) { // 只输出 接口 -> 实体类 中的调用关系
                        continue;
                    }

                    List<String> info = new ArrayList<>();
                    info.add(inMethodNode.getBelongClassName());
                    info.add(inMethodNode.getCompleteMethodName());
                    info.add(callEdge.getOutClassName());
                    info.add(callEdge.getOutCompleteMethodName());
                    info.add(callEdge.getWeight() + "");
                    infos.add(info);
                }
            }
        }
        ExcelUtil.append2Excel(prefix + "/" + svcName + "_structure.xlsx", "data6", infos);
    }

    /**
     * ⼦表8记录微服务中所有类到接⼝间的依赖关系：
     * 源类名（source_class）；源⽅法签名（source_method）；⽬标接⼝名（destination_interface）；⽬标操作签名（destination_operation）；权重（weight)
     * @param svcPath
     * @param prefix
     */
    public static void writeTable8OfSvc(String svcPath, String prefix) {
        MethodCallParser parser = new MethodCallParser();
        MethodCallGraph graph = parser.parse(svcPath);
        String svcName = PathUtil.getCurrentCatalog(svcPath);

        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("源类名", "源方法签名", "目标接口", "目标操作签名", "权重"));
        List<MethodNode> systemMethodNodes = graph.getSystemMethodNode();

        if (VarValidator.notEmpty(systemMethodNodes)) {
            for (MethodNode inMethodNode : systemMethodNodes) {
                if (inMethodNode.isApi()) { // 只输出实体类中的方法
                    continue;
                }
                List<MethodCallEdge> callEdges = inMethodNode.getCallEdges();
                if (VarValidator.empty(callEdges)) {
                    continue;
                }
                for (MethodCallEdge callEdge : callEdges) {
                    String outClassName = callEdge.getOutClassName();
                    String outCompleteMethodName = callEdge.getOutCompleteMethodName();
                    MethodNode outMethodNode = graph.findMethodNodeById(outClassName + "." + outCompleteMethodName);
                    if (outMethodNode == null || !outMethodNode.isApiClass() || !outMethodNode.isApiFunc()) { // 只输出 实体类 -> 接口类 中的调用关系
                        continue;
                    }

                    List<String> info = new ArrayList<>();
                    info.add(inMethodNode.getBelongClassName());
                    info.add(inMethodNode.getCompleteMethodName());
                    info.add(callEdge.getOutClassName());
                    info.add(callEdge.getOutCompleteMethodName());
                    info.add(callEdge.getWeight() + "");
                    infos.add(info);
                }
            }
        }
        ExcelUtil.append2Excel(prefix + "/" + svcName + "_structure.xlsx", "data8", infos);
    }


    /**
     * ⼦表9记录微服务中所有接⼝到接⼝间的依赖关系：
     * 源接⼝名（source_interface）；源操作签名（source_operation）；⽬标接⼝名（destination_interface）；⽬标操作名（destination_operation）；权重（weight)
     * @param svcPath
     * @param prefix
     */
    public static void writeTable9OfSvc(String svcPath, String prefix) {
        MethodCallParser parser = new MethodCallParser();
        MethodCallGraph graph = parser.parse(svcPath);
        String svcName = PathUtil.getCurrentCatalog(svcPath);

        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("源接口名", "源操作签名", "目标接口名", "目标操作签名", "权重"));
        List<MethodNode> systemMethodNodes = graph.getSystemMethodNode();

        if (VarValidator.notEmpty(systemMethodNodes)) {
            for (MethodNode inMethodNode : systemMethodNodes) {
                if (!inMethodNode.isApi() || !inMethodNode.isApiFunc()) { // 只输出接口中的操作
                    continue;
                }
                List<MethodCallEdge> callEdges = inMethodNode.getCallEdges();
                if (VarValidator.empty(callEdges)) {
                    continue;
                }
                for (MethodCallEdge callEdge : callEdges) {
                    String outClassName = callEdge.getOutClassName();
                    String outCompleteMethodName = callEdge.getOutCompleteMethodName();
                    MethodNode outMethodNode = graph.findMethodNodeById(outClassName + "." + outCompleteMethodName);
                    if (outMethodNode == null || !outMethodNode.isApiClass() || !outMethodNode.isApiFunc()) { // 只输出 接口类 -> 接口类 中的调用关系
                        continue;
                    }

                    List<String> info = new ArrayList<>();
                    info.add(inMethodNode.getBelongClassName());
                    info.add(inMethodNode.getCompleteMethodName());
                    info.add(callEdge.getOutClassName());
                    info.add(callEdge.getOutCompleteMethodName());
                    info.add(callEdge.getWeight() + "");
                    infos.add(info);
                }
            }
        }
        ExcelUtil.append2Excel(prefix + "/" + svcName + "_structure.xlsx", "data9", infos);
    }
}
