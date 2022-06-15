import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MiMQ {

    Map<String, MiMQ.Microservice> microserviceMap=new HashMap<>();
    Map<String,Integer> microservice2IndexMap=new HashMap<>();
    Map<Integer,String> index2MicroserviceMap=new HashMap<>();
    int microserviceIndex = 0;

    public static void main(String[] args) throws IOException {
        String microservicesRecordFile = "svc_info.txt";
        String projectPrefix = "data/";
        String projectLocation = "61-microservice-recruit-master";
        //String projectLocation = "63-springcloud-course";
        //String projectLocation = "70-SpringCloudDemo";
        //String projectLocation = "76-cangjingge";
        //String projectLocation = "77-momo-cloud-permission";
        //String projectLocation = "116-spring-boot-microservices";
        //String projectLocation = "116-spring-boot-microservices";
        //String projectLocation = "123-madao_service";
        //String projectLocation = "131-mall-cloud-alibaba"; //interface invoke interface
        //String projectLocation = "195-iclyj-cloud"; //bug
        //String projectLocation = "199-light-reading-cloud";
        String microservicesDependencies = "/"+projectLocation+"_structure.xlsx";
        List<String> microservicesList = Arrays.asList(new String(Files.readAllBytes(Paths.get(projectPrefix+projectLocation+"/"+microservicesRecordFile)), StandardCharsets.UTF_8).split(",", -1));

        System.out.println(microservicesList.size());

        double MiMQ = new MiMQ().calculateMicroserviceMQ(1,1,0.5,projectPrefix+projectLocation+"/", microservicesList,microservicesDependencies);
        System.out.println("Modularity of this MSA is "+MiMQ);
    }

    public double calculateMicroserviceMQ(int mode, int weightedMode, double lambda, String projectPath, List<String>  microservicesList,String microservicesDependencies){

        // 先读取每个微服务结构数据并计算其内聚
        double COH = 0, Coh_int =0, Coh_ent = 0;
        double Weight_int =0, Weight_ent = 0;
        for (String microserviceName: microservicesList){
            if (!microserviceMap.containsKey(microserviceName)){
                MiMQ.Microservice microservice = new MiMQ.Microservice(microserviceName);
                microserviceMap.put(microserviceName,microservice);
                microservice2IndexMap.put(microserviceName,microserviceIndex);
                index2MicroserviceMap.put(microserviceIndex++,microserviceName);
                readASingleMicroservice(microservice,projectPath+microserviceName+"_structure.xlsx");

                int index = microservice.getIndex();
                int interfaceIndex = microservice.getInterfaceIndex();

                //处理接口-类关系信息表
                double[][] relationship2 =new double[interfaceIndex][index];
                //处理类-类关系信息表
                double[][] relationship =new double[index][index];

                handleRelationshipMatrix(mode,projectPath+microserviceName+"_structure.xlsx",microservice, relationship,relationship2);

                double currentCoh_int = calculateMicroserviceInterfaceCohesion(index, interfaceIndex,relationship2);
                double currentCoh_ent = calculateMicroserviceClassCohesion(index,relationship);
                System.out.println("cohesion between interface-entity in microservice "+microserviceName+ " "+ currentCoh_int);
                System.out.println("cohesion between entity-entity in microservice "+microserviceName+ " "+ currentCoh_ent);
                System.out.println("cohesion in microservice "+microserviceName+ " "+ (currentCoh_int+currentCoh_ent) );

                if (weightedMode == 1){
                    currentCoh_int *= (interfaceIndex * index);
                    Weight_int += interfaceIndex * index;
                    currentCoh_ent *= (index * index);
                    Weight_ent += index * index;
                }
                Coh_int += currentCoh_int;
                Coh_ent += currentCoh_ent;
            }
        }
        if (weightedMode == 1){
            Coh_int = Coh_int/Weight_int;
            Coh_ent = Coh_ent/Weight_ent;
            COH = lambda*Coh_int + (1-lambda)*Coh_ent;
        }else{
            COH = lambda*Coh_int + (1-lambda)*Coh_ent;
            COH /= microserviceIndex;
        }

        System.out.println("cohesion of this MSA is "+COH);

        //再读取微服务之间的依赖数据并计算耦合
        double COP = calculateMicroservicesCoupling(mode,weightedMode,  projectPath+microservicesDependencies);

        System.out.println("coupling of this MSA is "+COP);
        return COH-COP;
    }

    public void readASingleMicroservice(MiMQ.Microservice microservice, String fileName){
        //读取类方法表信息
        List<List<String>> classMethodList=Util.readExcel(fileName,5,0);
        for(List<String> list:classMethodList){
            if(list.get(0).equals(""))
                continue;
            String className = list.get(0);
            microservice.addClass(className);

            if(list.get(1).equals(""))
                continue;
            MiMQ.Method method = new MiMQ.Method(list.get(1),list.get(2),list.get(3),list.get(4));
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
            MiMQ.Field field = new MiMQ.Field(list.get(1),list.get(2),list.get(3));
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
            MiMQ.Method method = new MiMQ.Method(list.get(1),list.get(2),list.get(3),list.get(4));
            microservice.addMethod2Interface(interfaceName,method);

        }
    }

    public double calculateMicroserviceInterfaceCohesion(int index, int interfaceIndex, double[][] relationship2){

        //计算接口与类体现的内聚
        if (interfaceIndex == 0) return 0;// 若服务接口数为0，则接口与类关系体现的内聚为0

        double int2clRelationshipCount = 0;
        for(int i=0;i<interfaceIndex;i++){
            for(int j=0;j<index;j++){
                int2clRelationshipCount+=relationship2[i][j];
            }
        }
        System.out.println("interface count:"+interfaceIndex);
        System.out.println("interface-class relationship count:"+int2clRelationshipCount);
        double MiCoh_int_ent = int2clRelationshipCount/(interfaceIndex*index);

        return MiCoh_int_ent;
    }

    public double calculateMicroserviceClassCohesion(int index, double[][] relationship){
        if (index == 0) return 0;// 若服务类数为0，则类与类关系体现的内聚为0

        //计算类与类体现的内聚
        double cl2clRelationshipCount = 0;
        for(int i=0;i<index;i++){
            for(int j=0;j<index;j++){
                cl2clRelationshipCount+=relationship[i][j];
            }
        }
        System.out.println("class count:"+index);
        System.out.println("class-class relationship count:"+cl2clRelationshipCount);
        double MiCoh_ent_ent = cl2clRelationshipCount/(index*index);

        return MiCoh_ent_ent;
    }

    public double calculateMicroservicesCoupling(int mode, int weightedMode, String microservicesDependencies){
        //处理微服务-微服务关系信息表
        double[][] relationship =new double[microserviceIndex][microserviceIndex];
        handleMicroservicesRelationship(mode, microservicesDependencies,relationship);

        double COP = 0;//所有服务间耦合的和
        double Weight_COP = 0;
        double cop = 0; //从服务i到服务j的耦合
        for(int i=0;i<microserviceIndex;i++){
            double COPofAMicroservice = 0;
            for(int j=0;j<microserviceIndex;j++){
                if(i != j){
                    int microservice1Class = microserviceMap.get(index2MicroserviceMap.get(i)).getIndex();
                    int microservice2Interface = microserviceMap.get(index2MicroserviceMap.get(j)).getInterfaceIndex();
                    if (microservice1Class == 0 || microservice2Interface == 0) cop =0;
                    else cop =(double)relationship[i][j]/(microservice1Class*microservice2Interface);
                    System.out.println("class-interface relationship count:"+relationship[i][j]);
                    System.out.println("the coupling between microservice "+ i + " and microservice "+j +" is: " +cop);
                    COPofAMicroservice += cop;
                    if (weightedMode == 1){
                        cop *= (microservice1Class*microservice2Interface);
                        Weight_COP += microservice1Class*microservice2Interface;
                    }
                    COP+=cop;
                }
            }
            System.out.println("the coupling from microservice " + i + ": "+ COPofAMicroservice/(microserviceIndex-1));
        }
        if (weightedMode == 1) {
            if (Weight_COP != 0)COP /= Weight_COP;
            COP *= 2;
        }else COP /= ((microserviceIndex)*(microserviceIndex-1)/2);

        return COP;
    }

    public void handleMicroservicesRelationship(int mode, String fileName, double[][] microservicesRelationship){
        Map<String, String> microRelationshipHashMap=new HashMap<>();

        List<List<String>> relationshipList = Util.readExcel(fileName,7,0);
        for(List<String> list:relationshipList){
            MiMQ.Microservice microservice1 = microserviceMap.get(list.get(0));
            MiMQ.Microservice microservice2 = microserviceMap.get(list.get(3));

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

        if (mode != 0){
            for(int i=0;i<microserviceIndex;i++){
                for(int j=0;j<microserviceIndex;j++){
                    if (i!=j){
                        if (microservicesRelationship[i][j]!=0) microservicesRelationship[i][j]=countMicroserviceClassInterfaceStd(i,j,fileName);
                    }
                }
            }
        }

    }

    private double countMicroserviceClassInterfaceStd(int microservice1Index, int microservice2Index, String fileName){
        MiMQ.Microservice oriMicroservice= microserviceMap.get(index2MicroserviceMap.get(microservice1Index));
        MiMQ.Microservice desMicroservice= microserviceMap.get(index2MicroserviceMap.get(microservice2Index));

        int classInMicroservice1 = oriMicroservice.getIndex();
        int interfaceInMicroservice2 = desMicroservice.getInterfaceIndex();
        double relationship[][] = new double[classInMicroservice1][interfaceInMicroservice2];

        Map<String, String> microRelationshipHashMap=new HashMap<>();
        List<List<String>> relationshipList = Util.readExcel(fileName,7,0);
        for(List<String> list:relationshipList) {
            // 过滤非业务微服务依赖关系
            if (!microserviceMap.containsKey(list.get(0)) || !microserviceMap.containsKey(list.get(3))) continue;
            MiMQ.Microservice microservice1 = microserviceMap.get(list.get(0));
            MiMQ.Microservice microservice2 = microserviceMap.get(list.get(3));

            if (microservice1 == null || microservice2 == null)
                continue;

            int index1 = microservice2IndexMap.get(list.get(0));
            if (index1 != microservice1Index) continue;
            int index2 = microservice2IndexMap.get(list.get(3));
            if (index2 != microservice2Index) continue;

            String microRelationship = list.get(0) +" "+ list.get(1) +" "+ list.get(2) + list.get(3) + list.get(4) + list.get(5);
            System.out.println(microRelationship);
            if (!microRelationshipHashMap.containsKey(microRelationship)){
                microRelationshipHashMap.put(microRelationship,microRelationship);

                int classIndex = microservice1.getClass2IndexMap().get(list.get(1));
                int interfaceIndex = microservice2.getInterface2IndexMap().get(list.get(4));

                relationship[classIndex][interfaceIndex]++;
            }
        }

        // update class-2-interface std relationship
        double relationshipStrengthInM1M2 = 0;
        for (int i = 0; i < classInMicroservice1; i++)
            for (int j = 0; j < interfaceInMicroservice2; j++){
                int methodCountInClass = oriMicroservice.getClassMap().get(oriMicroservice.getIndex2ClassMap().get(i)).getMethodCount();
                int operationCountInInterface = desMicroservice.getInterfaceMap().get(desMicroservice.getIndex2InterfaceMap().get(j)).getOperationCount();
                double denominator = methodCountInClass * operationCountInInterface;
                if (denominator != 0){
                    relationship[i][j] /= denominator;
                }
                relationshipStrengthInM1M2 += relationship[i][j];
            }

        return relationshipStrengthInM1M2;
    }

    public void handleRelationshipMatrix(int mode,String fileName,MiMQ.Microservice microservice, double[][] relationship,double[][] relationship2){
        int index = microservice.getIndex();
        int interfaceIndex = microservice.getInterfaceIndex();

        handleClassMethodRelationship(mode,microservice,fileName,relationship);
        handleClassFiledRelationship(mode,microservice,fileName,relationship);

        handleInterfaceMethodRelationship(mode,microservice,fileName,relationship2);
        handleInterfaceFieldRelationship(mode,microservice,fileName,relationship2);

        if (mode != 0){
            for(int i=0;i<interfaceIndex;i++){
                for(int j=0;j<index;j++){
                    if (relationship2[i][j]!=0) updateInterfaceStd(microservice,i,j,relationship2);
                }
            }

            for(int i=0;i<index;i++){
                for(int j=0;j<index;j++){
                    if (relationship[i][j]!=0) updateClassStd(microservice,i,j,relationship);
                }
            }
        }

    }

    public void handleClassMethodRelationship(int mode, MiMQ.Microservice microservice, String fileName,double[][] relationship){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,2);
        for(List<String> list:relationshipList){
            MiMQ.Class class1=microservice.getClassMap().get(list.get(0));
            MiMQ.Class class2=microservice.getClassMap().get(list.get(2));

            if(class1==null||class2==null)
                continue;

            int index1=microservice.getClass2IndexMap().get(list.get(0));
            int index2=microservice.getClass2IndexMap().get(list.get(2));

            // 判断是否是来自同一个类的调用
            if (index1 == index2) continue;

            if (mode == 0) relationship[index1][index2]=1;
            else relationship[index1][index2]++;
        }
    }

    public void handleClassFiledRelationship(int mode, MiMQ.Microservice microservice,String file,double[][] relationship){
        List<List<String>> relationshipList = Util.readExcel(file,5,3);
        for(List<String> list:relationshipList){
            MiMQ.Class class1= microservice.getClassMap().get(list.get(0));
            MiMQ.Class class2= microservice.getClassMap().get(list.get(2));
            if(class1==null||class2==null)
                continue;

            int index1=microservice.getClass2IndexMap().get(list.get(0));
            int index2=microservice.getClass2IndexMap().get(list.get(2));

            // 判断是否是来自同一个类的调用
            if (index1 == index2) continue;

            if (mode == 0) relationship[index1][index2]=1;
            else relationship[index1][index2]++;
        }
    }

    public void handleInterfaceMethodRelationship(int mode,MiMQ.Microservice microservice, String file,double[][] relationship){
        List<List<String>> relationshipList = Util.readExcel(file,5,5);

        for(List<String> list:relationshipList){
            MiMQ.Interface i = microservice.getInterfaceMap().get(list.get(0));
            MiMQ.Class c = microservice.getClassMap().get(list.get(2));

            if(i==null||c==null)
                continue;
//            if (!isMethodPublic(c.methodList, list.get(3))) {
//                //System.out.println("an invoked method of the interface is not public!");
//            }
            int index1=microservice.getInterface2IndexMap().get(list.get(0));
            int index2=microservice.getClass2IndexMap().get(list.get(2));
            if (mode == 0) relationship[index1][index2]=1;
            else relationship[index1][index2]++;
        }
    }

    public void handleInterfaceFieldRelationship(int mode,MiMQ.Microservice microservice, String file,double[][] relationship){
        List<List<String>> relationshipList = Util.readExcel(file,5,6);
        for(List<String> list:relationshipList){
            MiMQ.Interface i = microservice.getInterfaceMap().get(list.get(0));
            MiMQ.Class c = microservice.getClassMap().get(list.get(2));

            if(i!=null)
                System.out.println(list.get(0));
            if(c!=null)
                System.out.println(list.get(2));
            if(i==null||c==null)
                continue;

//            if (!isFiledPublic(c.fieldList, list.get(3))) {
//                //System.out.println("an invoked field of the interface is not public!");
//            }
            int index1 = microservice.getInterface2IndexMap().get(list.get(0));
            int index2 = microservice.getClass2IndexMap().get(list.get(2));
            if (mode == 0) relationship[index1][index2]=1;
            else relationship[index1][index2]++;
        }
    }

    private void updateInterfaceStd(MiMQ.Microservice microservice,int i,int j,double[][] relationship){
        MiMQ.Interface interface1=microservice.getInterfaceMap().get(microservice.getIndex2InterfaceMap().get(i));
        MiMQ.Class class2=microservice.getClassMap().get(microservice.getIndex2ClassMap().get(j));

        int denominator1=interface1.methodList.size()*(class2.getPublicUnit());
        if(denominator1>0)
            relationship[i][j]=relationship[i][j]/denominator1;
    }

    private void updateClassStd(MiMQ.Microservice microservice,int i,int j,double[][] relationship){
        MiMQ.Class class1=microservice.getClassMap().get(microservice.getIndex2ClassMap().get(i));
        MiMQ.Class class2=microservice.getClassMap().get(microservice.getIndex2ClassMap().get(j));

        boolean innerClassRelationship = false;

        if (class1.name.contains("$")){
            String class1Name = class1.name;
            String [] arrofClass1Name = class1Name.split("$",-1);

            for (int k = 0; k < arrofClass1Name.length - 1; k++) {
                if (arrofClass1Name[k] == class2.name) {
                    innerClassRelationship = true; break;
                }
            }
        }

        int denominator1 = 0;
        if (innerClassRelationship)
            denominator1=class1.methodList.size()*(class2.getAvailableUnitOfInnerClass());
        else denominator1=class1.methodList.size()*(class2.getPublicUnit());
        if(denominator1>0) {
            relationship[i][j] = relationship[i][j] / denominator1;
        }




    }

    private boolean isMethodPublic(List<MiMQ.Method> methodList, String methodName){
        for(MiMQ.Method m:methodList){
            if(m.name.equals(methodName)){
                return m.modifier.equals("public");
            }
        }
        return false;
    }

    private boolean isFiledPublic(List<MiMQ.Field> fieldList, String fieldName){
        for(MiMQ.Field f:fieldList){
            if(f.name.equals(fieldName))
                return f.modifier.equals("public");
        }
        return false;
    }

    private boolean isMethodProtected(List<MiMQ.Method> methodList, String methodName){
        for(MiMQ.Method m:methodList){
            if(m.name.equals(methodName)){
                return m.modifier.equals("protected");
            }
        }
        return false;
    }

    private boolean isFiledProtected(List<MiMQ.Field> fieldList, String fieldName){
        for(MiMQ.Field f:fieldList){
            if(f.name.equals(fieldName))
                return f.modifier.equals("protected");
        }
        return false;
    }

    class Microservice{
        String name;

        Map<String, MiMQ.Class> classMap;
        Map<String,Integer> class2IndexMap;
        Map<Integer,String> index2ClassMap;
        int index;

        Map<String, MiMQ.Interface> interfaceMap;
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

        public Map<Integer, String> getIndex2ClassMap() {
            return index2ClassMap;
        }

        public Map<Integer, String> getIndex2InterfaceMap() {
            return index2InterfaceMap;
        }

        public Map<String, MiMQ.Class> getClassMap() {
            return classMap;
        }

        public Map<String, Integer> getClass2IndexMap() {
            return class2IndexMap;
        }

        public Map<String, MiMQ.Interface> getInterfaceMap() {
            return interfaceMap;
        }

        public Map<String, Integer> getInterface2IndexMap() {
            return interface2IndexMap;
        }

        public void addClass(String className){
            if(!classMap.containsKey(className)){
                classMap.put(className,new MiMQ.Class(className));
                class2IndexMap.put(className,index);
                index2ClassMap.put(index++,className);
            }
        }

        public void addMethod2Class(String className, MiMQ.Method method){
            classMap.get(className).addMethod(method);
        }

        public void addField2Class(String className, MiMQ.Field field){
            classMap.get(className).addField(field);
        }

        public void addInterface(String interfaceName){
            if(!interfaceMap.containsKey(interfaceName)){
                interfaceMap.put(interfaceName,new MiMQ.Interface(interfaceName));
                interface2IndexMap.put(interfaceName,interfaceIndex);
                index2InterfaceMap.put(interfaceIndex++,interfaceName);
            }
        }

        public void addMethod2Interface(String interfaceName, MiMQ.Method method){
            interfaceMap.get(interfaceName).addMethod(method);
        }
    }

    class Class {
        String name;
        List<MiMQ.Field> fieldList;
        int publicUnitCount;
        int protectedUnitCount;
        int privateUnitCount;
        List<MiMQ.Method> methodList;

        public Class(String name) {
            this.name = name;
            this.fieldList = new ArrayList<>();
            this.publicUnitCount = 0;
            this.protectedUnitCount = 0;
            this.privateUnitCount = 0;
            this.methodList = new ArrayList<>();
        }

        public void addField(MiMQ.Field a) {
            fieldList.add(a);
            if (a.modifier.equals("public"))
                publicUnitCount++;
            if (a.modifier.equals("protected"))
                protectedUnitCount++;
            if (a.modifier.equals("private"))
                privateUnitCount++;

        }

        public void addMethod(MiMQ.Method a) {
            methodList.add(a);
            if (a.modifier.equals("public"))
                publicUnitCount++;
            if (a.modifier.equals("protected"))
                protectedUnitCount++;
            if (a.modifier.equals("private"))
                privateUnitCount++;
        }

        public int getMethodCount(){
            return this.methodList.size();
        }

        public int getPublicUnit(){
            return this.publicUnitCount;
        }

        public int getAvailableUnitOfInnerClass(){
            return this.publicUnitCount+this.protectedUnitCount+this.privateUnitCount;
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
        List<MiMQ.Method> methodList;
        int publicMethodCount;
        int protectedMethodCount;
        public Interface(String name){
            this.name = name;
            this.methodList = new ArrayList<>();
            this.publicMethodCount = 0; //
            this.protectedMethodCount = 0;
        }

        public void addMethod(MiMQ.Method m){
            methodList.add(m);
            if(m.modifier.equals("public"))
                publicMethodCount++;
            if(m.modifier.equals("protected"))
                protectedMethodCount++;
        }

        public int getOperationCount(){
            return this.methodList.size();
        }
    }

}
