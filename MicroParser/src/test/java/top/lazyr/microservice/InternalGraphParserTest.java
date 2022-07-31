package top.lazyr.microservice;

import javassist.CtClass;
import org.junit.Test;
import top.lazyr.util.SCUtil;
import top.lazyr.microservice.graph.svc.InternalGraph;
import top.lazyr.manager.CtClassManager;
import top.lazyr.microservice.parser.InternalGraphParser;
import top.lazyr.microservice.writer.GraphWriter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InternalGraphParserTest {

    @Test
    public void parse() {

        String msPath = "/Users/lazyr/Work/projects/devops/test/data/dop";
        CtClassManager ctClassManager = CtClassManager.getCtClassManager();
        List<CtClass> ctClasses = ctClassManager.extractCtClass(msPath);
        Set<String> feignFileNames = extractFeignFileNames(ctClasses);
        String svcPath = "/Users/lazyr/Work/projects/devops/test/data/dop/application-server";
        InternalGraphParser internalGraphParser = new InternalGraphParser(feignFileNames);
        InternalGraph internalGraph = internalGraphParser.parse(svcPath);
        GraphWriter.printInfo(internalGraph);
    }


    private Set<String> extractFeignFileNames(List<CtClass> ctClasses) {
        Set<String> feignFileNames = new HashSet<>();
        for (CtClass ctClass : ctClasses) {
            if (SCUtil.isFeign(ctClass)) {
                feignFileNames.add(ctClass.getName());
            }
        }
        return feignFileNames;
    }
}
