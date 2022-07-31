package top.lazyr.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author lazyr
 * @created 2021/11/24
 */
public class ExcelUtil {
    private static Logger logger = LoggerFactory.getLogger(ExcelUtil.class);
    private static String PATH = "./src/main/resources/";

    public static void append2Excel(String workbookName, String sheetName, List<List<String>> infos) {
        Workbook workbook = null;
        File file = new File(PATH + workbookName);
        if (file.exists()) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);
                workbook = new XSSFWorkbook(inputStream);
            } catch (FileNotFoundException e) {
                logger.error(workbookName + " not found: " + e.getMessage());
            } catch (IOException e) {
                logger.error("read " + workbookName + " error: " + e.getMessage());
            }

        } else {
            logger.info("创建工作簿: " + workbookName);
            workbook = new XSSFWorkbook();
        }
        boolean isNew = false;
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            isNew = true;
            System.out.println("创建工作表: " + sheetName);
            sheet = workbook.createSheet(sheetName);
        }
        int lastRowNum = isNew ? 0 : sheet.getLastRowNum() + 1;
        for (List<String> info: infos) {
            Row row = sheet.createRow(lastRowNum++);
            for (int i = 0; i < info.size(); i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(info.get(i));
            }
        }
        System.out.println("写入中...");
        // 写入数据
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(PATH + workbookName);
            workbook.write(outputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void write2Excel(String fileName, String sheetName, List<List<String>> infos) {
        // 1、创建一个工作簿
        Workbook workbook = new XSSFWorkbook();
        logger.info("创建工作簿: " + fileName);
        Sheet sheet = workbook.createSheet(sheetName);
        logger.info("创建工作表: " + sheetName);
        int lastRowNum = 0;
        for (List<String> info: infos) {
            Row row = sheet.createRow(lastRowNum++);
            for (int i = 0; i < info.size(); i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(info.get(i));
            }
        }
        logger.info("写入中...");
        // 写入数据
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(PATH + fileName);
            workbook.write(outputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Map<String, List<List<String>>> readAllFromExcel(String fileName) {
        return null;
    }

    public static List<List<String>> readFromExcel(String fileName, String sheetName) {
        List<List<String>> infos = new ArrayList<>();
        FileInputStream inputStream = null;
        Workbook workbook = null;
        File file = new File(PATH + fileName);
        if (!file.exists()) {
            logger.info(fileName + " is not exist");
            return new ArrayList<>();
        }
        try {
            inputStream = new FileInputStream(file);
            workbook = new XSSFWorkbook(inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            logger.info("the " + sheetName + " of " + fileName + " is not exist");
            return new ArrayList<>();
        }

        int lastRowNum = sheet.getLastRowNum();
        for (int i = 0; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            int lastCellNum = row.getLastCellNum();
            List<String> info = new ArrayList<>();
            for (int col = 0; col < lastCellNum; col++) {
                Cell cell = row.getCell(col);
                info.add(cell.getStringCellValue());
            }
            infos.add(info);
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.error("FileInputStream close failed: " + e.getMessage());
            }
        }
        return infos;
    }

}
