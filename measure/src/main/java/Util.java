import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Util {


    //输入excel文件的名称，sheet编号以及读取的列数
    public static List<List<String>> readExcel(String filePath, int line,int index){
        Workbook workbook = readExcel(filePath);

        Sheet sheet = workbook.getSheetAt(index);
        List<List<String>> list=new ArrayList<>();


        for(int j=sheet.getFirstRowNum();j<=sheet.getLastRowNum();j++){
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
}
