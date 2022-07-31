package top.lazyr.microservice.writer;

import top.lazyr.constant.Printer;
import top.lazyr.microservice.graph.ms.LogicEdge;
import top.lazyr.microservice.graph.ms.Service;
import top.lazyr.microservice.model.Api;

import java.util.List;
import java.util.Map;

/**
 * @author lazyr
 * @created 2022/4/23
 */
public class SvcWriter {
    public static void printSvcInfo(Service service) {
        String svcName = service.getName();
        Map<Api, String> api2FileName = service.getApi2FileName();
        List<LogicEdge> logicCallEdges = service.getLogicCallEdges();
        int afferentWeight = service.getAfferentWeight();
        Printer.printTitle(svcName);
        Printer.printTitle("api");
        for (Api api : api2FileName.keySet()) {
            System.out.println(api + " => " + api2FileName.get(api));
        }
        Printer.printTitle("logic call");
        for (LogicEdge logicCallEdge : logicCallEdges) {
            System.out.println(logicCallEdge);
        }
        Printer.printTitle("afferentWeight: " + afferentWeight);
    }
}
