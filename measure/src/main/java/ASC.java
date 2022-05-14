import java.util.*;

public class ASC {

    public final static String[] removeTypeList = new String[]{"byte",
    "short","int","long","float","double","char","boolean"};

    public static void main(String[] args){
        new ASC().caculate("exercise.xlsx");
    }

    public double caculate(String fileName){
        //首先读取类的信息
        List<Class> classList=new ArrayList<>();
        Map<String,Class> classMap=new HashMap<>();
        readClassMethodInfo(fileName,classList,classMap);

        double classValue = 0.0;
        for(int i=0;i<classList.size();i++){
            for(int j=i+1;j<classList.size();j++){
                classValue += getValueBetweenClass(classList.get(i),classList.get(j));
            }
        }

        double loc1 = 1-classValue/(classList.size())/(classList.size()-1)*2;
        System.out.println("class loc is: "+loc1);

        //其次读取接口的信息
        List<Interface> interfaceList=new ArrayList<>();
        Map<String,Interface> interfaceMap=new HashMap<>();
        readInterfaceMethodInfo(fileName,interfaceList,interfaceMap);

        double interfaceValue = 0.0;
        for(int i=0;i<interfaceList.size();i++){
            for(int j=i+1;j<interfaceList.size();j++){
                interfaceValue += getValueBetweenInterface(interfaceList.get(i),interfaceList.get(j));
            }
        }

        double loc2 = 1-interfaceValue/interfaceList.size()/(interfaceList.size()-1)*2;
        System.out.println("interface loc is: "+loc2);

        System.out.println("the value of ASC is: "+(loc1+loc2)/2);
        return (loc1+loc2)/2;
    }

    private double getValueBetweenClass(Class a,Class b){
        double value = 0.0;
        for(Method m1:a.methodList){
            for(Method m2:b.methodList){
                value+=getValueOfCollection(m1.typeSet,m2.typeSet);
            }
        }
        return value/a.methodList.size()/b.methodList.size();
    }

    private double getValueBetweenInterface(Interface a,Interface b){
        double value = 0.0;
        for(Method m1:a.methodList){
            for(Method m2:b.methodList){
                value+=getValueOfCollection(m1.typeSet,m2.typeSet);
            }
        }
        return value/a.methodList.size()/b.methodList.size();
    }


    private double getValueOfCollection(Set<String> a,Set<String> b){
        Set<String> union=new HashSet<>();
        int intersection=0;
        union.addAll(a);
        union.addAll(b);

        for(String s:a){
            if(b.contains(s))
                intersection++;
        }

        if(union.size()==0)
            return 0;
        return intersection*1.0/union.size();
    }

    private void readClassMethodInfo(String fileName,
                                     List<Class> list,Map<String,Class> map){
        List<List<String>> infoList=Util.readExcel(fileName,5,0);
        for(List<String> l:infoList){
            //类名缺失
            if(l.get(0).equals(""))
                continue;
            String className = l.get(0);
            if(!map.containsKey(className)){
                Class c=new Class(className);
                map.put(className,c);
                list.add(c);
            }

            //方法名缺失
            if(l.get(1).equals(""))
                continue;
            Method method=new Method(l.get(1),l.get(2),l.get(3),l.get(4));
            map.get(className).addMethod(method);
        }
    }

    private void readInterfaceMethodInfo(String fileName,
                                     List<Interface> list,Map<String,Interface> map){
        List<List<String>> infoList=Util.readExcel(fileName,4,1);
        for(List<String> l:infoList){
            //接口名缺失
            if(l.get(0).equals(""))
                continue;
            String interfaceName = l.get(0);
            if(!map.containsKey(interfaceName)){
                Interface i=new Interface(interfaceName);
                map.put(interfaceName,i);
                list.add(i);
            }

            //方法名缺失
            if(l.get(1).equals(""))
                continue;
            //接口不包含fieldname字段
            Method method=new Method(l.get(1),l.get(2),l.get(3),"");
            map.get(interfaceName).addMethod(method);
        }
    }


    class Class{
        String name;
        List<Method> methodList;

        public Class(String name){
            this.name = name;
            this.methodList=new ArrayList<>();
        }

        private void addMethod(Method m){
            this.methodList.add(m);
        }
    }

    class Interface{
        String name;
        List<Method> methodList;

        public Interface(String name){
            this.methodList=new ArrayList<>();
            this.name = name;
        }

        private void addMethod(Method m){
            this.methodList.add(m);
        }
    }

    class Method{
        String methodName;
        String parameterName;
        String returnTypeName;
        String fieldName;
        Set<String> typeSet=new HashSet<>();

        public Method(String methodName,String parameterName,String returnTypeName,String fieldName){
            this.methodName = methodName;
            this.parameterName = parameterName;
            this.returnTypeName = returnTypeName;
            this.fieldName = fieldName;

            addType(parameterName);
            addType(fieldName);
            addType(returnTypeName);

            removeBasicType();
        }

        private void addType(String cur){
            if(cur.length()==0)
                return;
            Collections.addAll(typeSet, cur.split(","));
        }

        private void removeBasicType(){
            for(String basicType:removeTypeList)
                typeSet.remove(basicType);
        }
    }
}
