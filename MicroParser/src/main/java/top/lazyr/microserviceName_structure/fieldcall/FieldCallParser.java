package top.lazyr.microserviceName_structure.fieldcall;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.manager.ClassInfoManager;
import top.lazyr.manager.CtClassManager;
import top.lazyr.microserviceName_structure.fieldcall.model.FieldCallGraph;
import top.lazyr.microserviceName_structure.fieldcall.model.FieldNode;
import top.lazyr.microserviceName_structure.fieldcall.model.MethodNode;
import top.lazyr.util.SCUtil;
import top.lazyr.validator.VarValidator;

import java.util.List;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class FieldCallParser {
    private static Logger logger = LoggerFactory.getLogger(FieldCallParser.class);
    private CtClassManager ctClassManager;
    private FieldCallGraph graph;

    public FieldCallParser() {
        this.ctClassManager = CtClassManager.getCtClassManager();
        this.graph = new FieldCallGraph();
    }

    public FieldCallGraph parse(String svcPath) {
        List<CtClass> ctClasses = ctClassManager.extractCtClass(svcPath);
        if (VarValidator.empty(ctClasses)) {
            logger.info(svcPath + " 下无class文件");
            return graph;
        }
        extractSystemMethodNodes(ctClasses);
        extractSystemFieldNodes(ctClasses);
        buildMethodCall(ctClasses);
        return graph;
    }

    private void extractSystemFieldNodes(List<CtClass> ctClasses) {
        if (VarValidator.empty(ctClasses)) {
            logger.info("无类文件");
            return;
        }
        for (CtClass ctClass : ctClasses) {
            boolean isApiClass = SCUtil.isApiClass(ctClass);
            if (isApiClass) { // 不考虑接口中的字段
                continue;
            }
            boolean isSystem = true;
            CtField[] fields = ctClass.getDeclaredFields();
            if (VarValidator.empty(fields)) {
                continue;
            }

            for (CtField field : fields) {
                FieldNode fieldNode = new FieldNode(ctClass.getName(), field.getName());
                graph.addFieldNode(fieldNode);
            }
        }
    }

    private void extractSystemMethodNodes(List<CtClass> ctClasses) {
        if (VarValidator.empty(ctClasses)) {
            logger.info("无类文件");
            return;
        }
        for (CtClass ctClass : ctClasses) {
            boolean isApiClass = SCUtil.isApiClass(ctClass);
            boolean isSystem = true;
            CtMethod[] ctMethods = ClassInfoManager.getAllCtMethods(ctClass);
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
                    ctMethod.instrument(new FieldCallBuilder(methodNode));
                } catch (CannotCompileException e) {
                    logger.error("build depend edge failed: err = " + e.getMessage());
                }
            }
        }
    }

    private class FieldCallBuilder extends ExprEditor {
        private MethodNode inMethodNode;

        public FieldCallBuilder(MethodNode inMethodNode) {
            this.inMethodNode = inMethodNode;
        }

        @Override
        public void edit(FieldAccess f) throws CannotCompileException {
//            System.out.println("类名 => " + f.getClassName());
//            System.out.println("字段名 => " + f.getFieldName());
//            System.out.println("签名 => " + f.getSignature());
            buildCall(f.getClassName(), f.getFieldName());
        }

        public void buildCall(String outClassName, String outFieldName) {
            if (this.inMethodNode == null) { // 若入节点为null，则记录为异常
                logger.error("the in method is not exist.");
                return;
            }

//            FieldNode outFieldNode = extractFieldNode(outClassName, outFieldName);
            FieldNode outFieldNode = graph.findFieldNodeById(outClassName + "." + outFieldName);
            if (outFieldNode == null) { // 若非项目内字段，则不做处理
                return;
            }

            if (outClassName.equals(inMethodNode.getBelongClassName())) { // 若自己依赖自己，则不做处理
                return;
            }
            graph.addCall(inMethodNode, outFieldNode);
        }

        private FieldNode extractFieldNode(String outClassName, String outFieldName) {
            FieldNode outFieldNode = graph.findFieldNodeById(outClassName + "." + outFieldName);
            if (outFieldNode == null) { // 若为null，则表示graph中未添加FieldNode
                outFieldNode = new FieldNode(outClassName, outFieldName);
                graph.addFieldNode(outFieldNode);
            }
            return outFieldNode;
        }
    }
}
