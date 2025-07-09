import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

public class ExcelSheetComparator {
    
    public static void main(String[] args) {
        String filePath = "path/to/your/excel/file.xlsx"; // Update with your file path
        
        try {
            compareColumnA(filePath);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
    
    public static void compareColumnA(String filePath) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = null;
        
        try {
            // Determine workbook type based on file extension
            if (filePath.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (filePath.endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis);
            } else {
                throw new IllegalArgumentException("Unsupported file format. Use .xlsx or .xls");
            }
            
            // Get the sheets
            Sheet sheet1 = workbook.getSheet("Sheet1");
            Sheet sheet2 = workbook.getSheet("Sheet2");
            
            if (sheet1 == null || sheet2 == null) {
                throw new IllegalArgumentException("Sheet1 or Sheet2 not found in the workbook");
            }
            
            // Get column A values from sheet1
            Set<String> sheet1Values = getColumnAValues(sheet1);
            
            // Get column A values from sheet2 that are not in sheet1
            List<String> uniqueInSheet2 = getUniqueValues(sheet2, sheet1Values);
            
            // Display results
            displayResults(uniqueInSheet2);
            
        } finally {
            if (workbook != null) {
                workbook.close();
            }
            fis.close();
        }
    }
    
    private static Set<String> getColumnAValues(Sheet sheet) {
        Set<String> values = new HashSet<>();
        
        for (Row row : sheet) {
            Cell cell = row.getCell(0); // Column A is index 0
            if (cell != null) {
                String cellValue = getCellValueAsString(cell);
                if (!cellValue.trim().isEmpty()) {
                    values.add(cellValue.trim());
                }
            }
        }
        
        return values;
    }
    
    private static List<String> getUniqueValues(Sheet sheet2, Set<String> sheet1Values) {
        List<String> uniqueValues = new ArrayList<>();
        
        for (Row row : sheet2) {
            Cell cell = row.getCell(0); // Column A is index 0
            if (cell != null) {
                String cellValue = getCellValueAsString(cell);
                if (!cellValue.trim().isEmpty() && !sheet1Values.contains(cellValue.trim())) {
                    uniqueValues.add(cellValue.trim());
                }
            }
        }
        
        return uniqueValues;
    }
    
    private static String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Format numeric values to avoid scientific notation
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
    
    private static void displayResults(List<String> uniqueValues) {
        System.out.println("=== Items in Sheet2 (Column A) that are NOT in Sheet1 ===");
        
        if (uniqueValues.isEmpty()) {
            System.out.println("No unique items found in Sheet2. All items are present in Sheet1.");
        } else {
            System.out.println("Found " + uniqueValues.size() + " unique item(s):");
            System.out.println("------------------------------------------------------");
            
            for (int i = 0; i < uniqueValues.size(); i++) {
                System.out.println((i + 1) + ". " + uniqueValues.get(i));
            }
        }
    }
}

// Alternative method if you want to compare two different Excel files
class ExcelFileComparator {
    
    public static void compareTwoFiles(String file1Path, String file2Path) throws IOException {
        // Read sheet1 from first file
        Set<String> sheet1Values = getColumnAFromFile(file1Path, "Sheet1");
        
        // Read sheet2 from second file
        List<String> uniqueValues = getUniqueValuesFromFile(file2Path, "Sheet2", sheet1Values);
        
        // Display results
        displayResults(uniqueValues);
    }
    
    private static Set<String> getColumnAFromFile(String filePath, String sheetName) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = null;
        
        try {
            if (filePath.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else {
                workbook = new HSSFWorkbook(fis);
            }
            
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet " + sheetName + " not found");
            }
            
            return getColumnAValues(sheet);
            
        } finally {
            if (workbook != null) {
                workbook.close();
            }
            fis.close();
        }
    }
    
    private static List<String> getUniqueValuesFromFile(String filePath, String sheetName, Set<String> referenceValues) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = null;
        
        try {
            if (filePath.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else {
                workbook = new HSSFWorkbook(fis);
            }
            
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet " + sheetName + " not found");
            }
            
            return getUniqueValues(sheet, referenceValues);
            
        } finally {
            if (workbook != null) {
                workbook.close();
            }
            fis.close();
        }
    }
    
    // Helper methods (same as above)
    private static Set<String> getColumnAValues(Sheet sheet) {
        Set<String> values = new HashSet<>();
        
        for (Row row : sheet) {
            Cell cell = row.getCell(0);
            if (cell != null) {
                String cellValue = getCellValueAsString(cell);
                if (!cellValue.trim().isEmpty()) {
                    values.add(cellValue.trim());
                }
            }
        }
        
        return values;
    }
    
    private static List<String> getUniqueValues(Sheet sheet, Set<String> referenceValues) {
        List<String> uniqueValues = new ArrayList<>();
        
        for (row : sheet) {
            Cell cell = row.getCell(0);
            if (cell != null) {
                String cellValue = getCellValueAsString(cell);
                if (!cellValue.trim().isEmpty() && !referenceValues.contains(cellValue.trim())) {
                    uniqueValues.add(cellValue.trim());
                }
            }
        }
        
        return uniqueValues;
    }
    
    private static String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
    
    private static void displayResults(List<String> uniqueValues) {
        System.out.println("=== Items in Sheet2 (Column A) that are NOT in Sheet1 ===");
        
        if (uniqueValues.isEmpty()) {
            System.out.println("No unique items found in Sheet2. All items are present in Sheet1.");
        } else {
            System.out.println("Found " + uniqueValues.size() + " unique item(s):");
            System.out.println("------------------------------------------------------");
            
            for (int i = 0; i < uniqueValues.size(); i++) {
                System.out.println((i + 1) + ". " + uniqueValues.get(i));
            }
        }
    }
            }
