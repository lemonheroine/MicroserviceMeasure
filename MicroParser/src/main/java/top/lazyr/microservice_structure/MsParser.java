package top.lazyr.microservice_structure;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.constant.Printer;
import top.lazyr.manager.CtClassManager;
import top.lazyr.microservice_structure.model.Agency;
import top.lazyr.microservice_structure.model.MS;
import top.lazyr.microservice_structure.model.Operation;
import top.lazyr.microservice_structure.model.Svc;
import top.lazyr.util.*;
import top.lazyr.validator.VarValidator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class MsParser {
    private static Logger logger = LoggerFactory.getLogger(MsParser.class);
    private CtClassManager ctClassManager;
    /**
     * a.b.ClassName.func(java.lang.String,java.lang.Integer) -> Agency
     */
    private Map<String, Agency> agencyMap;
    private MS ms;

    public MsParser() {
        this.ctClassManager = CtClassManager.getCtClassManager();
        this.agencyMap = new HashMap<>();
    }

    public MS parse(String msPath) {
        // 1、初始化项目中所有Feign文件
        initAgencies(msPath);
        Printer.printMap(agencyMap);
        // 2、初始化MS
        this.ms = new MS();
        // 3、初始化Svc中的基础信息
        List<String> svcPaths = FileUtil.getSubCatalogPaths(msPath);
        for (String svcPath : svcPaths) {
            initSvc(svcPath);
        }
        // 4、初始化调用信息
        for (String svcPath : svcPaths) {
            String svcName = PathUtil.getCurrentCatalog(svcPath);
            initOpCall(svcPath, ms.findSvcByName(svcName));
        }
        return ms;
    }

    private void initOpCall(String svcPath, Svc svc) {
        List<CtClass> ctClasses = ctClassManager.extractCtClass(svcPath);
        for (CtClass ctClass : ctClasses) {
            CtMethod[] ctMethods = CtClassManager.getAllCtMethods(ctClass);
            if (VarValidator.empty(ctMethods)) {
                continue;
            }
            for (CtMethod ctMethod : ctMethods) {
                try {
                    ctMethod.instrument(new OpCallBuilder(svc, ctClass.getName(), ctMethod));
                } catch (CannotCompileException e) {
                    logger.error("build depend edge failed: err = " + e.getMessage());
                }
            }
        }
    }

    private void initSvc(String svcPath) {
        // 1、初始化服务名
        String svcName = PathUtil.getCurrentCatalog(svcPath);
        // 2、初始Svc
        Svc svc = new Svc(svcName);
        // 3、初始化Service中的Api信息
        List<CtClass> ctClasses = ctClassManager.extractCtClass(svcPath);
        initOpOfSvc(svc, ctClasses);
        // 4、添加Svc
        ms.addSvc(svc);
    }

    private void initOpOfSvc(Svc svc, List<CtClass> ctClasses) {
        for (CtClass ctClass : ctClasses) {
            if (SCUtil.isApiClass(ctClass)) {
                String prefix = SCUtil.extractApiPrefix(ctClass);
                CtMethod[] ctMethods = ctClass.getMethods();
                for (CtMethod ctMethod : ctMethods) {
                    if (SCUtil.isApiFunc(ctMethod)) {
                        Set<Operation> ops = SCUtil.extractOpFromCtMethod(ctClass.getName(), ctMethod, prefix);
                        svc.addOps(ops);
                    }
                }
            }
        }
    }

    private void initAgencies(String msPath) {
        List<CtClass> ctClasses = ctClassManager.extractCtClass(msPath);
        if (VarValidator.empty(ctClasses)) {
            logger.info("{} 下无class文件", msPath);
            return;
        }
        for (CtClass ctClass : ctClasses) {
            if (SCUtil.isFeign(ctClass)) { // 为Feign类
                CtMethod[] ctMethods = CtClassManager.getAllCtMethods(ctClass);
                if (VarValidator.empty(ctMethods)) {
                    continue;
                }
                for (CtMethod ctMethod : ctMethods) {
                    if (SCUtil.isApiFunc(ctMethod)) { // 为封装的方法
                        agencyMap.put(ctClass.getName() + "." + CtClassManager.buildCompleteMethodName(ctMethod),
                                buildAgency(ctClass, ctMethod));
                    }
                }

            }
        }
    }

    private Agency buildAgency(CtClass ctClass, CtMethod ctMethod) {
        String svcName = SCUtil.extractSvcName(ctClass);
        String agencyClassName = ctClass.getName();
        String completeFuncName = CtClassManager.buildCompleteMethodName(ctMethod);
        Agency agency = new Agency(svcName, agencyClassName, completeFuncName);

        String prefix = SCUtil.extractFeignPrefix(ctClass);
        Set<Operation> ops = SCUtil.extractOpFromCtMethod(agencyClassName, ctMethod, prefix);
        agency.setOperations(ops);
        return agency;
    }


    private class OpCallBuilder extends ExprEditor {
        private Svc inSvc;
        private String inClassName;
        private String inCompleteFuncName;
        /**
         * 使用该类构建依赖关系的inClassName都为项目内的类，则默认FileNode已创建
         * - 若未创建，则记录为error
         * - 若sourceClassName为内部类的名字且外部类Node未创建，则记录为error
         * - 若sourceClassName则将其转换为外部类的名字
         * @param inClassName
         */
        public OpCallBuilder(Svc inSvc, String inClassName, CtMethod ctMethod) {
            this.inClassName = ClsUtil.extractFileName(inClassName);
            this.inSvc = inSvc;
            this.inCompleteFuncName = CtClassManager.buildCompleteMethodName(ctMethod);
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
            String paramStr = Descriptor.toString(m.getSignature());
            paramStr = paramStr.substring(1, paramStr.length() - 1);
            String[] paramSplit = paramStr.split(",");
            StringBuilder completeFuncName = new StringBuilder();
            completeFuncName.append(m.getMethodName() + "(");
            if (VarValidator.notEmpty(paramSplit)) {
                for (int i = 0; i < paramSplit.length; i++) {
                    completeFuncName.append(TypeUtil.unbox(paramSplit[i]));
                    if (i != paramSplit.length - 1) {
                        completeFuncName.append(",");
                    }
                }
            }
            completeFuncName.append(")");
            buildCall(m.getClassName(), completeFuncName.toString());
        }

        public void buildCall(String outClassName, String outCompleteFuncName) {
//            String feignName = ClsUtil.extractFileName(outClassName);
            String agencyKey = outClassName + "." + outCompleteFuncName;
            if (!agencyMap.containsKey(agencyKey)) { // 若调用的文件不是Feign文件
                return;
            }
            // 1、获取feignName对应的Feign
            Agency agency = agencyMap.get(agencyKey);
            // 2、获取逻辑调用的微服务名
            String outSvcName = agency.getSvcName();
            // 3、获取调用Feign文件中method对应的Api信息
//            Map<String, Set<Api>> func2Api = agency.getOperationMap();
            if (!agency.getId().equals(agencyKey)) { // 若调用的方法不是api方法，则结束
                logger.info(agencyKey + " is not a api func.");
                return;
            }

            // 4、获取outSvc中agency对应的Api文件
            Operation outOp = ms.getOutOpByApiAndSvc(outSvcName, agency);
            if (outOp == null) {
                logger.error("func({}) of svc({}) is not existed.", agency.getId(), outSvcName);
                return;
            }

            // 5、添加服务间的逻辑调用

            ms.addCall(inSvc, inClassName, inCompleteFuncName, ms.findSvcByName(outSvcName), outOp.getClassName(), outOp.getCompleteFuncName());
        }
    }
}
