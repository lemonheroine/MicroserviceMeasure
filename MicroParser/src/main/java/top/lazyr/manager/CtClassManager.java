package top.lazyr.manager;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.util.FileUtil;
import top.lazyr.util.SCUtil;
import top.lazyr.util.TypeUtil;
import top.lazyr.validator.VarValidator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author lazyr
 * @created 2022/1/25
 */
public class CtClassManager {
    private static Logger logger = LoggerFactory.getLogger(CtClassManager.class);
    private static CtClassManager manager;
    // TODO: 代优化单例模式
    private static ClassPool classPool = new ClassPool(true);



    public static CtClassManager getCtClassManager() {
        if (manager == null) {
            manager.ensureLogManagerInitialized();
        }
        return manager;
    }

    private CtClassManager() {
    }

    private static final void ensureLogManagerInitialized() {
        // TODO: 单例模式
        manager = new CtClassManager();
    }


    /**
     * 获取指定路径的CtClass
     * @param classAbsolutePath
     * @return
     */
    public CtClass getOuterCtClass(String classAbsolutePath) {
        CtClass ctClass = null;
        try {
            ctClass = classPool.makeClass(new FileInputStream(classAbsolutePath));
        } catch (IOException e) {
            logger.error("the path ({}) of non system class not found , err: {}", classAbsolutePath, e.getMessage());
        }
        return ctClass;
    }

    /**
     * 获取已加载的CtClass
     * @param className
     * @return
     */
    public CtClass getCtClass(String className) {
        CtClass ctClass = null;
        try {
//            classPool.insertClassPath(new ClassClassPath(Class.forName(className)));//为防止项目被打成jar包，请使用该语句
            ctClass = classPool.getCtClass(className);
        } /*catch (ClassNotFoundException e) {
            logger.error("inner class file not found => " + className + ", err: " + e.getMessage());
        }*/ catch (NotFoundException e) {
            logger.error("system class not found => " + className + ", err: " + e.getMessage());
        }
        return ctClass;
    }


    /**
     * 从源码文件提取CtClass对象
     * @param sourceCodePath
     * @return
     */
    public List<CtClass> extractCtClass(String sourceCodePath) {
        List<String> filesAbsolutePath = FileUtil.getFilesAbsolutePath(sourceCodePath, ".class");
        List<CtClass> ctClasses = new ArrayList<>();
        for (String fileAbsolutePath : filesAbsolutePath) {
            CtClass ctClass = getOuterCtClass(fileAbsolutePath);
            ctClasses.add(ctClass);
        }
        return ctClasses;
    }

    public Map<String, CtClass> extractPath2CtClass(String sourceCodePath) {
        List<String> filesAbsolutePath = FileUtil.getFilesAbsolutePath(sourceCodePath, ".class");
        Map<String, CtClass> path2CtClass = new HashMap<>();
        for (String fileAbsolutePath : filesAbsolutePath) {
            CtClass ctClass = getOuterCtClass(fileAbsolutePath);
            path2CtClass.put(fileAbsolutePath, ctClass);
        }
        return path2CtClass;
    }

    /**
     * 将ctMethod转换为如下格式
     * [methodName]([paramClassName1,paramClassName2])
     * getName(java.lang.Integer[],java.lang.String)
     * getName()
     * @param ctMethod
     * @return
     */
    public static String buildCompleteMethodName(CtMethod ctMethod) {

        String methodName = ctMethod.getName();


        String paramStr = Descriptor.toString(ctMethod.getSignature());
        paramStr = paramStr.substring(1, paramStr.length() - 1);
        String[] paramSplit = paramStr.split(",");
        StringBuilder paramClassNames = new StringBuilder();
        paramClassNames.append("(");
        if (VarValidator.notEmpty(paramSplit)) {
            for (int i = 0; i < paramSplit.length; i++) {
                paramClassNames.append(TypeUtil.unbox(paramSplit[i]));
                if (i != paramSplit.length - 1) {
                    paramClassNames.append(",");
                }
            }
        }
        paramClassNames.append(")");



//        paramClassNames.append("(");
//        try {
//            CtClass[] paramClasses = ctMethod.getParameterTypes();
//            if (VarValidator.notEmpty(paramClasses)) {
//                for (int i = 0; i < paramClasses.length; i++) {
//                    CtClass paramClass = paramClasses[i];
//                    paramClassNames.append(TypeUtil.unbox(paramClass.getName()));
//                    if (i != paramClasses.length - 1){
//                        paramClassNames.append(",");
//                    }
//                }
//            }
//        } catch (NotFoundException e) { // 参数为项目外类
////            throw new RuntimeException(e);
//            String[] nonSystemParamClassNames = e.getMessage().split(",");
//            if (VarValidator.notEmpty(nonSystemParamClassNames)) {
//                for (int i = 0; i < nonSystemParamClassNames.length; i++) {
//                    String nonSystemParamClassName = nonSystemParamClassNames[i];
//                    paramClassNames.append(nonSystemParamClassName);
//                    if (i != nonSystemParamClassNames.length - 1){
//                        paramClassNames.append(",");
//                    }
//                }
//            }
//        }
//        paramClassNames.append(")");

        return methodName + paramClassNames.toString();
    }

    /**
     * 获取cls及其父类中所有修饰符的CtMethod
     * @param cls
     * @return
     */
    public static CtMethod[] getAllCtMethods(CtClass cls) {
        Set<CtMethod> ctMethods = new HashSet<>();
        CtMethod[] methods = cls.getMethods();
        ctMethods.addAll(Arrays.asList(methods));
        ctMethods.addAll(Arrays.asList(cls.getDeclaredMethods()));
        return ctMethods.toArray(new CtMethod[0]);
    }




}
