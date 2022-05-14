import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ESC {

    public static void main(String[] args){
        new ESC().caculate("exercise.xlsx");
    }

    public double caculate(String fileName){
        double loc1=0.0;
        System.out.println("class loc is: "+loc1);

        //读取接口的信息
        Map<String,Integer> interfaceIndex=new HashMap<>();
        Map<String,Interface> interfaceMap=new HashMap<>();
        List<String> interfaceName = new ArrayList<>();
        int[][] interfaceRelationship = readInterfaceInfo(fileName,2,interfaceIndex
        ,interfaceMap,interfaceName);
        double loc2=getInterfaceESCValue(interfaceIndex,interfaceMap,interfaceName,interfaceRelationship);
        System.out.println("interface loc is: "+loc2);

        System.out.println("the value of");
        return (loc1+loc2)/2;
    }

    private double getInterfaceESCValue(Map<String,Integer> index,
                                        Map<String,Interface> map,List<String> name,
                                        int[][] relationship){
        double interfaceValue =0.0;
        for(int i=0;i<name.size();i++){
            for(int j=i+1;j<name.size();j++){
                Interface i1=map.get(name.get(i));
                Interface i2=map.get(name.get(j));

                if(i1.method.size()==0||i2.method.size()==0)
                    continue;
                interfaceValue += 1.0*relationship[index.get(name.get(i))][index.get(name.get(j))]
                        /i1.method.size()/i2.method.size();
            }
        }

        return 1-interfaceValue/name.size()/(name.size()-1)*2;
    }

    private int[][] readInterfaceInfo(String fileName, int sheetIndex,
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
    }
}
