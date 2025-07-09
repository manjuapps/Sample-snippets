import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import com.google.gson.Gson;

@WebServlet("/api/dam/createPackage")
public class DamAssetPackageServlet extends HttpServlet {
    
    private static final long MAX_PACKAGE_SIZE = 500 * 1024 * 1024; // 500MB in bytes
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String PACKAGE_PREFIX = "dam_package_";
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            // Parse request parameters
            String rootPath = request.getParameter("rootPath");
            String packageName = request.getParameter("packageName");
            
            if (rootPath == null || rootPath.trim().isEmpty()) {
                sendErrorResponse(response, "Root path is required");
                return;
            }
            
            if (packageName == null || packageName.trim().isEmpty()) {
                packageName = "dam_assets";
            }
            
            // Validate root path
            Path rootDir = Paths.get(rootPath);
            if (!Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
                sendErrorResponse(response, "Invalid root path: " + rootPath);
                return;
            }
            
            // Create packages
            List<PackageInfo> packages = createAssetPackages(rootDir, packageName);
            
            // Send success response
            PackageResponse packageResponse = new PackageResponse();
            packageResponse.setSuccess(true);
            packageResponse.setMessage("Packages created successfully");
            packageResponse.setPackages(packages);
            packageResponse.setTotalPackages(packages.size());
            
            response.getWriter().write(new Gson().toJson(packageResponse));
            
        } catch (Exception e) {
            sendErrorResponse(response, "Error creating packages: " + e.getMessage());
        }
    }
    
    private List<PackageInfo> createAssetPackages(Path rootDir, String packageName) 
            throws IOException {
        
        List<PackageInfo> packages = new ArrayList<>();
        List<Path> allFiles = collectAllFiles(rootDir);
        
        if (allFiles.isEmpty()) {
            throw new RuntimeException("No files found in the specified directory");
        }
        
        int packageIndex = 1;
        long currentPackageSize = 0;
        List<Path> currentPackageFiles = new ArrayList<>();
        
        for (Path file : allFiles) {
            long fileSize = Files.size(file);
            
            // If single file exceeds limit, create separate package
            if (fileSize > MAX_PACKAGE_SIZE) {
                // First, create package with current files if any
                if (!currentPackageFiles.isEmpty()) {
                    PackageInfo pkg = createZipPackage(currentPackageFiles, rootDir, 
                                                    packageName, packageIndex++);
                    packages.add(pkg);
                    currentPackageFiles.clear();
                    currentPackageSize = 0;
                }
                
                // Create package for large file
                List<Path> largeFileList = Arrays.asList(file);
                PackageInfo largePkg = createZipPackage(largeFileList, rootDir, 
                                                      packageName, packageIndex++);
                packages.add(largePkg);
                continue;
            }
            
            // Check if adding this file would exceed limit
            if (currentPackageSize + fileSize > MAX_PACKAGE_SIZE && !currentPackageFiles.isEmpty()) {
                // Create package with current files
                PackageInfo pkg = createZipPackage(currentPackageFiles, rootDir, 
                                                packageName, packageIndex++);
                packages.add(pkg);
                currentPackageFiles.clear();
                currentPackageSize = 0;
            }
            
            currentPackageFiles.add(file);
            currentPackageSize += fileSize;
        }
        
        // Create final package if there are remaining files
        if (!currentPackageFiles.isEmpty()) {
            PackageInfo pkg = createZipPackage(currentPackageFiles, rootDir, 
                                            packageName, packageIndex);
            packages.add(pkg);
        }
        
        return packages;
    }
    
    private List<Path> collectAllFiles(Path rootDir) throws IOException {
        List<Path> files = new ArrayList<>();
        
        Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        return files;
    }
    
    private PackageInfo createZipPackage(List<Path> files, Path rootDir, 
                                       String packageName, int packageIndex) 
            throws IOException {
        
        String zipFileName = String.format("%s_%d.zip", packageName, packageIndex);
        Path zipPath = Paths.get(TEMP_DIR, zipFileName);
        
        long totalSize = 0;
        int fileCount = 0;
        
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(zipPath)))) {
            
            for (Path file : files) {
                // Get relative path from root directory
                Path relativePath = rootDir.relativize(file);
                String entryName = relativePath.toString().replace('\\', '/');
                
                ZipEntry entry = new ZipEntry(entryName);
                entry.setTime(Files.getLastModifiedTime(file).toMillis());
                zos.putNextEntry(entry);
                
                Files.copy(file, zos);
                zos.closeEntry();
                
                totalSize += Files.size(file);
                fileCount++;
            }
        }
        
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageName(zipFileName);
        packageInfo.setFilePath(zipPath.toString());
        packageInfo.setFileCount(fileCount);
        packageInfo.setTotalSize(totalSize);
        packageInfo.setPackageSize(Files.size(zipPath));
        packageInfo.setCreatedAt(new Date());
        
        return packageInfo;
    }
    
    private void sendErrorResponse(HttpServletResponse response, String message) 
            throws IOException {
        PackageResponse errorResponse = new PackageResponse();
        errorResponse.setSuccess(false);
        errorResponse.setMessage(message);
        
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write(new Gson().toJson(errorResponse));
    }
    
    // Response classes
    public static class PackageResponse {
        private boolean success;
        private String message;
        private List<PackageInfo> packages;
        private int totalPackages;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public List<PackageInfo> getPackages() { return packages; }
        public void setPackages(List<PackageInfo> packages) { this.packages = packages; }
        
        public int getTotalPackages() { return totalPackages; }
        public void setTotalPackages(int totalPackages) { this.totalPackages = totalPackages; }
    }
    
    public static class PackageInfo {
        private String packageName;
        private String filePath;
        private int fileCount;
        private long totalSize;
        private long packageSize;
        private Date createdAt;
        
        // Getters and setters
        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public int getFileCount() { return fileCount; }
        public void setFileCount(int fileCount) { this.fileCount = fileCount; }
        
        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
        
        public long getPackageSize() { return packageSize; }
        public void setPackageSize(long packageSize) { this.packageSize = packageSize; }
        
        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    }
}
