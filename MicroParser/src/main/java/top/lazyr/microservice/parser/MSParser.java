package top.lazyr.microservice.parser;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.manager.CtClassManager;
import top.lazyr.microservice.graph.svc.InternalGraph;
import top.lazyr.microservice.model.Api;
import top.lazyr.microservice.model.Feign;
import top.lazyr.util.ClsUtil;
import top.lazyr.util.SCUtil;
import top.lazyr.microservice.graph.ms.Microservices;
import top.lazyr.microservice.graph.ms.Service;
import top.lazyr.util.FileUtil;
import top.lazyr.util.PathUtil;

import java.util.*;

/**
 * @author lazyr
 * @created 2022/4/22
 */
public class MSParser {
    private static Logger logger = LoggerFactory.getLogger(InternalGraphParser.class);
    private CtClassManager ctClassManager;
    private Map<String, Feign> feignFileName2Feign;
    private Microservices microservices;

    public MSParser() {
        this.ctClassManager = CtClassManager.getCtClassManager();
        this.feignFileName2Feign = new HashMap<>();
    }

    public Microservices parse(String msPath) {
        // 1、初始化项目中所有Feign文件
        initFeignInfo(msPath);
        // 2、初始化Microservices
        this.microservices = new Microservices();
        // 3、初始化Service中的基础信息
        List<String> svcPaths = FileUtil.getSubCatalogPaths(msPath);
        for (String svcPath : svcPaths) {
            initSvc(svcPath);
        }
        // 4、初始化调用信息
        for (String svcPath : svcPaths) {
            String svcName = PathUtil.getCurrentCatalog(svcPath);
            initLogicCall(svcPath, microservices.findSvcByName(svcName));
        }

        return microservices;
    }

    private void initLogicCall(String svcPath, Service svc) {
        List<CtClass> ctClasses = ctClassManager.extractCtClass(svcPath);
        for (CtClass ctClass : ctClasses) {
            try {
                ctClass.instrument(new SvcCallBuilder(ctClass.getName(), svc));
            } catch (CannotCompileException e) {
                logger.error("build depend edge failed: err = " + e.getMessage());
            }
        }
    }

    private void initSvc(String svcPath) {
        // 1、初始化服务名
        String svcName = PathUtil.getCurrentCatalog(svcPath);
        // 2、初始化内部依赖关系图
        InternalGraphParser internalGraphParser = new InternalGraphParser(feignFileName2Feign.keySet());
        InternalGraph internalGraph = internalGraphParser.parse(svcPath);
        // 3、初始Service
        Service service = new Service(svcName, internalGraph);
        // 4、初始化Service中的Api信息
        List<CtClass> ctClasses = ctClassManager.extractCtClass(svcPath);
        initApiOfSvc(service, ctClasses);
        // 5、添加Svc
        microservices.addService(service);
    }

    private void initApiOfSvc(Service service, List<CtClass> ctClasses) {
        for (CtClass ctClass : ctClasses) {
            if (SCUtil.isApiClass(ctClass)) {
                String prefix = SCUtil.extractApiPrefix(ctClass);
                CtMethod[] ctMethods = ctClass.getMethods();
                for (CtMethod ctMethod : ctMethods) {
                    if (SCUtil.isApiFunc(ctMethod)) {
                        Set<Api> apis = SCUtil.extractApiFromCtMethod(ctMethod, prefix);
                        service.addApi(apis, ClsUtil.extractFileName(ctClass.getName()));
                    }
                }
            }
        }
    }

    private void initFeignInfo(String msPath) {
        List<CtClass> ctClasses = ctClassManager.extractCtClass(msPath);
        for (CtClass ctClass : ctClasses) {
            if (SCUtil.isFeign(ctClass)) {
                feignFileName2Feign.put(ClsUtil.extractFileName(ctClass.getName()), buildFeign(ctClass));
            }
        }
//        System.out.println(feignFileName2Feign.size());
//        for (String feignName : feignFileName2Feign.keySet()) {
//            Printer.printTitle(feignName);
//            System.out.println(feignFileName2Feign.get(feignName));
//        }
    }

    /**
     * 传进来的一定是Feign类
     * @param ctClass
     * @return
     */
    private Feign buildFeign(CtClass ctClass) {
        String feignFileName = ctClass.getName();
        String svcName = SCUtil.extractSvcName(ctClass);
        Feign feign = new Feign(feignFileName, svcName);

        // 一定以 "/" 开头
        String prefix = SCUtil.extractFeignPrefix(ctClass);
        CtMethod[] ctMethods = ctClass.getMethods();
        for (CtMethod ctMethod : ctMethods) {
            if (SCUtil.isApiFunc(ctMethod)) {
                Set<Api> apis = SCUtil.extractApiFromCtMethod(ctMethod, prefix);
//                String longFuncName = ctClass.getName() + "." + TypeManager.extractMethodName(ctMethod.getLongName());
                String longFuncName = ctClass.getName() + "." +  ctMethod.getName() + ctMethod.getSignature();
                feign.putApi(longFuncName, apis);
            }
        }

        return feign;
    }

    private class SvcCallBuilder extends ExprEditor {
        /* 会将内部类转换为所在文件名 */
        private String inFileName;
        private Service inSvc;
        /**
         * 使用该类构建依赖关系的inClassName都为项目内的类，则默认FileNode已创建
         * - 若未创建，则记录为error
         * - 若sourceClassName为内部类的名字且外部类Node未创建，则记录为error
         * - 若sourceClassName则将其转换为外部类的名字
         * @param inClassName
         */
        public SvcCallBuilder(String inClassName, Service inSvc) {
            this.inFileName = ClsUtil.extractFileName(inClassName);
            this.inSvc = inSvc;
        }

        /**
         * 当 类A调用类B的方法 时触发
         * @param m
         * @throws CannotCompileException
         */
        @SneakyThrows
        @Override
        public void edit(MethodCall m) throws CannotCompileException {

            // 方法所在的类名.方法名(方法参数列表) 而不是方法的父类
            String longFuncName = m.getClassName() + "." + m.getMethodName() + m.getSignature();
            buildCall(m.getClassName(), longFuncName);
        }

        public void buildCall(String outClassName, String longFuncName) {
            String feignName = ClsUtil.extractFileName(outClassName);

            if (!feignFileName2Feign.containsKey(feignName)) { // 若调用的文件为Feign文件
                return;
            }
            // 1、获取feignName对应的Feign
            Feign feign = feignFileName2Feign.get(feignName);
            // 2、获取逻辑调用的微服务名
            String outSvcName = feign.getSvcName();
            // 3、获取调用Feign文件中method对应的Api信息
            Map<String, Set<Api>> func2Api = feign.getFunc2Api();
            if (!func2Api.containsKey(longFuncName)) { // 若调用的方法不是api方法，则结束
                logger.error(longFuncName + " is not a api func.");
                return;
            }
            // 4、获取对应的调用Api
            Set<Api> apis = func2Api.get(longFuncName);

            // 5、获取outSvc中api对应的Api文件
            String outFileName = microservices.getOutFileByApiAndSvc(outSvcName, apis);
            if (outFileName == null) {
                logger.error(apis + " of " + outSvcName + " is not existed.");
                return;
            }

            // 6、添加服务间的逻辑调用
            microservices.addCall(inSvc, inFileName, microservices.findSvcByName(outSvcName), outFileName);

        }

    }

}
