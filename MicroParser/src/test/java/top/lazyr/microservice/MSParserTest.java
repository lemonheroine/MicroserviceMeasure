package top.lazyr.microservice;

import org.junit.Test;
import top.lazyr.microservice.graph.ms.Microservices;
import top.lazyr.microservice.graph.ms.Service;
import top.lazyr.microservice.parser.MSParser;
import top.lazyr.microservice.writer.SvcWriter;

import java.util.List;

public class MSParserTest {

    @Test
    public void parse() {
//        String msPath = "/Users/lazyr/Work/projects/devops/test/data/dop";
        String msPath = "/Users/lazyr/Work/projects/devops/test/data/Nacos";
//        String msPath = "/Users/lazyr/Work/projects/devops/test/data/Nacos的副本";
        MSParser msParser = new MSParser();
        Microservices microservices = msParser.parse(msPath);
        List<Service> services = microservices.getServices();
        for (Service service : services) {
            SvcWriter.printSvcInfo(service);
//            InternalGraph graph = service.getGraph();
//            GraphWriter.printInfo(graph);
        }

    }
}
