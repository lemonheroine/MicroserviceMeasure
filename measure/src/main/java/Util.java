import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Util {

    //输入excel文件的名称，sheet编号以及读取的列数
    public static List<List<String>> readExcel(String filePath, int line,int index){
        Workbook workbook = readExcel(filePath);

        //Sheet sheet = workbook.getSheetAt(index);
        //通过sheet的名称获取表格
        Sheet sheet = workbook.getSheet("data"+(index+1));
        List<List<String>> list=new ArrayList<>();


        for(int j=sheet.getFirstRowNum()+1;j<=sheet.getLastRowNum();j++){
            Row cur = sheet.getRow(j);
            if(cur==null)
                continue;
            List<String> curList=new ArrayList<>();
            for(int k=0;k<line;k++) {
                if(cur.getCell(k)==null) {
                    curList.add("");
                }
                else
                    curList.add(cur.getCell(k).toString());
            }

            list.add(curList);
        }
        return list;
    }

    public static List<List<String>> readExcel(String filePath, String sheetName, int line){
        System.out.println("filePath: "+ filePath);
        Workbook workbook = readExcel(filePath);

        //通过sheet的名称获取表格
        Sheet sheet = workbook.getSheet(sheetName);
        List<List<String>> list=new ArrayList<>();


        for(int j=sheet.getFirstRowNum()+1;j<=sheet.getLastRowNum();j++){
            Row cur = sheet.getRow(j);
            if(cur==null)
                continue;
            List<String> curList=new ArrayList<>();
            for(int k=0;k<line;k++) {
                if(cur.getCell(k)==null) {
                    curList.add("");
                }
                else
                    curList.add(cur.getCell(k).toString());
            }

            list.add(curList);
        }
        return list;
    }

    private static Workbook readExcel(String filePath) {
        if (filePath == null) {
            return null;
        }
        String extString = filePath.substring(filePath.lastIndexOf("."));

        try {
            InputStream is = new FileInputStream(filePath);
            if (".xls".equals(extString)) {
                return new HSSFWorkbook(is);
            } else if (".xlsx".equals(extString)) {
                return new XSSFWorkbook(is);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Sheet createSheet1(Workbook workbook){
        Sheet sheet = workbook.createSheet("sheet1");
        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex);
        titleRow.createCell(0).setCellValue("orgMicroservice");
        titleRow.createCell(1).setCellValue("dstMicroservice");
        titleRow.createCell(2).setCellValue("EntitiesOfOrg"); // dst microservice is the one being depended on
        titleRow.createCell(3).setCellValue("InterfaceOfOrg");
        titleRow.createCell(4).setCellValue("EntitiesOfDst"); // dst microservice is the one being depended on
        titleRow.createCell(5).setCellValue("InterfaceOfDst");
        titleRow.createCell(6).setCellValue("ACT");
        titleRow.createCell(7).setCellValue("CaT");
        titleRow.createCell(8).setCellValue("CeT");
        titleRow.createCell(9).setCellValue("MIF");

        return sheet;
    }

    public static Sheet createSheet2(Workbook workbook){
        Sheet sheet = workbook.createSheet("sheet2");
        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex);
        titleRow.createCell(0).setCellValue("Microservice");
        titleRow.createCell(1).setCellValue("Entities");
        titleRow.createCell(2).setCellValue("Interface");
        titleRow.createCell(3).setCellValue("AIS");
        titleRow.createCell(4).setCellValue("ADS");
        titleRow.createCell(5).setCellValue("Ca");
        titleRow.createCell(6).setCellValue("Ce");
        titleRow.createCell(7).setCellValue("MIFa");
        titleRow.createCell(8).setCellValue("MIFe");
//        titleRow.createCell(10).setCellValue("MIFetestB");
//        titleRow.createCell(11).setCellValue("MIFetestb");

        return sheet;
    }

    public static Sheet createSheetTwoMicro(Workbook workbook, String sheetName){
        Sheet sheet = workbook.createSheet(sheetName);
        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex);
        titleRow.createCell(0).setCellValue("orgMicroservice");
        titleRow.createCell(1).setCellValue("dstMicroservice");
        titleRow.createCell(2).setCellValue("EntitiesOfOrg");
        titleRow.createCell(3).setCellValue("InterfaceOfOrg"); // dst microservice is the one being depended on
        titleRow.createCell(4).setCellValue("EntitiesOfDst");
        titleRow.createCell(5).setCellValue("InterfaceOfDst"); // dst microservice is the one being depended on
        titleRow.createCell(6).setCellValue("ACT");
        titleRow.createCell(7).setCellValue("CaT");
        titleRow.createCell(8).setCellValue("CeT");
        titleRow.createCell(9).setCellValue("MIF");
        titleRow.createCell(10).setCellValue("BORE(t=0)");
        titleRow.createCell(11).setCellValue("CORE(t=0)");
        titleRow.createCell(12).setCellValue("BORE(t=1)");
        titleRow.createCell(13).setCellValue("CORE(t=1)");
        titleRow.createCell(14).setCellValue("BORE(t=2)");
        titleRow.createCell(15).setCellValue("CORE(t=2)");
        titleRow.createCell(16).setCellValue("BORE(t=3)");
        titleRow.createCell(17).setCellValue("CORE(t=3)");

        return sheet;
    }

    public static Sheet createDPMResult(Workbook workbook, String sheetName){
        Sheet sheet = workbook.createSheet(sheetName);
        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex);
        titleRow.createCell(0).setCellValue("projectName");
        titleRow.createCell(1).setCellValue("microservicesCount");

        titleRow.createCell(2).setCellValue("distCP2Microservices");
        titleRow.createCell(3).setCellValue("distCVMCI");
        titleRow.createCell(4).setCellValue("distCVCaT");
        titleRow.createCell(5).setCellValue("distCVCeT");
        titleRow.createCell(6).setCellValue("distCVACT");

        titleRow.createCell(7).setCellValue("DPM_MCI");
        titleRow.createCell(8).setCellValue("DPM_CaT");
        titleRow.createCell(9).setCellValue("DPM_CeT");
        titleRow.createCell(10).setCellValue("DPM_ACT");

        titleRow.createCell(11).setCellValue("distCPAfferent");
        titleRow.createCell(12).setCellValue("distCVaMCI");
        titleRow.createCell(13).setCellValue("distCVCa");
        titleRow.createCell(14).setCellValue("distCVAIS");

        titleRow.createCell(15).setCellValue("DPM_aMCI");
        titleRow.createCell(16).setCellValue("DPM_Ca");
        titleRow.createCell(17).setCellValue("DPM_AIS");

        titleRow.createCell(18).setCellValue("distCPEfferent");
        titleRow.createCell(19).setCellValue("distCVeMCI");
        titleRow.createCell(20).setCellValue("distCVCe");
        titleRow.createCell(21).setCellValue("distCVADS");

        titleRow.createCell(22).setCellValue("DPM_eMCI");
        titleRow.createCell(23).setCellValue("DPM_Ce");
        titleRow.createCell(24).setCellValue("DPM_ADS");

        return sheet;
    }

    public static Sheet createSimulationResult(Workbook workbook, String sheetName){
        Sheet sheet = workbook.createSheet(sheetName);
        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex);
        titleRow.createCell(0).setCellValue("callarEntityNumber");
        titleRow.createCell(1).setCellValue("calleeInterfaceNumber");
        titleRow.createCell(2).setCellValue("ACT");
        titleRow.createCell(3).setCellValue("CaT");
        titleRow.createCell(4).setCellValue("CeT");
        titleRow.createCell(5).setCellValue("MCI");
        titleRow.createCell(6).setCellValue("DCF");
        titleRow.createCell(7).setCellValue("DCS");
        titleRow.createCell(8).setCellValue("ICF");
        titleRow.createCell(9).setCellValue("ICS");
        titleRow.createCell(10).setCellValue("OCF");
        titleRow.createCell(11).setCellValue("OCS");

        return sheet;
    }

    public static Sheet createSimulationAfferentResult(Workbook workbook, String sheetName){
        Sheet sheet = workbook.createSheet(sheetName);
        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex);
        titleRow.createCell(0).setCellValue("calleeInterfaceNumber");
        titleRow.createCell(1).setCellValue("AIS");
        titleRow.createCell(2).setCellValue("Ca");
        titleRow.createCell(3).setCellValue("MCIor");
        titleRow.createCell(4).setCellValue("DCF");
        titleRow.createCell(5).setCellValue("DCS");
        titleRow.createCell(6).setCellValue("ICF");
        titleRow.createCell(7).setCellValue("ICS");
        titleRow.createCell(8).setCellValue("OCF");
        titleRow.createCell(9).setCellValue("OCS");

        return sheet;
    }


    public static Sheet createSimulationEfferentResult(Workbook workbook, String sheetName){
        Sheet sheet = workbook.createSheet(sheetName);
        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex);
        titleRow.createCell(0).setCellValue("callarEntityNumber");
        titleRow.createCell(1).setCellValue("ADS");
        titleRow.createCell(2).setCellValue("Ce");
        titleRow.createCell(3).setCellValue("MCIee");
        titleRow.createCell(4).setCellValue("DCF");
        titleRow.createCell(5).setCellValue("DCS");
        titleRow.createCell(6).setCellValue("ICF");
        titleRow.createCell(7).setCellValue("ICS");
        titleRow.createCell(8).setCellValue("OCF");
        titleRow.createCell(9).setCellValue("OCS");

        return sheet;
    }

    public static void writeToDPMResult(Sheet sheet, int rowIndex, String projectName, int microservicesCount,
                                        int distCP2Microservices, int distCVMCI, int distCVCaT, int distCVCeT, int distCVACT,
                                                       int distCPAfferent, int distCVaMCI, int distCVCa, int distCVAIS,
                                                       int distCPEfferent, int distCVeMCI, int distCVCe, int distCVADS){

        rowIndex++;
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(projectName);
        row.createCell(1).setCellValue(microservicesCount);

        row.createCell(2).setCellValue(distCP2Microservices);
        row.createCell(3).setCellValue(distCVMCI);
        row.createCell(4).setCellValue(distCVCaT);
        row.createCell(5).setCellValue(distCVCeT);
        row.createCell(6).setCellValue(distCVACT);

        row.createCell(7).setCellValue((double)distCVMCI/distCP2Microservices);
        row.createCell(8).setCellValue((double)distCVCaT/distCP2Microservices);
        row.createCell(9).setCellValue((double)distCVCeT/distCP2Microservices);
        row.createCell(10).setCellValue((double)distCVACT/distCP2Microservices);

        row.createCell(11).setCellValue(distCPAfferent);
        row.createCell(12).setCellValue(distCVaMCI);
        row.createCell(13).setCellValue(distCVCa);
        row.createCell(14).setCellValue(distCVAIS);

        row.createCell(15).setCellValue((double)distCVaMCI/distCPAfferent);
        row.createCell(16).setCellValue((double)distCVCa/distCPAfferent);
        row.createCell(17).setCellValue((double)distCVAIS/distCPAfferent);

        row.createCell(18).setCellValue(distCPEfferent);
        row.createCell(19).setCellValue(distCVeMCI);
        row.createCell(20).setCellValue(distCVCe);
        row.createCell(21).setCellValue(distCVADS);

        row.createCell(22).setCellValue((double)distCVeMCI/distCPEfferent);
        row.createCell(23).setCellValue((double)distCVCe/distCPEfferent);
        row.createCell(24).setCellValue((double)distCVADS/distCPEfferent);

    }

    public static void writeToSimulationEfferentResult(Sheet sheet, int rowIndex, int callarEntityNumber,
                                                       double ADS, double Ce, double MCIee,
                                                       double DCF, double DCS, double ICF, double ICS, double OCF, double OCS){

        rowIndex++;
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(callarEntityNumber);
        row.createCell(1).setCellValue(ADS);
        row.createCell(2).setCellValue(Ce);
        row.createCell(3).setCellValue(MCIee);
        row.createCell(4).setCellValue(DCF);
        row.createCell(5).setCellValue(DCS);
        row.createCell(6).setCellValue(ICF);
        row.createCell(7).setCellValue(ICS);
        row.createCell(8).setCellValue(OCF);
        row.createCell(9).setCellValue(OCS);

    }

    public static void writeToSimulationAfferentResult(Sheet sheet, int rowIndex, int calleeInterfaceNumber,
                                               double AIS, double Ca, double MCIor,
                                               double DCF, double DCS, double ICF, double ICS, double OCF, double OCS){

        rowIndex++;
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(calleeInterfaceNumber);
        row.createCell(1).setCellValue(AIS);
        row.createCell(2).setCellValue(Ca);
        row.createCell(3).setCellValue(MCIor);
        row.createCell(4).setCellValue(DCF);
        row.createCell(5).setCellValue(DCS);
        row.createCell(6).setCellValue(ICF);
        row.createCell(7).setCellValue(ICS);
        row.createCell(8).setCellValue(OCF);
        row.createCell(9).setCellValue(OCS);

    }

    public static void writeToSimulationResult(Sheet sheet, int rowIndex, int callarEntityNumber, int calleeInterfaceNumber,
                                               double ACT, double CaT, double CeT, double MCI,
                                               double DCF, double DCS, double ICF, double ICS, double OCF, double OCS){

        rowIndex++;
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(callarEntityNumber);
        row.createCell(1).setCellValue(calleeInterfaceNumber);
        row.createCell(2).setCellValue(ACT);
        row.createCell(3).setCellValue(CaT);
        row.createCell(4).setCellValue(CeT);
        row.createCell(5).setCellValue(MCI);
        row.createCell(6).setCellValue(DCF);
        row.createCell(7).setCellValue(DCS);
        row.createCell(8).setCellValue(ICF);
        row.createCell(9).setCellValue(ICS);
        row.createCell(10).setCellValue(OCF);
        row.createCell(11).setCellValue(OCS);

    }

    public static void writeMicroserviceToSheet1(Sheet sheet, int rowIndex, String orgMicroservice, int EntitiesOfOrg, int InterfaceOfOrg,
                                                 String dstMicroservice,int EntitiesOfDst, int InterfaceOfDst, double ACT, double CaT, double CeT, double MIF){
        rowIndex++;
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(orgMicroservice);
        row.createCell(1).setCellValue(dstMicroservice);
        row.createCell(2).setCellValue(EntitiesOfOrg);
        row.createCell(3).setCellValue(InterfaceOfOrg);
        row.createCell(4).setCellValue(EntitiesOfDst);
        row.createCell(5).setCellValue(InterfaceOfDst);
        row.createCell(6).setCellValue(ACT);
        row.createCell(7).setCellValue(CaT);
        row.createCell(8).setCellValue(CeT);
        row.createCell(9).setCellValue(MIF);

    }

    public static void writeMicroserviceToSheet2(Sheet sheet, int rowIndex, MCI.Microservice microservice){
        rowIndex++;
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(microservice.name);
        row.createCell(1).setCellValue(microservice.index+microservice.interfaceIndex);
        row.createCell(2).setCellValue(microservice.interfaceIndex);
        row.createCell(3).setCellValue(microservice.AIS);
        row.createCell(4).setCellValue(microservice.ADS);
        row.createCell(5).setCellValue(microservice.Ca);
        row.createCell(6).setCellValue(microservice.Ce);
        row.createCell(7).setCellValue(microservice.MCIa);
        row.createCell(8).setCellValue(microservice.MCIe);
//        row.createCell(10).setCellValue(microservice.MIFetestB);
//        row.createCell(11).setCellValue(microservice.MIFetestb);
    }


    public static void writeExcel(Workbook workbook,String filePath) throws FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(filePath);
        try {
            workbook.write(fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void main(String[] args){
        List<List<String>> list=readExcel("mall-auth_structure.xlsx",5,0);
        for(List<String> l:list)
            System.out.println(l);
    }
}
