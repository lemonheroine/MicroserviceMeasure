import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SSC {

    public static void main(String[] args){
        new SSC().caculate("mall-swarm-mall-auth.xlsx");
    }

    public double caculate(String fileName){
        //读取类的信息
        List<Class> classList=new ArrayList<>();
        Map<String,Class> classMap = new HashMap<>();
        readClassInfo(fileName,0,classList,classMap);
//        for(Class c:classList){
//            System.out.println(c.name);
//            for(Method o:c.methodList){
//                System.out.println("    "+o.vector+o.methodName);
//            }
//        }
        double loc1= getSSCValue(classList);
        System.out.println("class loc1 is: "+loc1);

        //读取接口的信息
        List<Class> interfaceList=new ArrayList<>();
        Map<String,Class> interfaceMap = new HashMap<>();
        readClassInfo(fileName,1,interfaceList,interfaceMap);
        double loc2= getSSCValue(interfaceList);
        System.out.println("interface loc1 is: "+loc2);

        System.out.println("the value of SSC is: "+(loc1+loc2)/2);
        return (loc1+loc2)/2;
    }

    private double getSSCValue(List<Class> list){
        double classValue = 0.0;
        for(int i=0;i<list.size();i++){
            for(int j=i+1;j<list.size();j++){
                classValue+=getSSCValueBetweenClass(list.get(i),list.get(j));
            }
        }
        return 1-classValue/list.size()/(list.size()-1)*2;
    }

    private double getSSCValueBetweenClass(Class a,Class b){
        double value =0.0;
        for(Method m1:a.methodList){
            for(Method m2:b.methodList){
                //这种情况说明数据出了问题
                if(m1.vector.size()!=m2.vector.size())
                    continue;

                double multiply = 0;
                for(int i=0;i<m1.vector.size();i++){
                    multiply+=m1.vector.get(i)*m2.vector.get(i);
                }

                if(m1.vectorValue==0||m2.vectorValue==0){
                    value+=0;
                    continue;
                }

                multiply/=Math.sqrt(m1.vectorValue*m2.vectorValue);
                value +=multiply;
            }
        }
        return value/a.methodList.size()/b.methodList.size();
    }

    //读取类的信息
    private void readClassInfo(String fileName,int sheetIndex,List<Class> l,Map<String,Class> m){
        List<List<String>> infoList=Util.readExcel(fileName,4,sheetIndex);
        for(List<String> list:infoList){
            //类名为空的情况
            if(list.get(0).equals(""))
                continue;

            String className = list.get(0);
            if(!m.containsKey(className)){
                m.put(className,new Class(className));
                l.add(m.get(className));
            }

            //方法名为空的情况
            if(list.get(1).equals(""))
                continue;
            Method method = new Method(list.get(1),list.get(2),list.get(3));
            m.get(className).methodList.add(method);
        }
    }

    class Class{
        String name;
        List<Method> methodList;
        public Class(String name){
            this.name = name;
            this.methodList = new ArrayList<>();
        }
    }

    class Method{
        String methodName;
        String textList;
        List<Integer> vector;
        int vectorValue ;

        public Method(String methodName,String textList,String vector){
            this.methodName = methodName;
            this.textList = textList;
            this.vector = new ArrayList<>();
            this.vectorValue = 0;
            if(vector.length()>0&&vector.charAt(0)=='[')
                vector=vector.substring(1);
            if(vector.length()>0&&vector.charAt(vector.charAt(vector.length()-1))==']')
                vector = vector.substring(0,vector.length()-1);
            if(vector.length()>0){
                for(String s:vector.split(",")){
                    if(s.length()>0&&s.charAt(0)==' ')
                        s=s.substring(1);
                    if(s.equals("0"))
                        this.vector.add(0);
                    if(s.equals("1")) {
                        this.vector.add(1);
                        this.vectorValue ++;
                    }
                }
            }
        }
    }
}
