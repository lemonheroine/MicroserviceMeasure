package top.lazyr.microserviceName_structure.classinfo;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.constant.Printer;
import top.lazyr.manager.CtClassManager;
import top.lazyr.microserviceName_structure.classinfo.model.FieldInfo;
import top.lazyr.microserviceName_structure.classinfo.model.MethodInfo;
import top.lazyr.util.ClsUtil;
import top.lazyr.util.SCUtil;
import top.lazyr.util.TypeUtil;
import top.lazyr.validator.VarValidator;

import java.util.*;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class ClassInfoParser {
    private static Logger logger = LoggerFactory.getLogger(ClassInfoParser.class);
    private CtClassManager ctClassManager;

    public ClassInfoParser() {
        this.ctClassManager = CtClassManager.getCtClassManager();
    }

    /**
     * @param svcPath
     * @param isApi
     * @return
     */
    public List<MethodInfo> parseMethodInfos(String svcPath, boolean isApi) {
        List<MethodInfo> methodInfos = new ArrayList<>();
        List<CtClass> ctClasses = ctClassManager.extractCtClass(svcPath);
        if (VarValidator.empty(ctClasses)) {
            logger.info( svcPath + " 路径下无class文件");
            return methodInfos;
        }

        for (CtClass ctClass : ctClasses) {
            boolean isApiClass = SCUtil.isApiClass(ctClass);
            List<MethodInfo> methodInfosByACls = null;
            /**
             * - 若 isApi = true, 则只获取接口类中的方法
             * - 若 isApi = false, 则只获取实体类中的方法
             */
            if ((isApi && isApiClass) || (!isApi && !isApiClass)) {
                methodInfosByACls = extractMethodInfos(ctClass, isApi);
            }

            if (VarValidator.empty(methodInfosByACls)) {
                if (isApiClass == isApi) {
                    logger.info("{}({})无{}", isApiClass ? "接口" : "实体类", ctClass.getName(), isApiClass ? "操作" : "方法");
                }
                continue;
            }
            methodInfos.addAll(methodInfosByACls);
        }
        return methodInfos;
    }


    public List<FieldInfo> parseFieldInfos(String svcPath, boolean isApi) {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        List<CtClass> ctClasses = ctClassManager.extractCtClass(svcPath);
        if (VarValidator.empty(ctClasses)) {
            logger.info( svcPath + " 路径下无class文件");
            return fieldInfos;
        }
        for (CtClass ctClass : ctClasses) {
            if (SCUtil.isApiClass(ctClass) != isApi) {
                continue;
            }
            List<FieldInfo> fieldInfosByACls = extractFiledInfos(ctClass);
            if (VarValidator.empty(fieldInfosByACls)) {
                continue;
            }
            fieldInfos.addAll(fieldInfosByACls);
        }
        return fieldInfos;
    }

    /**
     * 获取cls中所有的方法（包括父类）对应的MethodInfo
     * - 若isApi为true，则只返回api类型的方法
     * @param cls
     * @return
     */
    private List<MethodInfo> extractMethodInfos(CtClass cls, boolean isApi) {
        List<MethodInfo> methodInfos = new ArrayList<>();
        CtMethod[] methods = CtClassManager.getAllCtMethods(cls);
        if (VarValidator.empty(methods)) {
            logger.info("{} 中无方法", cls.getName());
            return methodInfos;
        }

        boolean isApiClass = SCUtil.isApiClass(cls);
        for (CtMethod method : methods) {
            boolean isApiFunc = SCUtil.isApiFunc(method);

            MethodInfo methodInfo = null;
            if (isApi && isApiClass && isApiFunc) { // 获取api方法 && 是api类 && 是api方法
                methodInfo = buildMethodInfo(method, cls);
            } else if (!isApi) { // 获取实体类中的所有方法
                methodInfo = buildMethodInfo(method, cls);
            }
            if (methodInfo == null) {
                continue;
            }


            methodInfos.add(methodInfo);
        }
        return methodInfos;
    }

    /**
     * 获取该类及其父类所有的方法
     * - 若 method or belongClass 为null，则返回null
     * - 若 method or belongClass 不为null，则返回对应的MethodInfo
     * @param ctMethod
     * @param belongClass
     * @return
     */
    private MethodInfo buildMethodInfo(CtMethod ctMethod, CtClass belongClass) {
        if (ctMethod == null || belongClass == null) {
            return null;
        }
        MethodInfo methodInfo = null;
        List<String> paramTypes = new ArrayList<>();
        String typeName = "";
//        String methodName = ctMethod.getName();
//        if (methodName.contains("getUseIntegrationAmount")) {
//            System.out.println("getUseIntegrationAmount => " + Descriptor.toString(ctMethod.getSignature()));
//        }

        // 根据方法参数签名来获取参数列表
        String paramStr = Descriptor.toString(ctMethod.getSignature());
        paramStr = paramStr.substring(1, paramStr.length() - 1);
        String[] paramSplit = paramStr.split(",");
        if (VarValidator.notEmpty(paramSplit)) {
            for (String param : paramSplit) {
                paramTypes.add(TypeUtil.unbox(param));
            }
        }

//        try {
//            CtClass[] paramClasses = ctMethod.getParameterTypes();
//            if (VarValidator.notEmpty(paramClasses)) {
//                for (CtClass paramClass : paramClasses) {
//                    paramTypes.add(TypeUtil.unbox(paramClass.getName()));
//                }
//            }
//        } catch (NotFoundException e) { // 参数为项目外类
//            String[] paramClassNames = e.getMessage().split(",");
//            for (String paramClassName : paramClassNames) {
//                paramTypes.add(paramClassName);
//            }
//        }

        try {
            typeName = ctMethod.getReturnType().getName();
        } catch (NotFoundException e) { // 返回值为项目外类
            typeName = e.getMessage();
        }
        methodInfo = MethodInfo.builder()
                .methodModifier(ClsUtil.toModifierStr(ctMethod.getModifiers()))
                .methodName(ctMethod.getName())
                .paramClassNames(paramTypes)
                .returnClassName(typeName.equals("void") ? "void" : TypeUtil.unbox(typeName))
                .isApiClass(SCUtil.isApiClass(belongClass))
                .isApiFunc(SCUtil.isApiFunc(ctMethod))
                .belongClassName(belongClass.getName())
                .belongClassModifier(ClsUtil.toModifierStr(belongClass.getModifiers()))
                .belongClassType(ClsUtil.toTypeStr(belongClass.getModifiers()))
                .build();
        return methodInfo;
    }

    /**
     * 获取cls中所有的字段对应的FieldInfo
     * @param cls
     * @return
     */
    private List<FieldInfo> extractFiledInfos(CtClass cls) {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        CtField[] fields = cls.getDeclaredFields();
        if (VarValidator.empty(fields)) {
            logger.info("{} 中无成员变量", cls.getName());
            return fieldInfos;
        }

        for (CtField field : fields) {
            FieldInfo fieldInfo = buildFieldInfo(field, cls);
            if (fieldInfo == null) {
                continue;
            }
            fieldInfos.add(fieldInfo);
        }
        return fieldInfos;
    }

    /**
     * - 若 field or belongClass 为null，则返回null
     * - 若 field or belongClass 不为null，则返回对应的FieldInfo
     * @param field
     * @param belongClass
     * @return
     */
    private FieldInfo buildFieldInfo(CtField field, CtClass belongClass) {
        if (field == null || belongClass == null) {
            return null;
        }
        String typeName = "";
        try {
            CtClass type = field.getType();
            typeName = type.getName();
        } catch (NotFoundException e) {
            typeName = e.getMessage();
        }

        FieldInfo fieldInfo = FieldInfo.builder()
                .fieldName(field.getName())
                .className(TypeUtil.unbox(typeName))
                .modifier(ClsUtil.toModifierStr(field.getModifiers()))
                .isApiClass(SCUtil.isApiClass(belongClass))
                .belongClassModifier(ClsUtil.toModifierStr(belongClass.getModifiers()))
                .belongClassType(ClsUtil.toTypeStr(belongClass.getModifiers()))
                .belongClassName(belongClass.getName()).build();

        return fieldInfo;
    }


    public static void main(String[] args) {
        String svcPath = "/Users/lazyr/Work/projects/devops/test/data/dop/application-server";
        ClassInfoParser parser = new ClassInfoParser();
//        List<MethodInfo> methodInfos = parser.parseMethodInfos(svcPath, false);
//        Printer.printList(methodInfos);
        List<FieldInfo> fieldInfos = parser.parseFieldInfos(svcPath, false);
        Printer.printList(fieldInfos);
    }

}
