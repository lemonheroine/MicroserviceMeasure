package top.lazyr.microservice.parser;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.expr.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.manager.CtClassManager;
import top.lazyr.microservice.manager.GraphManager;
import top.lazyr.microservice.graph.svc.InternalGraph;
import top.lazyr.util.ClsUtil;
import top.lazyr.util.SCUtil;
import top.lazyr.microservice.graph.svc.Edge;
import top.lazyr.microservice.graph.svc.FileNode;
import top.lazyr.util.TypeUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyr
 * @created 2022/4/22
 */
public class InternalGraphParser {
    private static Logger logger = LoggerFactory.getLogger(InternalGraphParser.class);
    private CtClassManager ctClassManager;
    private InternalGraph graph;
    /* 所有服务的FeignFile名字 */
    private Set<String> feignFileNames;

    public InternalGraphParser(Set<String> feignFileNames) {
        this.ctClassManager = CtClassManager.getCtClassManager();
        this.graph = new InternalGraph();
        this.feignFileNames = feignFileNames;
    }

    public InternalGraph parse(String svcPath) {
        List<CtClass> ctClasses = ctClassManager.extractCtClass(svcPath);
        extractSystemFileNodes(ctClasses);
        buildFileCall(ctClasses);
        return graph;
    }

    /**
     * 构建文件与文件之间的调用关系（详细构建规则看内部类FileDepend中注释）
     */
    private void buildFileCall(List<CtClass> ctClasses) {
        Set<String> innerClassNames = new HashSet<>();
        for (CtClass ctClass : ctClasses) {
            String className = ctClass.getName();
            if (className.contains("$")) {
                innerClassNames.add(className);
            }
            try {
                ctClass.instrument(new FileCallBuilder(className));
            } catch (CannotCompileException e) {
                logger.error("build depend edge failed: err = " + e.getMessage());
            }
        }

        // 删除内部类节点
        for (String innerClassName : innerClassNames) {
            graph.removeFileNode(innerClassName);
        }
    }

    /**
     * 将ctClasses转换系统内文件节点
     * @param ctClasses
     * @return
     */
    private void extractSystemFileNodes(List<CtClass> ctClasses) {
        for (CtClass ctClass : ctClasses) {
            FileNode fileNode = null;
            if (SCUtil.isFeign(ctClass)) {
                fileNode = GraphManager.buildSystemFeignFile(ctClass.getName());
            } else if (SCUtil.isApiClass(ctClass)) {
                fileNode = GraphManager.buildSystemApiFile(ctClass.getName());
            } else {
                fileNode = GraphManager.buildSystemCellFile(ctClass.getName());
            }
            graph.addFileNode(fileNode);
        }
    }

    private class FileCallBuilder extends ExprEditor {
        /* 会将内部类转换为外部类名 */
        private String inFileName;
        /* sourceClassName对应的外部类Java文件 */
        private FileNode inFileNode;
        /**
         * 使用该类构建依赖关系的inClassName都为项目内的类，则默认FileNode已创建
         * - 若未创建，则记录为error
         * - 若sourceClassName为内部类的名字且外部类Node未创建，则记录为error
         * - 若sourceClassName则将其转换为外部类的名字
         * @param inClassName
         */
        public FileCallBuilder(String inClassName) {
            this.inFileName = ClsUtil.extractFileName(inClassName);
            this.inFileNode = graph.findFileByName(this.inFileName);
        }

        /**
         * 当 类A调用类B的方法 时触发
         * - 若 类B == 类A，则不认为类A依赖类B；
         * - 若 类B 为 类A的内部类，则不认为类A依赖类B
         * - 若 类B 为 类A的外部类，则不认为类A依赖类B
         * - 若 类A为内部类， 类B不为内部类，则认为类A的外部类依赖类B
         * - 若 类A为内部类，类B为内部类，则认为类A的外部类依赖类B的外部类
         * - 若 类A为普通类，类B为内部类，则认为类A依赖类B的外部类
         * - 若 类B为基本数据类型数组，
         *   - 若调用的为clone方法等数组自己实现的方法，则认为类A依赖类B的包装类
         *   - 否则认为类A依赖Object（编译原理导致的）
         * - 若 类B为普通类型数组，则认为类A依赖类B
         * @param m
         * @throws CannotCompileException
         */
        @Override
        public void edit(MethodCall m) throws CannotCompileException {
            String dependClassName = m.getClassName();
//            logger.debug("class({}) call the method({}) of class({}).", this.inFileName, m.getMethodName(), dependClassName);
            buildCall(dependClassName, Edge.METHOD_CALL);
        }

        /**
         * 当 类A使用super()调用父类B的构造器 时触发（只会调用父类），暂时废弃
         * @param c
         * @throws CannotCompileException
         */
        @Override
        public void edit(ConstructorCall c) throws CannotCompileException {
//            logger.debug("class({}) use the constructor({}) parent class({}).", inFileName, c.getMethodName(), c.getClassName());
//            buildDepend(c.getClassName());
        }

