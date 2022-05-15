import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ESC {

    public static void main(String[] args){
        new ESC().caculate("exercise.xlsx","exercise.xlsx");
    }

    //需要传入两个参数，一个是ESC文件参数，一个是SDC文件参数
    public double caculate(String fileName,String fileName2){
        Map<String,Integer> classMap=new HashMap<>();
        List<Class> classList=new ArrayList<>();
        readClassInfo(fileName2,classMap,classList);
        int[][] classRelationship=new int[classList.size()][classList.size()];
        readClassRelationship(fileName,classMap,classRelationship);
        double loc1=getClassESC(classList,classRelationship);
        System.out.println("class loc is: "+loc1);

        //读取接口的信息
        Map<String,Integer> interfaceMap=new HashMap<>();
        List<Interface> interfaceList = new ArrayList<>();
        readInterfaceInfo(fileName2,interfaceMap,interfaceList);
        int[][] interfaceRelationship=new int[interfaceList.size()][interfaceList.size()];
        readInterfaceRelationship(fileName,interfaceMap,interfaceRelationship);
        double loc2=getInterfaceESCValue(interfaceList,interfaceRelationship);
        System.out.println("interface loc is: "+loc2);

        System.out.println("the value of");
        return (loc1+loc2)/2;
    }

    private double getInterfaceESCValue(List<Interface> list,int[][] relationship){
        double interfaceValue =0.0;
        for(int i=0;i<list.size();i++){
            for(int j=i+1;j<list.size();j++){
                int count = list.get(i).method.size()*list.get(j).method.size();
                if(count==0)
                    continue;
                interfaceValue += 1.0*relationship[i][j]/count;
            }
        }

        return 1-interfaceValue/list.size()/(list.size()-1)*2;
    }

    private double getClassESC(List<Class> list,int[][] relationship){
        double classValue = 0.0;
        for(int i=0;i<list.size();i++){
            for(int j=i+1;j<list.size();j++){
                int count = getMethodAndFieldCount(list.get(i))*getMethodAndFieldCount(list.get(j));
                if(count>0)
                    classValue += 1.0*relationship[i][j]/count;
            }
        }

        return 1-classValue/list.size()/(list.size()-1)*2;
    }

    private int getMethodAndFieldCount(Class a){
        return a.field.size()+a.method.size();
    }

    private void readInterfaceInfo(String fileName,
                                         Map<String,Integer> map,
                                            List<Interface> list){
        List<List<String>> methodList=Util.readExcel(fileName,5,4);
        int i=0;
        for(List<String> l:methodList){
            if(l.get(0).equals(""))
                continue;

            if(!map.containsKey(l.get(0))){
                map.put(l.get(0),i++);
                list.add(new Interface(l.get(0)));
            }

            if(!l.get(1).equals("")){
                list.get((map.get(l.get(0)))).method.add(l.get(1));
            }
        }
    }

    private void readClassRelationship(String fileName,Map<String,Integer> map,int[][] relationship){
        List<List<String>> classRelationship =Util.readExcel(fileName,7,0);
        for(List<String> l:classRelationship){
            if(!collectFormat(l))
                continue;

            if(map.containsKey(l.get(1))&&map.containsKey(l.get(4))){
                relationship[map.get(l.get(1))][map.get(l.get(4))]++;
                relationship[map.get(l.get(4))][map.get(l.get(1))]++;
            }
        }
    }

    private void readInterfaceRelationship(String fileName,Map<String,Integer> map,int[][] relationship){
        List<List<String>> interfaceRelationship =Util.readExcel(fileName,7,2);
        for(List<String> l:interfaceRelationship){
            if(!collectFormat(l))
                continue;

            if(map.containsKey(l.get(1))&&map.containsKey(l.get(4))){
                relationship[map.get(l.get(1))][map.get(l.get(4))]++;
                relationship[map.get(l.get(4))][map.get(l.get(1))]++;
            }
        }
    }


    private void readClassInfo(String fileName,Map<String,Integer> map,List<Class> list){
        int index=0;
        List<List<String>> methodList=Util.readExcel(fileName,5,0);
        for(List<String> l:methodList){
            //类名为空的情况直接跳过
            if(l.get(0).equals(""))
                continue;

            if(!map.containsKey(l.get(0))){
                map.put(l.get(0),index++);
                list.add(new Class(l.get(0)));
            }

            Class c=list.get(map.get(l.get(0)));
            if(l.get(1).equals(""))
                continue;
            c.method.add(l.get(1));
        }

        List<List<String>> field=Util.readExcel(fileName,4,1);
        for(List<String> l:field){
            if(l.get(0).equals(""))
                continue;

            if(!map.containsKey(l.get(0))){
                map.put(l.get(0),index++);
                list.add(new Class(l.get(0)));
            }

            Class c=list.get(map.get(l.get(0)));
            if(l.get(1).equals(""))
                continue;
            c.field.add(l.get(1));
        }
    }

    private int[][] readClassInfo(String fileName, int sheetIndex,
                                            Map<String,Integer> index,
                                            Map<String,Interface> map,
                                            List<String> name){
        List<List<String>> infoList=Util.readExcel(fileName,7,2);
        int i=map.size();
        for(List<String> list:infoList){
            if(!collectFormat(list))
                continue;

            String interface1=list.get(0)+list.get(1);
            String interface2=list.get(3)+list.get(4);

            if(!index.containsKey(interface1)){
                index.put(interface1,i++);
                map.put(interface1,new Interface(interface1));
                name.add(interface1);
            }
            if(!index.containsKey(interface2)){
                index.put(interface2,i++);
                map.put(interface2,new Interface(interface2));
                name.add(interface2);
            }

            map.get(interface1).method.add(list.get(2));
            map.get(interface2).method.add(list.get(5));
        }

        int[][] relationship=new int[i][i];
        for(List<String> list:infoList){
            if(!collectFormat(list))
                continue;

            String interface1=list.get(0)+list.get(1);
            String interface2=list.get(3)+list.get(4);

            int i1=index.get(interface1);
            int i2=index.get(interface2);
            relationship[i1][i2]++;
            relationship[i2][i1]++;
        }

        return relationship;
    }

    private boolean collectFormat(List<String> list){
        if(list.size()!=7)
            return false;
        for(String cur:list){
            if(cur.equals(""))
                return false;
        }
        return true;
    }

    class Interface{
        String name;
        List<String> method;

        public Interface(String name){
            this.name=name;
            this.method=new ArrayList<>();
        }
    }

    class Class{
        String name;
        List<String> method;
        List<String> field;

        public Class(String name){
            this.name = name;
            this.method = new ArrayList<>();
            this.field = new ArrayList<>();
        }
    }
}
