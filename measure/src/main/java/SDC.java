import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class SDC {

    Map<String,Class> classMap=new HashMap<>();
    Map<String,Integer> class2IndexMap=new HashMap<>();
    Map<Integer,String> index2ClassMap=new HashMap<>();
    int index = 0;

    Map<String,Interface> interfaceMap = new HashMap<>();
    Map<String,Integer> interface2IndexMap=new HashMap<>();
    Map<Integer,String> index2InterfaceMap = new HashMap<>();
    int interfaceIndex = 0;

    //表示实际计算过程中实体到实体关系在数值计算时所占比重
    final private double lambda = 0.5;

    public static void main(String[] args) {
        System.out.println("hello world");

        String fileName = "mall-auth_structure.xlsx";
        new SDC().calculate(fileName);
    }

    public double calculate(String fileName){

        //读取类方法表信息
        List<List<String>> classMethodList=Util.readExcel(fileName,5,0);
        for(List<String> list:classMethodList){
            if(list.get(0).equals(""))
                continue;
            String className = list.get(0);
            if(!classMap.containsKey(className)){
                classMap.put(className,new Class(className));
                class2IndexMap.put(className,index);
                index2ClassMap.put(index++,className);
            }
            if(list.get(1).equals(""))
                continue;
            Method method = new Method(list.get(1),list.get(2),list.get(3),list.get(4));
            classMap.get(className).addMethod(method);
        }

        //读取类变量表信息
        List<List<String>> classFieldList = Util.readExcel(fileName,4,1);
        for(List<String> list:classFieldList){
            if(list.get(0).equals(""))
                continue;
            String className = list.get(0);
            if(!classMap.containsKey(className)){
                classMap.put(className,new Class(className));
                class2IndexMap.put(className,index);
                index2ClassMap.put(index++,className);
            }
            if(list.get(1).equals(""))
                continue;
            Field field = new Field(list.get(1),list.get(2),list.get(3));
            classMap.get(className).addField(field);
        }

/*
        for(String className :classMap.keySet()){
            System.out.println(className);
            for(Method o:classMap.get(className).methodList){
                System.out.println("  "+o.name);
            }
            for(Field f:classMap.get(className).fieldList){
                System.out.println("   "+f.name+" "+f.modifier);
            }
        }
*/

        //处理类-类关系信息表
        int[][] relationship =new int[index][index];
        hanleClassMethodRelationship(fileName,relationship);
        hanleClassFiledRelationship(fileName,relationship);
//        for(String key:classMap.keySet()){
//            Class c=classMap.get(key);
//            System.out.println(key+" "+c.fieldList+" "+c.methodList);
//        }
//        //计算指标
//        for(int[] r:relationship)
//            System.out.println(Arrays.toString(r));
        double std1 = 0;
        for(int i=0;i<index;i++){
            for(int j=i+1;j<index;j++){
                std1+=getClassStd(i,j,relationship);
            }
        }
        double loc1 = 1-std1/(index)/(index-1)*2;

        System.out.println("class loc is: "+loc1);


        //读取接口方法表信息
        List<List<String>> interfaceMethodList=Util.readExcel(fileName,5,4);
        for(List<String> list:interfaceMethodList){
            if(list.get(0).equals(""))
                continue;
            String interfaceName = list.get(0);
            if(!interfaceMap.containsKey(interfaceName)){
                interfaceMap.put(interfaceName,new Interface(interfaceName));
                interface2IndexMap.put(interfaceName,interfaceIndex);
                index2InterfaceMap.put(interfaceIndex++,interfaceName);
            }
            if(list.get(1).equals(""))
                continue;
            Method method = new Method(list.get(1),list.get(2),list.get(3),list.get(4));
            interfaceMap.get(interfaceName).addMethod(method);
        }

        //处理类-类关系信息表
        int[][] relationship2 =new int[interfaceIndex][index];
        handleInterfaceMethodRelationship(fileName,relationship2);
        handleInterfaceFieldRelationship(fileName,relationship2);

        double std2 = 0;
        for(int i=0;i<interfaceIndex;i++){
            for(int j=0;j<index;j++){
                std2+=getInterfaceStd(i,j,relationship2);
            }
        }
        double loc2 = 1-std2/(index)/interfaceIndex;
        System.out.println("interface loc is: "+loc2);
        System.out.println("the value of SDC is: "+loc1*lambda+(1-lambda)*loc2);
        return loc1*lambda+(1-lambda)*loc2;
    }


    public void handleInterfaceMethodRelationship(String file,int[][] relationship){
        List<List<String>> relationshipList = Util.readExcel(file,5,5);
        for(List<String> list:relationshipList){
            Interface i = interfaceMap.get(list.get(0));
            Class c = classMap.get(list.get(2));
            if(i==null||c==null)
                continue;

           // System.out.println("haha:"+list.get(0)+list.get(2));

            if (isMethodPublic(c.methodList, list.get(3))) {
                int index1=interface2IndexMap.get(list.get(0));
                int index2=class2IndexMap.get(list.get(2));
                relationship[index1][index2]++;
            }
        }
    }

    public void handleInterfaceFieldRelationship(String file,int[][] relationship){
        List<List<String>> relationshipList = Util.readExcel(file,5,6);
        for(List<String> list:relationshipList){
            Interface i = interfaceMap.get(list.get(0));
            Class c = classMap.get(list.get(2));

            if(i!=null)
                System.out.println(list.get(0));
            if(c!=null)
                System.out.println(list.get(2));
            if(i==null||c==null)
                continue;
            //System.out.println("haha:"+list.get(0)+list.get(2));
            if (isFiledPublic(c.fieldList, list.get(3))) {
                int index1=interface2IndexMap.get(list.get(0));
                int index2=class2IndexMap.get(list.get(2));
                relationship[index1][index2]++;
            }
        }
    }

    public void hanleClassMethodRelationship(String file,int[][] relationship){
        List<List<String>> relationshipList = Util.readExcel(file,5,2);
        for(List<String> list:relationshipList){
            Class class1=classMap.get(list.get(0));
            Class class2=classMap.get(list.get(2));
            if(class1==null||class2==null)
                continue;
            //System.out.println("haha:"+list.get(0)+list.get(2));

            int index1=class2IndexMap.get(list.get(0));
            int index2=class2IndexMap.get(list.get(2));
            //判断这两个方法是不是public的
            if(isMethodPublic(class1.methodList,list.get(1))&&
                    isMethodPublic(class2.methodList,list.get(3))){
                relationship[index2][index1]++;
            }

            else if(isMethodPublic(class1.methodList,list.get(1)))
                relationship[index2][index1]++;

            else if(isMethodPublic(class2.methodList,list.get(3))){
                relationship[index1][index2]++;
            }
        }
    }

    public void hanleClassFiledRelationship(String file,int[][] relationship){
        List<List<String>> relationshipList = Util.readExcel(file,5,3);
        for(List<String> list:relationshipList){
            Class class1=classMap.get(list.get(0));
            Class class2=classMap.get(list.get(2));
            if(class1==null||class2==null)
                continue;

            //判断这两个方法是不是public的
            if(isFiledPublic(class2.fieldList,list.get(3))){

                int index1=class2IndexMap.get(list.get(0));
                int index2=class2IndexMap.get(list.get(2));
                relationship[index1][index2]++;
            }
        }
    }

    private double getClassStd(int i,int j,int[][] relationship){
        Class class1=classMap.get(index2ClassMap.get(i));
        Class class2=classMap.get(index2ClassMap.get(j));

        if(relationship[i][j]==0&&relationship[j][i]==0)
            return 0;

        double std=0;
        int denominator1=class1.methodList.size()*(class2.publicFieldCount+class2.publicMethodCount);
        if(denominator1>0) {
            std += 1.0 * relationship[i][j] / denominator1;
            //System.out.println(1.0 * relationship[i][j] / denominator1);
        }
        int denominator2=class2.methodList.size()*(class1.publicFieldCount+class1.publicMethodCount);
        if(denominator2>0) {
            std += 1.0 * relationship[j][i] / denominator2;
          //  System.out.println(1.0 * relationship[j][i] / denominator2);

        }

        return std/2;
    }

    private double getInterfaceStd(int i,int j,int[][] relationship){
        Interface interface1=interfaceMap.get(index2InterfaceMap.get(i));
        Class class2=classMap.get(index2ClassMap.get(j));

        double std=0;
        int denominator1=interface1.methodList.size()*(class2.publicFieldCount+class2.publicMethodCount);
        if(denominator1>0)
            std+=1.0*relationship[i][j]/denominator1;

        return std;
    }

    private boolean isMethodPublic(List<Method> methodList,String methodName){
        for(Method m:methodList){
            if(m.name.equals(methodName)){
                return m.modifier.equals("true");
            }
        }
        return false;
    }

    private boolean isFiledPublic(List<Field> fieldList,String fieldName){
        for(Field f:fieldList){
            if(f.name.equals(fieldName))
                return f.modifier.equals("true");
        }
        return false;
    }




    class Interface{
        String name;
        List<Method> methodList;
        int publicMethodCount;
        public Interface(String name){
            this.name = name;
            this.methodList = new ArrayList<>();
            this.publicMethodCount =0;
        }

        public void addMethod(Method m){
            methodList.add(m);
            if(m.modifier.equals("true"))
                publicMethodCount++;
        }
    }


    class Class{
        String name;
        List<Field> fieldList;
        int publicFieldCount;
        List<Method> methodList;
        int publicMethodCount;
        public Class(String name){
            this.name = name;
            this.fieldList = new ArrayList<>();
            this.publicFieldCount = 0;
            this.methodList = new ArrayList<>();
            this.publicMethodCount = 0;
        }

        public void addField(Field a){
            fieldList.add(a);
            if(a.modifier.equals("true"))
                publicFieldCount++;
        }

        public void addMethod(Method a){
            methodList.add(a);
            if(a.modifier.equals("true"))
                publicMethodCount++;
        }

    }

    class Method{
        String name;
        String parameter;
        String returnType;
        String modifier;
        //目前来看并没有parameter属性，因为该属性被包含在了name里
        public Method(String name,String modifier,String returnType,String parameter){
            this.name = name;
            this.parameter =parameter;
            this.returnType = returnType;
            this.modifier = modifier;
        }
    }

    class Field{
        String name;
        String type;
        String modifier;
        public Field(String name ,String modifier,String type){
            this.name = name;
            this.type = type;
            this.modifier = modifier;
        }
    }
}
