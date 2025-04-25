import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class ExcelMatch {

    public static void main(String[] args) throws IOException {
        String inputFilePath = "your_excel_file.xlsx"; // Replace with your input file path
        String outputFilePath = "output_file.xlsx";    // Replace with your desired output file path

        FileInputStream file = new FileInputStream(new File(inputFilePath));
        Workbook workbook = new XSSFWorkbook(file);

        // Load the sheets
        Sheet dataSheet = workbook.getSheet("Sheet1"); // Replace with the name of the sheet containing columns A to F
        Sheet reportsSheet = workbook.getSheet("Reports"); // Replace with the name of the Reports sheet

        // Read the Reports sheet into memory
        Iterator<Row> reportsIterator = reportsSheet.iterator();
        reportsIterator.next(); // Skip header row
        Map<String, String> reportsMap = new HashMap<>();
        while (reportsIterator.hasNext()) {
            Row row = reportsIterator.next();
            Cell keyCell = row.getCell(0);
            Cell valueCell = row.getCell(1);

            if (keyCell != null && valueCell != null) {
                String key = keyCell.getStringCellValue().toLowerCase().trim();
                String value = valueCell.getStringCellValue().trim();
                reportsMap.put(key, value);
            }
        }

        // Process the Data sheet
        Iterator<Row> dataIterator = dataSheet.iterator();
        Row headerRow = dataIterator.next(); // Skip header row

        // Add new columns for "Matched Value" and "Match Flag"
        int matchedValueColIndex = headerRow.getLastCellNum();
        int matchFlagColIndex = matchedValueColIndex + 1;
        headerRow.createCell(matchedValueColIndex).setCellValue("Matched Value");
        headerRow.createCell(matchFlagColIndex).setCellValue("Match Flag");

        while (dataIterator.hasNext()) {
            Row row = dataIterator.next();
            boolean matchFound = false;
            boolean partialMatchFound = false;
            String matchedValue = null;
            String flag = null;

            // Check columns A to F
            for (int col = 0; col < 6; col++) {
                Cell cell = row.getCell(col);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    String cellValue = cell.getStringCellValue().toLowerCase().trim();

                    // Exact match
                    if (reportsMap.containsKey(cellValue)) {
                        matchedValue = reportsMap.get(cellValue);
                        flag = "Exact Match";
                        matchFound = true;
                        break;
                    }

                    // Partial match
                    for (String key : reportsMap.keySet()) {
                        if (key.contains(cellValue)) {
                            matchedValue = reportsMap.get(key);
                            flag = "Partial Match";
                            partialMatchFound = true;
                            break;
                        }
                    }
                }

                if (matchFound partialMatchFound) {
                    break;
                }
            }

            // Add results to the new columns
            Cell matchedValueCell = row.createCell(matchedValueColIndex);
            Cell matchFlagCell = row.createCell(matchFlagColIndex);

            if (matchFound partialMatchFound) {
                matchedValueCell.setCellValue(matchedValue);
                matchFlagCell.setCellValue(flag);
            } else {
                matchFlagCell.setCellValue("No Match");
            }
        }

        // Write the updated workbook to a new file
        FileOutputStream outputStream = new FileOutputStream(outputFilePath);
        workbook.write(outputStream);
        workbook.close();
        file.close();

        System.out.println("Results saved to " + outputFilePath);
    }
}
