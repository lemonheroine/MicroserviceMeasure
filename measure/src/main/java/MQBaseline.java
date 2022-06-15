
import org.apache.xmlbeans.impl.xb.ltgfmt.TestCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MQBaseline {

    Map<String, MQBaseline.Microservice> microserviceMap=new HashMap<>();
    Map<String,Integer> microservice2IndexMap=new HashMap<>();
    Map<Integer,String> index2MicroserviceMap=new HashMap<>();
    int microserviceIndex = 0;

    public static void main(String[] args) throws IOException {
        String microservicesRecordFile = "svc_info.txt";
        String projectPrefix = "data/";
        String projectLocation = "61-microservice-recruit-master";

        String microservicesDependencies = "/"+projectLocation+"_structure.xlsx";

        List<String> microservicesList = Arrays.asList(new String(Files.readAllBytes(Paths.get(projectPrefix+projectLocation+"/"+microservicesRecordFile)), StandardCharsets.UTF_8).split(",", -1));

        double MQBaseline = new MQBaseline().calculateMicroserviceMQ(projectPrefix+projectLocation+"/", microservicesList,microservicesDependencies);
        System.out.println("Modularity of this MSA is "+MQBaseline);

    }

    public double calculateMicroserviceMQ(String projectPath, List<String>  microservicesList,String microservicesDependencies){
        // 先读取每个微服务结构数据并计算其内聚
        double COH = 0;
        for (String microserviceName: microservicesList){
            //String microserviceName = microserviceFile.substring(0,microserviceFile.indexOf("_structure"));
            if (!microserviceMap.containsKey(microserviceName)){
                MQBaseline.Microservice microservice = readASingleMicroservice(microserviceName,projectPath+microserviceName+"_structure.xlsx");
                if (microservice != null) COH += calculateMicroserviceCohesion(microservice,projectPath+microserviceName+"_structure.xlsx");
            }
        }
        COH /= microserviceIndex;
        System.out.println("cohesion of this MSA is "+COH);

        //再读取微服务之间的依赖数据并计算耦合
        double COP = calculateMicroservicesCoupling(projectPath+microservicesDependencies);
        System.out.println("coupling of this MSA is "+COP);
        return COH-COP;
    }

    public Microservice readASingleMicroservice(String microserviceName,String fileName){
        Microservice microservice = new MQBaseline.Microservice(microserviceName);
        //读取类方法表信息
        List<List<String>> classMethodList=Util.readExcel(fileName,5,0);
        for(List<String> list:classMethodList){
            if(list.get(0).equals(""))
                continue;
            String className = list.get(0);
            microservice.addClass(className);

            if(list.get(1).equals(""))
                continue;
            MQBaseline.Method method = new MQBaseline.Method(list.get(1),list.get(2),list.get(3),list.get(4));
            microservice.addMethod2Class(className,method);

        }

        //读取类变量表信息
        List<List<String>> classFieldList = Util.readExcel(fileName,4,1);
        for(List<String> list:classFieldList){
            if(list.get(0).equals(""))
                continue;
            String className = list.get(0);
            microservice.addClass(className);

            if(list.get(1).equals(""))
                continue;
            MQBaseline.Field field = new MQBaseline.Field(list.get(1),list.get(2),list.get(3));
            microservice.addField2Class(className,field);

        }

        //读取接口方法表信息
        List<List<String>> interfaceMethodList=Util.readExcel(fileName,5,4);
        for(List<String> list:interfaceMethodList){
            if(list.get(0).equals(""))
                continue;
            String interfaceName = list.get(0);
            microservice.addInterface(interfaceName);

            if(list.get(1).equals(""))
                continue;
            MQBaseline.Method method = new MQBaseline.Method(list.get(1),list.get(2),list.get(3),list.get(4));
            microservice.addMethod2Interface(interfaceName,method);
        }

        if (microservice.getIndex() == 0 || microservice.getInterfaceIndex() == 0) return null;

        microserviceMap.put(microserviceName,microservice);
        microservice2IndexMap.put(microserviceName,microserviceIndex);
        index2MicroserviceMap.put(microserviceIndex++,microserviceName);
        return microservice;
    }

    public double calculateMicroserviceCohesion(Microservice microservice,String fileName){
        int index = microservice.getIndex();
        int interfaceIndex = microservice.getInterfaceIndex();

        System.out.println("class count:"+index);
        //处理类-类关系信息表
        int[][] relationship =new int[index][index];
        handleClassMethodRelationship(microservice,fileName,relationship);
        handleClassFiledRelationship(microservice,fileName,relationship);

        double relationshipCount = 0;
        for(int i=0;i<index;i++){
            for(int j=0;j<index;j++){
                relationshipCount+=relationship[i][j];
            }
        }
        System.out.println("class-class relationship count:"+relationshipCount);
        System.out.println("interface count:"+interfaceIndex);


        //处理接口-类关系信息表
        int[][] relationship2 =new int[interfaceIndex][index];
        handleInterfaceMethodRelationship(microservice,fileName,relationship2);
        handleInterfaceFieldRelationship(microservice,fileName,relationship2);

        int interfaceRelationshipCount = 0;
        for(int i=0;i<interfaceIndex;i++){
            for(int j=0;j<index;j++){
                interfaceRelationshipCount+=relationship2[i][j];
            }
        }
        System.out.println("interface-class relationship count:"+interfaceRelationshipCount);
        double Coh = (relationshipCount+interfaceRelationshipCount)/Math.pow(interfaceIndex+index,2);
        System.out.println("Cohesion of the "+fileName+ " microservice is: "+Coh);
        return Coh;
    }

    public double calculateMicroservicesCoupling(String microservicesDependencies){

        //处理微服务-微服务关系信息表
        int[][] relationship =new int[microserviceIndex][microserviceIndex];
        handleMicroservicesRelationship(microservicesDependencies,relationship);

        double COP = 0;
        for(int i=0;i<microserviceIndex;i++){
            for(int j=0;j<microserviceIndex;j++){
                if (i == j){

                }else{
                    int microservice1Node = microserviceMap.get(index2MicroserviceMap.get(i)).getUnitIndex();
                    int microservice2Node = microserviceMap.get(index2MicroserviceMap.get(j)).getUnitIndex();
                    double Cop =(double)(relationship[i][j]+relationship[j][i])/(2*microservice1Node*microservice2Node);
                    System.out.println("the coupling between microservice "+ i + " and microservice "+j +" is: " +Cop);
                    COP+=Cop;
                }
            }
        }
        COP /= ((microserviceIndex)*(microserviceIndex-1)/2);

        return COP;
    }

    public void handleMicroservicesRelationship(String fileName, int[][] microservicesRelationship){
        Map<String, String> microRelationshipHashMap=new HashMap<>();

        List<List<String>> relationshipList = Util.readExcel(fileName,7,0);

        for(List<String> list:relationshipList){
            MQBaseline.Microservice microservice1 = microserviceMap.get(list.get(0));
            MQBaseline.Microservice microservice2 = microserviceMap.get(list.get(3));

            if (microservice1==null||microservice2==null)
                continue;

            int index1= microservice2IndexMap.get(list.get(0));
            int index2= microservice2IndexMap.get(list.get(3));

            // 判断是否是来自同一个微服务的调用
            if (index1 == index2) continue;

            String microRelationship = list.get(0) + list.get(1) + list.get(3) + list.get(4);
            if (!microRelationshipHashMap.containsKey(microRelationship)){
                microRelationshipHashMap.put(microRelationship,microRelationship);
                microservicesRelationship[index1][index2]++;
            }

        }
    }

    public void handleClassMethodRelationship(Microservice microservice, String fileName,int[][] relationship){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,2);
        for(List<String> list:relationshipList){
            MQBaseline.Class class1=microservice.getClassMap().get(list.get(0));
            MQBaseline.Class class2=microservice.getClassMap().get(list.get(2));

            if(class1==null||class2==null)
                continue;
            //System.out.println("haha:"+list.get(0)+list.get(2));

            int index1=microservice.getClass2IndexMap().get(list.get(0));
            int index2=microservice.getClass2IndexMap().get(list.get(2));

            // 判断是否是来自同一个类的调用
            if (index1 == index2) continue;

            //判断被调用的方法是不是public
            if(!isMethodPublic(class2.methodList,list.get(3))){
               // System.out.println("an invoked method is not public!");
            }
            relationship[index1][index2]=1;
        }
    }

    public void handleClassFiledRelationship(Microservice microservice,String file,int[][] relationship){
        List<List<String>> relationshipList = Util.readExcel(file,5,3);
        for(List<String> list:relationshipList){
            MQBaseline.Class class1=microservice.getClassMap().get(list.get(0));
            MQBaseline.Class class2=microservice.getClassMap().get(list.get(2));
            if(class1==null||class2==null)
                continue;

            int index1=microservice.getClass2IndexMap().get(list.get(0));
            int index2=microservice.getClass2IndexMap().get(list.get(2));

            // 判断是否是来自同一个类的调用
            if (index1 == index2) continue;

            //判断该字段是不是public的
            if(!isFiledPublic(class2.fieldList,list.get(3))){
                //System.out.println("an invoked field is not public!");
            }

            relationship[index1][index2]=1;
        }
    }

    public void handleInterfaceMethodRelationship(Microservice microservice,String file,int[][] relationship){
        List<List<String>> relationshipList = Util.readExcel(file,5,5);
        for(List<String> list:relationshipList){
            MQBaseline.Interface i = microservice.getInterfaceMap().get(list.get(0));
            MQBaseline.Class c = microservice.getClassMap().get(list.get(2));
            if(i==null||c==null)
                continue;

            if (!isMethodPublic(c.methodList, list.get(3))) {
                //System.out.println("an invoked method of the interface is not public!");
            }
            int index1=microservice.getInterface2IndexMap().get(list.get(0));
            int index2=microservice.getClass2IndexMap().get(list.get(2));
            relationship[index1][index2]=1;
        }
    }

    public void handleInterfaceFieldRelationship(Microservice microservice,String file,int[][] relationship){
        List<List<String>> relationshipList = Util.readExcel(file,5,6);
        for(List<String> list:relationshipList){
            MQBaseline.Interface i = microservice.getInterfaceMap().get(list.get(0));
            MQBaseline.Class c = microservice.getClassMap().get(list.get(2));

            if(i!=null)
                System.out.println(list.get(0));
            if(c!=null)
                System.out.println(list.get(2));
            if(i==null||c==null)
                continue;

            if (!isFiledPublic(c.fieldList, list.get(3))) {
                //System.out.println("an invoked field of the interface is not public!");
            }
            int index1=microservice.getInterface2IndexMap().get(list.get(0));
            int index2=microservice.getClass2IndexMap().get(list.get(2));
            relationship[index1][index2] = 1;
        }
    }

    private boolean isMethodPublic(List<MQBaseline.Method> methodList, String methodName){
        for(MQBaseline.Method m:methodList){
            if(m.name.equals(methodName)){
                return m.modifier.equals("true");
            }
        }
        return false;
    }

    private boolean isFiledPublic(List<MQBaseline.Field> fieldList, String fieldName){
        for(MQBaseline.Field f:fieldList){
            if(f.name.equals(fieldName))
                return f.modifier.equals("true");
        }
        return false;
    }

    class Microservice{
        String name;

        Map<String, MQBaseline.Class> classMap;
        Map<String,Integer> class2IndexMap;
        Map<Integer,String> index2ClassMap;
        int index;

        Map<String, MQBaseline.Interface> interfaceMap;
        Map<String,Integer> interface2IndexMap;
        Map<Integer,String> index2InterfaceMap;
        int interfaceIndex;

        public Microservice(String name){
            this.name = name;
            this.classMap = new HashMap<>();
            this.class2IndexMap = new HashMap<>();
            this.index2ClassMap = new HashMap<>();
            this.index =0;

            this.interfaceMap = new HashMap<>();
            this.interface2IndexMap = new HashMap<>();
            this.index2InterfaceMap =  new HashMap<>();
            this.interfaceIndex = 0;
        }

        public int getIndex(){
            return this.index;
        }

        public int getInterfaceIndex(){
            return this.interfaceIndex;
        }

        public int getUnitIndex(){
            return this.index + this.interfaceIndex;
        }

        public Map<String, Class> getClassMap() {
            return classMap;
        }

        public Map<String, Integer> getClass2IndexMap() {
            return class2IndexMap;
        }

        public Map<String, Interface> getInterfaceMap() {
            return interfaceMap;
        }

        public Map<String, Integer> getInterface2IndexMap() {
            return interface2IndexMap;
        }

        public void addClass(String className){
            if(!classMap.containsKey(className)){
                classMap.put(className,new MQBaseline.Class(className));
                class2IndexMap.put(className,index);
                index2ClassMap.put(index++,className);
            }
        }

        public void addMethod2Class(String className, MQBaseline.Method method){
            classMap.get(className).addMethod(method);
        }

        public void addField2Class(String className, MQBaseline.Field field){
            classMap.get(className).addField(field);
        }

        public void addInterface(String interfaceName){
            if(!interfaceMap.containsKey(interfaceName)){
                interfaceMap.put(interfaceName,new MQBaseline.Interface(interfaceName));
                interface2IndexMap.put(interfaceName,interfaceIndex);
                index2InterfaceMap.put(interfaceIndex++,interfaceName);
            }
        }

        public void addMethod2Interface(String interfaceName, MQBaseline.Method method){
            interfaceMap.get(interfaceName).addMethod(method);
        }
    }

    class Class {
        String name;
        List<MQBaseline.Field> fieldList;
        int publicFieldCount;
        List<MQBaseline.Method> methodList;
        int publicMethodCount;

        public Class(String name) {
            this.name = name;
            this.fieldList = new ArrayList<>();
            this.publicFieldCount = 0;
            this.methodList = new ArrayList<>();
            this.publicMethodCount = 0;
        }

        public void addField(MQBaseline.Field a) {
            fieldList.add(a);
            if (a.modifier.equals("true"))
                publicFieldCount++;
        }

        public void addMethod(MQBaseline.Method a) {
            methodList.add(a);
            if (a.modifier.equals("true"))
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

    class Interface{
        String name;
        List<MQBaseline.Method> methodList;
        int publicMethodCount;
        public Interface(String name){
            this.name = name;
            this.methodList = new ArrayList<>();
            this.publicMethodCount ++;
        }

        public void addMethod(MQBaseline.Method m){
            methodList.add(m);
            if(m.modifier.equals("true"))
                publicMethodCount++;
        }
    }

}
