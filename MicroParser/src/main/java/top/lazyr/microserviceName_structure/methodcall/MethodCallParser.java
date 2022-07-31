package top.lazyr.microserviceName_structure.methodcall;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.constant.Printer;
import top.lazyr.manager.ClassInfoManager;
import top.lazyr.manager.CtClassManager;
import top.lazyr.microserviceName_structure.methodcall.model.MethodCallGraph;
import top.lazyr.microserviceName_structure.methodcall.model.MethodNode;
import top.lazyr.util.SCUtil;
import top.lazyr.util.TypeUtil;
import top.lazyr.validator.VarValidator;

import java.util.List;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class MethodCallParser {
    private static Logger logger = LoggerFactory.getLogger(MethodCallParser.class);
    private CtClassManager ctClassManager;
    private MethodCallGraph graph;

    public MethodCallParser() {
        this.ctClassManager = CtClassManager.getCtClassManager();
        this.graph = new MethodCallGraph();
    }

    public MethodCallGraph parse(String svcPath) {
        List<CtClass> ctClasses = ctClassManager.extractCtClass(svcPath);
        if (VarValidator.empty(ctClasses)) {
            logger.info(svcPath + " 下无class文件");
            return graph;
        }
        extractSystemMethodNodes(ctClasses);
        buildMethodCall(ctClasses);
        return graph;
    }

    private void extractSystemMethodNodes(List<CtClass> ctClasses) {
        if (VarValidator.empty(ctClasses)) {
            logger.info("无类文件");
            return;
        }
        for (CtClass ctClass : ctClasses) {
            boolean isApiClass = SCUtil.isApiClass(ctClass);
            boolean isSystem = true;
            CtMethod[] ctMethods = CtClassManager.getAllCtMethods(ctClass);
            if (VarValidator.empty(ctMethods)) {
                continue;
            }
            for (CtMethod ctMethod : ctMethods) {
                boolean isApiFunc = SCUtil.isApiFunc(ctMethod);

                MethodNode methodNode = new MethodNode(ctClass.getName(), ctMethod, isSystem, isApiClass, isApiFunc);
                this.graph.addMethodNode(methodNode);
            }
        }
    }

    private void buildMethodCall(List<CtClass> ctClasses) {
        for (CtClass ctClass : ctClasses) {
            CtMethod[] ctMethods = ClassInfoManager.getAllCtMethods(ctClass);
            if (VarValidator.empty(ctMethods)) {
                continue;
            }
            for (CtMethod ctMethod : ctMethods) {
                MethodNode methodNode = this.graph.findMethodNodeById(ctClass.getName() + "." + CtClassManager.buildCompleteMethodName(ctMethod));
                try {
                    ctMethod.instrument(new MethodCallBuilder(methodNode));
                } catch (CannotCompileException e) {
                    logger.error("build depend edge failed: err = " + e.getMessage());
                }
            }
        }
    }

    private class MethodCallBuilder extends ExprEditor {
        private MethodNode inMethodNode;

        public MethodCallBuilder(MethodNode inMethodNode) {
            this.inMethodNode = inMethodNode;
        }


        @Override
        public void edit(MethodCall m) throws CannotCompileException {
            String paramStr = Descriptor.toString(m.getSignature());
            paramStr = paramStr.substring(1, paramStr.length() - 1);
            String[] paramSplit = paramStr.split(",");
            StringBuilder completeMethodName = new StringBuilder();
            completeMethodName.append(m.getMethodName() + "(");
            if (VarValidator.notEmpty(paramSplit)) {
                for (int i = 0; i < paramSplit.length; i++) {
                    completeMethodName.append(TypeUtil.unbox(paramSplit[i]));
                    if (i != paramSplit.length - 1) {
                        completeMethodName.append(",");
                    }
                }
            }
            completeMethodName.append(")");
            buildCall(m.getClassName(), completeMethodName.toString());

//            try {
//                CtMethod method = m.getMethod();
//            } catch (NotFoundException e) {
//                System.out.println("依赖外部方法 => " + completeMethodName.toString());
//                System.out.println("依赖外部方法 => " + e.getMessage()
//                        + ", funcName => " + Descriptor.toString(m.getSignature())
//                        + ", => " + m.getMethodName());
//                throw new RuntimeException(e);
//            }
        }

        public void buildCall(String outClassName, String outCompleteMethodName) {
            if (this.inMethodNode == null) { // 若入节点为null，则记录为异常
                logger.error("the in method is not exist.");
                return;
            }


            // 一定非空
//            MethodNode outMethodNode = extractMethodNode(outClassName, outCompleteMethodName);
            MethodNode outMethodNode = graph.findMethodNodeById(outClassName + "." + outCompleteMethodName);
            if (outMethodNode == null) { // 若outMethodNode为项目外方法，则不做处理
                return;
            }

            if (outMethodNode.equals(this.inMethodNode)) { // 若自己依赖自己，则不做处理
                return;
            }

            graph.addCall(inMethodNode, outMethodNode);
        }

        /**
         * - 若className为int、float等基本数据类型，则返回对应的包装类型
         * - 若className为数组，则返回数组的类型
         * - 若className为内部类，则返回其外部类Node
         * 若处理后的className对应的文件粒度Node不存在，则默认创建 项目外的的文件粒度的Node 和对应的并添加到graph中后返回
         * TODO: 未判断依赖的节点的外部类是否存在，若不存在则可能出现是通过工具直接生成的内部类
         * @return
         */
        public MethodNode extractMethodNode(String outClassName, String outCompleteMethodName) {

            MethodNode outMethodNode = graph.findMethodNodeById(outClassName + "." + outCompleteMethodName);

            if (outMethodNode == null) { // 若为null，则表示graph中未添加该系统外Node
//                if (feignFileNames.contains(fileName)) {
//                    methodNode = GraphManager.buildNonSystemFeignFile(fileName);
//                } else {
//                    methodNode = GraphManager.buildNonSystemCellFile(fileName);
//                }
                outMethodNode = new MethodNode(outClassName, outCompleteMethodName, false, false, false);
                graph.addMethodNode(outMethodNode);
            }
            return outMethodNode;
        }
    }
}
