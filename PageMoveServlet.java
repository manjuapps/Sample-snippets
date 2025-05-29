package com.yourcompany.core.servlets;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.FileItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@Component(service = Servlet.class, property = {
    "sling.servlet.methods=POST",
    "sling.servlet.paths=/bin/movePages",
    "sling.servlet.extensions=json"
})
public class PageMoveServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(PageMoveServlet.class);
    
    @Reference
    private PageManager pageManager;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            // Check if request contains multipart content
            if (!ServletFileUpload.isMultipartContent(request)) {
                writeErrorResponse(out, "Request must contain multipart content with Excel file");
                return;
            }
            
            // Parse the multipart request
            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            List<FileItem> items = upload.parseRequest(request);
            
            FileItem excelFile = null;
            for (FileItem item : items) {
                if (!item.isFormField() && item.getFieldName().equals("excelFile")) {
                    excelFile = item;
                    break;
                }
            }
            
            if (excelFile == null) {
                writeErrorResponse(out, "No Excel file found in request");
                return;
            }
            
            // Process the Excel file
            List<PageMoveOperation> moveOperations = parseExcelFile(excelFile.getInputStream(), 
                                                                   excelFile.getName());
            
            if (moveOperations.isEmpty()) {
                writeErrorResponse(out, "No valid move operations found in Excel file");
                return;
            }
            
            // Execute page moves
            List<String> results = executePageMoves(moveOperations, request.getResourceResolver().adaptTo(Session.class));
            
            // Write success response
            writeSuccessResponse(out, results);
            
        } catch (Exception e) {
            LOG.error("Error processing page move request", e);
            writeErrorResponse(out, "Error processing request: " + e.getMessage());
        }
    }
    
    private List<PageMoveOperation> parseExcelFile(InputStream inputStream, String fileName) 
            throws IOException {
        
        List<PageMoveOperation> operations = new ArrayList<>();
        Workbook workbook = null;
        
        try {
            // Determine workbook type based on file extension
            if (fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else if (fileName.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
            } else {
                throw new IOException("Unsupported file format. Please use .xlsx or .xls files.");
            }
            
            Sheet sheet = workbook.getSheetAt(0); // Get first sheet
            
            // Skip header row if present (assuming row 0 is header)
            int startRow = 1;
            if (sheet.getPhysicalNumberOfRows() > 0) {
                Row firstRow = sheet.getRow(0);
                if (firstRow != null && firstRow.getCell(0) != null) {
                    String firstCellValue = getCellValueAsString(firstRow.getCell(0));
                    if (firstCellValue.toLowerCase().contains("source") || 
                        firstCellValue.toLowerCase().contains("from")) {
                        startRow = 1; // Skip header
                    } else {
                        startRow = 0; // No header, start from first row
                    }
                }
            }
            
            // Process each row
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Cell sourceCell = row.getCell(0);
                Cell destinationCell = row.getCell(1);
                
                if (sourceCell == null || destinationCell == null) continue;
                
                String sourcePath = getCellValueAsString(sourceCell);
                String destinationPath = getCellValueAsString(destinationCell);
                
                if (sourcePath != null && !sourcePath.trim().isEmpty() && 
                    destinationPath != null && !destinationPath.trim().isEmpty()) {
                    
                    operations.add(new PageMoveOperation(sourcePath.trim(), destinationPath.trim()));
                }
            }
            
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
        
        return operations;
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    private List<String> executePageMoves(List<PageMoveOperation> operations, Session session) {
        List<String> results = new ArrayList<>();
        
        for (PageMoveOperation operation : operations) {
            try {
                String result = movePageOperation(operation.getSourcePath(), 
                                                operation.getDestinationPath(), session);
                results.add(result);
                
            } catch (Exception e) {
                String errorMsg = String.format("Failed to move page from %s to %s: %s", 
                                              operation.getSourcePath(), 
                                              operation.getDestinationPath(), 
                                              e.getMessage());
                LOG.error(errorMsg, e);
                results.add(errorMsg);
            }
        }
        
        return results;
    }
    
    private String movePageOperation(String sourcePath, String destinationPath, Session session) 
            throws WCMException, RepositoryException {
        
        // Get the page to move
        Page sourcePage = pageManager.getPage(sourcePath);
        if (sourcePage == null) {
            throw new WCMException("Source page not found: " + sourcePath);
        }
        
        // Validate destination path
        String destinationParent = destinationPath.substring(0, destinationPath.lastIndexOf('/'));
        String newPageName = destinationPath.substring(destinationPath.lastIndexOf('/') + 1);
        
        Page destinationParentPage = pageManager.getPage(destinationParent);
        if (destinationParentPage == null) {
            throw new WCMException("Destination parent page not found: " + destinationParent);
        }
        
        // Check if destination already exists
        Page existingPage = pageManager.getPage(destinationPath);
        if (existingPage != null) {
            throw new WCMException("Destination page already exists: " + destinationPath);
        }
        
        // Perform the move operation
        Node sourceNode = sourcePage.adaptTo(Node.class);
        Node destinationParentNode = destinationParentPage.adaptTo(Node.class);
        
        // Move the node
        session.move(sourceNode.getPath(), destinationParentNode.getPath() + "/" + newPageName);
        session.save();
        
        String successMsg = String.format("Successfully moved page from %s to %s", 
                                        sourcePath, destinationPath);
        LOG.info(successMsg);
        return successMsg;
    }
    
    private void writeSuccessResponse(PrintWriter out, List<String> results) {
        out.write("{");
        out.write("\"status\": \"success\",");
        out.write("\"message\": \"Page move operations completed\",");
        out.write("\"results\": [");
        
        for (int i = 0; i < results.size(); i++) {
            out.write("\"" + escapeJson(results.get(i)) + "\"");
            if (i < results.size() - 1) {
                out.write(",");
            }
        }
        
        out.write("]");
        out.write("}");
    }
    
    private void writeErrorResponse(PrintWriter out, String message) {
        out.write("{");
        out.write("\"status\": \"error\",");
        out.write("\"message\": \"" + escapeJson(message) + "\"");
        out.write("}");
    }
    
    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    // Inner class to hold move operation data
    private static class PageMoveOperation {
        private final String sourcePath;
        private final String destinationPath;
        
        public PageMoveOperation(String sourcePath, String destinationPath) {
            this.sourcePath = sourcePath;
            this.destinationPath = destinationPath;
        }
        
        public String getSourcePath() {
            return sourcePath;
        }
        
        public String getDestinationPath() {
            return destinationPath;
        }
    }
                            }