        /**
         * 当 类A调用类B的成员变量 时触发
         * - 若成员变量为基本数据类型，则不认为类A依赖类B（编译后会直接将基本数据类型赋值）
         * - 若成员变量为普通类型，则认为类A依赖类B
         * @param f
         * @throws CannotCompileException
         */
        @Override
        public void edit(FieldAccess f) throws CannotCompileException {
//            logger.debug("class({}) use the field({}) of class({}).", inFileName, f.getFieldName(), f.getClassName());
            buildCall(f.getClassName(), Edge.FIELD_ACCESS);
        }

        /**
         * 当 类A使用new初始化普通类B 时触发
         * - 则认为类A依赖类B
         * @param e
         * @throws CannotCompileException
         */
        @Override
        public void edit(NewExpr e) throws CannotCompileException {
//            logger.debug("class({}) new class({}).", inFileName, e.getClassName());
            buildCall(e.getClassName(), Edge.NEW_EXPR);
        }

        /**
         * 当 类A使用new初始化数组 时触发
         * - 若数组的类型为基本数据类型，则将其转换为对应的包装类B，则认为类A依赖类B
         * - 若数组的类型为普通类型B，则认为类A依赖类B
         * @param a
         * @throws CannotCompileException
         */
        @Override
        public void edit(NewArray a) throws CannotCompileException {
            String dependClassName = "";
            try {
                dependClassName = TypeUtil.unboxing(a.getComponentType().getName());
            } catch (NotFoundException e) {
                dependClassName = e.getMessage();
                logger.info("init array({}) of non_system.", e.getMessage());
            }
//            logger.debug("class({}) new array({}).", inFileName, dependClassName);
            buildCall(dependClassName, Edge.NEW_ARRAY);
        }

        /**
         * 当 类A中使用类B进行类型转换 时触发
         * - 若类B为基本数据类型，则不认为类A依赖类B的包装类（直接编译成类B）
         * - 若类B为普通类型，则认为类A依赖类B
         * @param c
         * @throws CannotCompileException
         */
        @Override
        public void edit(Cast c) throws CannotCompileException {
            String dependClassName = "";
            try {
                dependClassName = c.getType().getName();
            } catch (NotFoundException e) {
                dependClassName = e.getMessage();
            }
//            logger.debug("in the class({}), cast class({}).", inFileName, dependClassName);
            buildCall(dependClassName, Edge.CAST);
        }

        /**
         * 当 类A中使用try-catch语句时触发
         * - 当使用throws语句抛出异常类B时，则认为类A依赖类B
         * @param h
         * @throws CannotCompileException
         */
        @Override
        public void edit(Handler h) throws CannotCompileException {
            CtClass[] throwsCtClasses = h.mayThrow();
            for (CtClass throwsCtClass : throwsCtClasses) {
                buildCall(throwsCtClass.getName(), Edge.HANDLER);
            }
        }

        /**
         * 类A为sourceClassName的外部类，类B为dependClassName的外部类
         * - 若 类B == 类A，则不认为类A依赖类B；
         * - 若 类B 为 类A的内部类，则不认为类A依赖类B
         * - 若 类B 为 类A的外部类，则不认为类A依赖类B
         * - 若 类A为内部类， 类B不为内部类，则认为类A的外部类依赖类B
         * - 若 类A为内部类，类B为内部类，则认为类A的外部类依赖类B的外部类
         * - 若 类A为普通类，类B为内部类，则认为类A依赖类B的外部类
         * - 若 类B为基本数据类型数组，
         *   - 若调用的为clone方法等数组自己实现的方法，则认为类A依赖类B的包装类
         *   - 否则认为类A依赖Object（编译原理导致的）
         * - 若 类B为普通类型数组，则认为类A依赖类B
         * - 若类A不存在，则记录为异常
         * @param outClassName
         */
        public void buildCall(String outClassName, String type) {
            if (this.inFileNode == null) { // 若类A不存在，则记录为异常
                logger.error("the class({}) is not existed.", inFileName);
                return;
            }
            // 一定非空
            FileNode outFileNode = extractFileNode(outClassName);
            if (outFileNode.equals(this.inFileNode)) { // 若自己依赖自己，则不做处理
                return;
            }
//            System.out.println(inFileName + " => " + outClassName);
            graph.addCall(inFileNode, outFileNode, type);
        }

        /**
         * - 若className为int、float等基本数据类型，则返回对应的包装类型
         * - 若className为数组，则返回数组的类型
         * - 若className为内部类，则返回其外部类Node
         * 若处理后的className对应的文件粒度Node不存在，则默认创建 项目外的的文件粒度的Node 和对应的并添加到graph中后返回
         * TODO: 未判断依赖的节点的外部类是否存在，若不存在则可能出现是通过工具直接生成的内部类
         * @param className
         * @return
         */
        public FileNode extractFileNode(String className) {
            String fileName = TypeUtil.arr2Class(
                                                            ClsUtil.extractFileName(
                                                                    TypeUtil.unboxing(className)));
            FileNode fileNode = graph.findFileByName(fileName);

            if (fileNode == null) { // 若为null，则表示graph中未添加该系统外Node
                if (feignFileNames.contains(fileName)) {
                    fileNode = GraphManager.buildNonSystemFeignFile(fileName);
                } else {
                    fileNode = GraphManager.buildNonSystemCellFile(fileName);
                }
                graph.addFileNode(fileNode);
            }
            return fileNode;
        }
    }


}
