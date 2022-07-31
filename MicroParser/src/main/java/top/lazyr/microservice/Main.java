package top.lazyr.microservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.microservice.graph.ms.LogicEdge;
import top.lazyr.microservice.graph.ms.Microservices;
import top.lazyr.microservice.graph.svc.Edge;
import top.lazyr.microservice.graph.svc.FileNode;
import top.lazyr.microservice.graph.ms.Service;
import top.lazyr.microservice.graph.svc.InternalGraph;
import top.lazyr.microservice.parser.MSParser;
import top.lazyr.util.ExcelUtil;
import top.lazyr.util.FileUtil;

import java.util.*;

/**
 * @author lazyr
 * @created 2021/11/21
 */
public class Main {
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws ClassNotFoundException {
        // 这里填写编译后项目的绝对路径
//        String absoluteMSPath = "/Users/lazyr/Work/projects/script/Abbreviation/target";
        String absoluteMSPath = "/Users/lazyr/Work/projects/devops/test/data/dop";
        String msName = "nacos";
        printInternalCall(absoluteMSPath, msName); // 打印每个服务内部纯方法调用关系
//        printInternalAllCall(absoluteMSPath, msName); // 打印每个服务内部所有调用关系
//        printMSCall(absoluteMSPath, msName); // 打印微服务间调用关系
//        printFileInfo(absoluteMSPath, msName); // 打印每个服务内的文件信息
//        printApi(absoluteMSPath, msName); // 打印每个服务的api信息到控制台
    }

    private static void printApi(String msPath, String msName) {
        MSParser msParser = new MSParser();
        Microservices ms = msParser.parse(msPath);
//        List<Service> services = ms.getServices();
//        for (Service service : services) {
//            Printer.printTitle(service.getName());
//            Map<Api, String> api2FileName = service.getApi2FileName();
//            for (Api api : api2FileName.keySet()) {
//                System.out.println(api + " => " + api2FileName.get(api));
//            }
//        }

    }

    /**
     * 打印每个服务内部纯方法调用关系
     * 输出文件名: [msName]服务内部纯方法调用依赖关系.xlsx
     * @param msPath
     * @param msName
     */
    private static void printInternalCall(String msPath, String msName) {
        FileUtil.deleteFile("src/main/resources/" + msName + "服务内部纯方法调用依赖关系.xlsx");
        MSParser msParser = new MSParser();
        Microservices ms = msParser.parse(msPath);
        List<Service> services = ms.getServices();
        for (Service service : services) {
            List<List<String>> infos = new ArrayList<>();
            infos.add(Arrays.asList("源文件名", "被依赖文件名", "权重"));
            String svcName = service.getName();
            InternalGraph graph = service.getGraph();
            List<FileNode> fileNodes = graph.getAllFileNodes();
            for (FileNode fileNode : fileNodes) {
                List<Edge> callEdges = fileNode.getCallEdges();
                for (Edge callEdge : callEdges) {
                    int methodCallWeight = callEdge.getWeightOfType(Edge.METHOD_CALL);
                    if (methodCallWeight != 0) {
                        infos.add(Arrays.asList(callEdge.getInFile(), callEdge.getOutFile(), methodCallWeight + ""));
                    }
                }
            }
            ExcelUtil.append2Excel(msName + "服务内部纯方法调用依赖关系.xlsx", svcName, infos);
        }
    }

    /**
     * 打印每个服务内部所有调用关系
     * 输出文件名: [msName]服务内部全部调用依赖关系.xlsx
     * @param msPath
     * @param msName
     */
    private static void printInternalAllCall(String msPath, String msName) {
        FileUtil.deleteFile("src/main/resources/" + msName + "服务内部全部调用依赖关系.xlsx");
        MSParser msParser = new MSParser();
        Microservices ms = msParser.parse(msPath);
        List<Service> services = ms.getServices();
        for (Service service : services) {
            List<List<String>> infos = new ArrayList<>();
            infos.add(Arrays.asList("源文件名", "被依赖文件名", "权重信息", "总权重"));
            String svcName = service.getName();
            InternalGraph graph = service.getGraph();
            List<FileNode> fileNodes = graph.getAllFileNodes();
            for (FileNode fileNode : fileNodes) {
                List<Edge> callEdges = fileNode.getCallEdges();
                for (Edge callEdge : callEdges) {
                    Map<String, Integer> callWeight = callEdge.getCallWeight();
                    StringBuilder allCallWeight = new StringBuilder();
                    for (String callType : callWeight.keySet()) {
                        allCallWeight.append(callType + " : " + callWeight.get(callType) + "\n");
                    }

                    infos.add(Arrays.asList(callEdge.getInFile(), callEdge.getOutFile(), allCallWeight.toString(), callEdge.getTotalWeight() + ""));
                }
            }
            ExcelUtil.append2Excel(msName + "服务内部全部调用依赖关系.xlsx", svcName, infos);
        }
    }

    /**
     * 打印微服务间调用关系
     * 输出文件名: [msName]服务间调用依赖关系.xlsx
     * @param msPath
     * @param msName
     */
    public static void printMSCall(String msPath, String msName) {
        FileUtil.deleteFile("src/main/resources/" + msName + "服务间调用依赖关系.xlsx");
        List<List<String>> infos = new ArrayList<>();
        infos.add(Arrays.asList("源服务名", "源文件名", "被依赖服务名", "被依赖文件名", "权重"));
        MSParser msParser = new MSParser();
        Microservices ms = msParser.parse(msPath);
        List<Service> services = ms.getServices();
        for (Service service : services) {
            List<LogicEdge> logicCallEdges = service.getLogicCallEdges();
            if (logicCallEdges.size() == 0) {
                continue;
            }
            for (LogicEdge logicCallEdge : logicCallEdges) {
                infos.add(Arrays.asList(logicCallEdge.getInSvcName(),
                                        logicCallEdge.getInFileName(),
                                        logicCallEdge.getOutSvcName(),
                                        logicCallEdge.getOutFileName(),
                                        logicCallEdge.getWeight() + ""));
            }
        }
        ExcelUtil.write2Excel(msName + "服务间调用依赖关系.xlsx", "data",infos);

    }

    /**
     * 打印每个服务内的文件信息
     * 输出文件名: [msName]服务内部文件信息.xls
     * @param msPath
     * @param msName
     */
    public static void printFileInfo(String msPath, String msName) {
        FileUtil.deleteFile("src/main/resources/" + msName + "服务内部文件信息.xlsx");
        MSParser msParser = new MSParser();
        Microservices ms = msParser.parse(msPath);
        List<Service> services = ms.getServices();
        for (Service service : services) {
            List<List<String>> infos = new ArrayList<>();
            infos.add(Arrays.asList("文件名", "类型", "来源"));
            String svcName = service.getName();
            InternalGraph graph = service.getGraph();
            List<FileNode> fileNodes = graph.getAllFileNodes();
            for (FileNode fileNode : fileNodes) {
                infos.add(Arrays.asList(fileNode.getName(), fileNode.getType(), fileNode.getFrom()));
            }
            ExcelUtil.append2Excel(msName + "服务内部文件信息.xlsx", svcName, infos);
        }
    }


}
