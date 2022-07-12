import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PostProcessing {

    Map<String,PostProcessing.Architecture2RippleMapping> accumulatedMappingMap;


    public static void main(String[] args) throws IOException {
        String postprocessingPrefix = "postprocessing_data/";
        String projectsListFile = "projectsList.txt";

        BufferedReader br = new BufferedReader(new FileReader(postprocessingPrefix+projectsListFile));
        String projectLocation = "";
        while ((projectLocation=br.readLine())!=null){
            new PostProcessing().processingData(postprocessingPrefix+projectLocation,projectLocation);

        }

    }

    private PostProcessing(){
        accumulatedMappingMap = new HashMap<>();
    }

    public void processingData(String projectFilePath, String projectName) throws IOException {
        String dataMappingFile = projectFilePath+"/data_mapping.txt";
        BufferedReader curBr = new BufferedReader(new FileReader(dataMappingFile));

        Workbook workbook  = new XSSFWorkbook();
        // read data of one version
        String dataMapping = "";
        while((dataMapping = curBr.readLine())!=null){
            String [] dataMappingArray = dataMapping.split(",");
            Map<String,Architecture2RippleMapping> oneVersion = readOneVersionTwoMicro(projectFilePath,dataMappingArray[0],dataMappingArray[1]);
            // 将处理完的一个版本结果写入sheet
            printOneSheet(workbook,dataMappingArray[0],projectFilePath,oneVersion);
            accumulatingVersionsTwoMicro(oneVersion);
        }
        //计算各版本均值并将结果写入sheet
        computingAverage();
        printOneSheet(workbook,"average",projectFilePath,this.accumulatedMappingMap);
        Util.writeExcel(workbook,projectFilePath+"/"+projectName+"resultTwoMicro.xlsx");

    }

    private void accumulatingVersionsTwoMicro(Map<String,Architecture2RippleMapping> oneVersion){
        // 将当前版本微服务对数据加入到累计数据中，并记录每个微服务对的累计次数
        Iterator<String> iterator = oneVersion.keySet().iterator();
        while(iterator.hasNext()) {
            String microservicePair = iterator.next();
            Architecture2RippleMapping dataMapping = oneVersion.get(microservicePair);
            if(!Double.isNaN(dataMapping.ripple.CORE0) && !Double.isNaN(dataMapping.ripple.BORE0)){// 当仅当该微服务对ripple数据有效时，对其进行累计

                if(!this.accumulatedMappingMap.containsKey(microservicePair)){//如果历史中无该微服务对，直接记录它
                    dataMapping.accumulatedVersion++;
                    this.accumulatedMappingMap.put(microservicePair,dataMapping);
                }else{//如果历史中已存在该微服务对，累计当前版本值
                    Architecture2RippleMapping accumulatedDataMapping = this.accumulatedMappingMap.get(microservicePair);
                    accumulatedDataMapping.accumulatedVersion++;
                    accumulatedDataMapping.architecture.accumulatingArchitectureTwoMicro(dataMapping.architecture);
                    accumulatedDataMapping.ripple.accumulatingRippleTwoMicro(dataMapping.ripple);
                }
            }
        }
        
    }

    private void computingAverage(){
        Iterator<String> iterator = this.accumulatedMappingMap.keySet().iterator();
        while(iterator.hasNext()) {
            String microservicePair = iterator.next();
            Architecture2RippleMapping dataMapping = this.accumulatedMappingMap.get(microservicePair);
            dataMapping.computingAverage();
        }
    }

    private void printOneSheet(Workbook workbook, String sheetName, String projectFilePath,Map<String, Architecture2RippleMapping> oneVersion){
        Sheet sheet = Util.createSheetTwoMicro(workbook,sheetName);
        int rowIndex = 0;

        Iterator<String> iterator = oneVersion.keySet().iterator();
        while(iterator.hasNext()) {
            String microservicePair = iterator.next();
            Architecture2RippleMapping dataMapping = oneVersion.get(microservicePair);
            String [] microserviceArray = microservicePair.split(",");
            Util.writeMicroserviceToSheetTwoMicro(sheet, rowIndex++, microserviceArray[0],microserviceArray[1],
                    dataMapping.architecture, dataMapping.ripple);

        }
    }

    private Map<String,Architecture2RippleMapping> readOneVersionTwoMicro(String projectFilePath,String architectureFile, String rippleFile){
        Map<String,PostProcessing.ArchitectureTwoMicro> architectureTwoMicroMap = new HashMap<>();
        Map<String,PostProcessing.RippleTwoMicro> rippleTwoMicroMap = new HashMap<>();

        // read architecture data
        List<List<String>> architectureDataList = Util.readExcel(projectFilePath+"/"+architectureFile+".xlsx","sheet1",10);
        for(List<String> data:architectureDataList){
            String microservice1 = data.get(0);
            String microservice2 = data.get(1);

            architectureTwoMicroMap.put(microservice1+","+microservice2,new ArchitectureTwoMicro(
                    processingNan(data.get(2)),processingNan(data.get(3)),
                    processingNan(data.get(4)),processingNan(data.get(5)),
                    processingNan(data.get(6)),processingNan(data.get(7)),
                    processingNan(data.get(8)),processingNan(data.get(9))));
        }

        // read ripple data
        List<List<String>> rippleDataList = Util.readExcel(projectFilePath+"/"+rippleFile+".xlsx","Sheet1",10);
        for(List<String> data:rippleDataList) {
            String microserviceDst = data.get(0);
            String microserviceOrg = data.get(1);

            rippleTwoMicroMap.put(microserviceOrg+","+microserviceDst, new RippleTwoMicro(
                    processingNan(data.get(2)),processingNan(data.get(3)),
                    processingNan(data.get(4)),processingNan(data.get(5)),
                    processingNan(data.get(6)),processingNan(data.get(7)),
                    processingNan(data.get(8)),processingNan(data.get(9))
            ));
        }

        Map<String,Architecture2RippleMapping> dataMapping = new HashMap<>();
        // only count the microservice-pair that exists in the architecture data
//        Iterator<String> iterator = architectureTwoMicroMap.keySet().iterator();
        // for each microservice-pair with ripple effect data
        Iterator<String> iterator = rippleTwoMicroMap.keySet().iterator();
        while(iterator.hasNext()){
            String microservicePair = iterator.next();
            if(architectureTwoMicroMap.containsKey(microservicePair)){
                dataMapping.put(microservicePair,new Architecture2RippleMapping(
                        architectureTwoMicroMap.get(microservicePair),rippleTwoMicroMap.get(microservicePair)));
            }
        }

        return dataMapping;
    }

    private Double processingNan(String rawData){
        if(rawData.equals("NAN"))return Double.NaN; // 将所有未观察到同时变更的微服务对ORE值设置为0
        else return Double.parseDouble(rawData);
    }


    class ArchitectureTwoMicro{
        double EntitiesOfOrg;
        double InterfaceOfOrg;
        double EntitiesOfDst;
        double InterfaceOfDst;
        double ACT;
        double CaT;



        public double getACT() {
            return ACT;
        }

        public double getCaT() {
            return CaT;
        }

        public double getCeT() {
            return CeT;
        }

        public double getMIF() {
            return MIF;
        }

        double CeT;
        double MIF;

        public ArchitectureTwoMicro(double EntitiesOfOrg, double InterfaceOfOrg,double EntitiesOfDst, double InterfaceOfDst,
                                    double ACT, double CaT, double CeT,double MIF){
            this.EntitiesOfOrg = EntitiesOfOrg;
            this.InterfaceOfDst = InterfaceOfDst;
            this.InterfaceOfOrg = InterfaceOfOrg;
            this.EntitiesOfDst = EntitiesOfDst;
            this.ACT = ACT;
            this.CaT = CaT;
            this.CeT = CeT;
            this.MIF = MIF;
        }

        public void accumulatingArchitectureTwoMicro(ArchitectureTwoMicro another){
            this.InterfaceOfDst += another.InterfaceOfDst;
            this.EntitiesOfOrg += another.EntitiesOfOrg;
            this.InterfaceOfOrg += another.InterfaceOfOrg;
            this.EntitiesOfDst += another.EntitiesOfDst;
            this.ACT += another.ACT;
            this.CaT += another.CaT;
            this.CeT += another.CeT;
            this.MIF += another.MIF;
        }

        public void computingAverage(int number){
            this.InterfaceOfDst /= number;
            this.EntitiesOfOrg /= number;
            this.InterfaceOfOrg /= number;
            this.EntitiesOfDst /= number;
            this.ACT /= number;
            this.CaT /= number;
            this.CeT /= number;
            this.MIF /= number;
        }

    }

    class RippleTwoMicro{
        double BORE0;
        double BORE1;
        double BORE2;
        double BORE3;

        public double getBORE0() {
            return BORE0;
        }

        public double getBORE1() {
            return BORE1;
        }

        public double getBORE2() {
            return BORE2;
        }

        public double getBORE3() {
            return BORE3;
        }

        public double getCORE0() {
            return CORE0;
        }

        public double getCORE1() {
            return CORE1;
        }

        public double getCORE2() {
            return CORE2;
        }

        public double getCORE3() {
            return CORE3;
        }

        double CORE0;
        double CORE1;
        double CORE2;
        double CORE3;

        public RippleTwoMicro(double BORE0, double CORE0, double BORE1,double CORE1,
                              double BORE2,double CORE2,double BORE3,double CORE3){
            this.BORE0 = BORE0;
            this.BORE1 = BORE1;
            this.BORE2 = BORE2;
            this.BORE3 = BORE3;
            this.CORE0 = CORE0;
            this.CORE1 = CORE1;
            this.CORE2 = CORE2;
            this.CORE3 = CORE3;
        }

        public void accumulatingRippleTwoMicro(RippleTwoMicro another){
            this.BORE0 += another.BORE0;
            this.BORE1 += another.BORE1;
            this.BORE2 += another.BORE2;
            this.BORE3 += another.BORE3;
            this.CORE0 += another.CORE0;
            this.CORE1 += another.CORE1;
            this.CORE2 += another.CORE2;
            this.CORE3 += another.CORE3;
        }

        public void computingAverage(int number){
            this.BORE0 /= number;
            this.BORE1 /= number;
            this.BORE2 /= number;
            this.BORE3 /= number;
            this.CORE0 /= number;
            this.CORE1 /= number;
            this.CORE2 /= number;
            this.CORE3 /= number;
        }
    }

    class Architecture2RippleMapping{
        ArchitectureTwoMicro architecture;
        RippleTwoMicro ripple;
        int accumulatedVersion;

        public Architecture2RippleMapping(ArchitectureTwoMicro architecture, RippleTwoMicro ripple){
            this.architecture = architecture;
            this.ripple = ripple;
            this.accumulatedVersion = 0;
        }

        public void computingAverage(){

            this.architecture.computingAverage(this.accumulatedVersion);
            this.ripple.computingAverage(this.accumulatedVersion);
        }
    }

}
