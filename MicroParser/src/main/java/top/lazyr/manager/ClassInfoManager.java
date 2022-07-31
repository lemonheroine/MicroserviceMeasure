package top.lazyr.manager;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.microserviceName_structure.classinfo.model.FieldInfo;
import top.lazyr.microserviceName_structure.classinfo.model.MethodInfo;
import top.lazyr.util.TypeUtil;
import top.lazyr.validator.VarValidator;

import java.util.*;

/**
 * @author lazyr
 * @created 2022/5/12
 */
public class ClassInfoManager {
    private static Logger logger = LoggerFactory.getLogger(ClassInfoManager.class);

    /**
     * 获取cls中所有的方法（包括父类）对应的MethodInfo
     * @param cls
     * @return
     */
    public List<MethodInfo> extractMethodInfos(CtClass cls) {
        List<MethodInfo> methodInfos = new ArrayList<>();
        CtMethod[] methods = getAllCtMethods(cls);
        if (VarValidator.empty(methods)) {
            logger.info("{} 中无方法", cls.getName());
            return methodInfos;
        }
        for (CtMethod method : methods) {
            MethodInfo methodInfo = buildMethodInfo(method, cls);
            if (methodInfo == null) {
                continue;
            }
            methodInfos.add(methodInfo);
        }
        return methodInfos;
    }

    /**
     * 获取cls中所有修饰符的CtMethod
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

    /**
     * 获取cls中所有的字段对应的FieldInfo
     * @param cls
     * @return
     */
    public List<FieldInfo> extractFiledInfos(CtClass cls) {
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
                .modifier(toModifierStr(field.getModifiers()))
                .belongClassModifier(toModifierStr(belongClass.getModifiers()))
                .belongClassType(toTypeStr(belongClass.getModifiers()))
                .belongClassName(belongClass.getName()).build();

        return fieldInfo;
    }

    /**
     * 获取该类及其父类所有的方法
     * - 若 method or belongClass 为null，则返回null
     * - 若 method or belongClass 不为null，则返回对应的MethodInfo
     * @param method
     * @param belongClass
     * @return
     */
    public MethodInfo buildMethodInfo(CtMethod method, CtClass belongClass) {
        if (method == null || belongClass == null) {
            return null;
        }
        MethodInfo methodInfo = null;
        List<String> paramTypes = new ArrayList<>();
        String typeName = "";
        try {
            CtClass[] paramClasses = method.getParameterTypes();
            if (VarValidator.notEmpty(paramClasses)) {
                for (CtClass paramClass : paramClasses) {
                    paramTypes.add(TypeUtil.unbox(paramClass.getName()));
                }
            }
        } catch (NotFoundException e) { // 参数为项目外类
//            throw new RuntimeException(e);
            String[] paramClassNames = e.getMessage().split(",");
            for (String paramClassName : paramClassNames) {
                paramTypes.add(paramClassName);
            }
        }
        try {
            typeName = method.getReturnType().getName();
        } catch (NotFoundException e) { // 返回值为项目外类
//            throw new RuntimeException(e);
            typeName = e.getMessage();
        }
        methodInfo = MethodInfo.builder()
                .methodModifier(toModifierStr(method.getModifiers()))
                .methodName(method.getName())
                .paramClassNames(paramTypes)
                .returnClassName(typeName.equals("void") ? "void" : TypeUtil.unbox(typeName))
                .belongClassName(belongClass.getName())
                .belongClassModifier(toModifierStr(belongClass.getModifiers()))
                .belongClassType(toTypeStr(belongClass.getModifiers()))
                .build();
        return methodInfo;
    }

//    /**
//     * 获取本类及其⽗类的字段属性
//     * @param clazz 当前类对象
//     * @return 字段数组
//     */
//    public static CtField[] getAllFields(CtClass clazz) {
//        List<CtField> fieldList = new ArrayList<>();
//        while (clazz != null){
//            fieldList.addAll(new ArrayList<>(Arrays.asList(clazz.getDeclaredFields())));
//            try {
//                clazz = clazz.getSuperclass();
//            } catch (NotFoundException e) {
//                logger.info("获取类({})的父类({})失败", clazz.getName(), e.getMessage());
////                throw new RuntimeException(e);
//            }
//        }
//        CtField[] fields = new CtField[fieldList.size()];
//        return fieldList.toArray(fields);
//    }

    /**
     * 将修饰符数字转换为类的暴露方式
     * - public
     * - private
     * - abstract
     * - default
     * @param modifier
     * @return
     */
    public static String toModifierStr(int modifier) {
        if ((modifier & 1) != 0) {
            return  "public";
        }

        if ((modifier & 4) != 0) {
            return "protected";
        }

        if ((modifier & 2) != 0) {
            return "private";
        }

        return "default";
    }

    /**
     * 将修饰符数字转换为类的类型
     * - interface
     * - abstract
     * - normal
     * @param modifier
     * @return
     */
    public static String toTypeStr(int modifier) {
        if ((modifier & 512) != 0) {
            return "interface";
        }

        if ((modifier & 1024) != 0) {
            return "abstract";
        }

        return "normal";
    }

    public static void main(String[] args) {
        String path = "/Users/lazyr/Work/projects/devops/lazy_ms_detection/target/classes/top/lazyr/Person.class";
        CtClassManager ctClassManager = CtClassManager.getCtClassManager();
        CtClass ctClass = ctClassManager.getOuterCtClass(path);
        ClassInfoManager classInfoManager = new ClassInfoManager();
        List<MethodInfo> methodInfos = classInfoManager.extractMethodInfos(ctClass);
        for (MethodInfo methodInfo : methodInfos) {
            System.out.println(methodInfo);
        }
//        List<FieldInfo> fieldInfos = classInfoManager.extractFiledInfos(ctClass);
//        for (FieldInfo fieldInfo : fieldInfos) {
//            System.out.println(fieldInfo);
//        }
    }

}
