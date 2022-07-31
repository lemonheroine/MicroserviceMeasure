package top.lazyr.microserviceName_structure.fieldcall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.microserviceName_structure.fieldcall.model.FieldCallEdge;
import top.lazyr.microserviceName_structure.fieldcall.model.FieldCallGraph;
import top.lazyr.microserviceName_structure.fieldcall.model.MethodNode;
import top.lazyr.microserviceName_structure.classinfo.Table125Writer;
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
public class Table47Writer {
    private static Logger logger = LoggerFactory.getLogger(Table125Writer.class);

    public static void writeTable47(String msPath) {
        List<String> svcPaths = FileUtil.getSubCatalogPaths(msPath);
        if (VarValidator.empty(svcPaths)) {
            logger.info("微服务项目目录 {} 下无服务项目目录", msPath);
            return;
        }

        for (String svcPath : svcPaths) {
            writeTable4OfSvc(svcPath, PathUtil.getCurrentCatalog(msPath));
            writeTable7OfSvc(svcPath, PathUtil.getCurrentCatalog(msPath));
        }
    }

    /**
     * ⼦表4记录微服务中所有⽅法到字段间的依赖关系：
     * 源类名（source_class）；源⽅法名（source_method）；⽬标类名（destination_class）；⽬标字段名（destination_field）；权重（weight)
     * @param svcPath
     * @param prefix
     */
    public static void writeTable4OfSvc(String svcPath, String prefix) {
        FieldCallParser parser = new FieldCallParser();
        FieldCallGraph graph = parser.parse(svcPath);
        String svcName = PathUtil.getCurrentCatalog(svcPath);

        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("源类名", "源方法签名", "目标类名", "目标字段名", "权重"));
        List<MethodNode> methodNodes = graph.getMethodNodes();

        if (VarValidator.notEmpty(methodNodes)) {
            for (MethodNode methodNode : methodNodes) {
                if (methodNode.isApi()) { // 只输出实体类中的方法
                    continue;
                }
                List<FieldCallEdge> callEdges = methodNode.getCallEdges();
                if (VarValidator.empty(callEdges)) {
                    continue;
                }
                for (FieldCallEdge callEdge : callEdges) {
                    List<String> info = new ArrayList<>();
                    info.add(methodNode.getBelongClassName());
                    info.add(methodNode.getCompleteMethodName());
                    info.add(callEdge.getOutClassName());
                    info.add(callEdge.getOutFieldName());
                    info.add(callEdge.getWeight() + "");
                    infos.add(info);
                }
            }
        }

        ExcelUtil.append2Excel(prefix + "/" + svcName + "_structure.xlsx", "data4", infos);
    }

    public static void writeTable7OfSvc(String svcPath, String prefix) {
        FieldCallParser parser = new FieldCallParser();
        FieldCallGraph graph = parser.parse(svcPath);
        String svcName = PathUtil.getCurrentCatalog(svcPath);

        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("源接口名", "源操作签名", "目标类名", "目标字段名", "权重"));
        List<MethodNode> methodNodes = graph.getMethodNodes();

        if (VarValidator.notEmpty(methodNodes)) {
            for (MethodNode methodNode : methodNodes) {
                if (!(methodNode.isApi() && methodNode.isApiFunc())) { // 只输出接口类中的操作
                    continue;
                }
                List<FieldCallEdge> callEdges = methodNode.getCallEdges();
                if (VarValidator.empty(callEdges)) {
                    continue;
                }
                for (FieldCallEdge callEdge : callEdges) {
                    List<String> info = new ArrayList<>();
                    info.add(methodNode.getBelongClassName());
                    info.add(methodNode.getCompleteMethodName());
                    info.add(callEdge.getOutClassName());
                    info.add(callEdge.getOutFieldName());
                    info.add(callEdge.getWeight() + "");
                    infos.add(info);
                }
            }
        }

        ExcelUtil.append2Excel(prefix + "/" + svcName + "_structure.xlsx", "data7", infos);
    }
}
