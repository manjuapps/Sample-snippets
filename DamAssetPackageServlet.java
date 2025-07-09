import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import javax.jcr.*;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Property;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component(immediate = true)
@Service
@Property(name = "sling.servlet.paths", value = "/bin/dam/create-package")
public class DamAssetPackageServlet extends SlingAllMethodsServlet {
    
    private static final long MAX_PACKAGE_SIZE = 500L * 1024 * 1024; // 500MB
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DAM_ROOT = "/content/dam";
    
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            // Parse request
            PackageRequest req = parseRequest(request);
            
            // Validate input
            validateRequest(req, request.getResourceResolver());
            
            // Create packages
            List<PackageInfo> packages = createPackages(req, request.getResourceResolver());
            
            // Send response
            PackageResponse result = new PackageResponse();
            result.success = true;
            result.message = "Packages created successfully";
            result.packages = packages;
            result.totalPackages = packages.size();
            
            response.getWriter().write(objectMapper.writeValueAsString(result));
            
        } catch (Exception e) {
            sendErrorResponse(response, e.getMessage());
        }
    }
    
    private PackageRequest parseRequest(SlingHttpServletRequest request) throws IOException {
        // Try to parse JSON body first
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            if (sb.length() > 0) {
                return objectMapper.readValue(sb.toString(), PackageRequest.class);
            }
        } catch (Exception e) {
            // Fall back to form parameters
        }
        
        // Fall back to form parameters
        PackageRequest req = new PackageRequest();
        req.jcrPath = request.getParameter("jcrPath");
        req.packageName = request.getParameter("packageName");
        req.includeSubfolders = Boolean.parseBoolean(
            request.getParameter("includeSubfolders"));
        req.renditionType = request.getParameter("renditionType");
        req.assetTypes = request.getParameterValues("assetTypes");
        
        return req;
    }
    
    private void validateRequest(PackageRequest req, ResourceResolver resolver) 
            throws IllegalArgumentException {
        if (req.jcrPath == null || req.jcrPath.trim().isEmpty()) {
            throw new IllegalArgumentException("JCR path is required");
        }
        
        // Ensure path starts with /content/dam
        if (!req.jcrPath.startsWith(DAM_ROOT)) {
            throw new IllegalArgumentException("Path must be under /content/dam");
        }
        
        Resource resource = resolver.getResource(req.jcrPath);
        if (resource == null) {
            throw new IllegalArgumentException("JCR path does not exist: " + req.jcrPath);
        }
        
        if (req.packageName == null || req.packageName.trim().isEmpty()) {
            req.packageName = "dam_package";
        }
        
        // Sanitize package name
        req.packageName = req.packageName.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        // Set defaults
        if (req.renditionType == null) {
            req.renditionType = "original";
        }
        
        if (req.assetTypes == null || req.assetTypes.length == 0) {
            req.assetTypes = new String[]{"image", "video", "document", "audio"};
        }
    }
    
    private List<PackageInfo> createPackages(PackageRequest req, ResourceResolver resolver) 
            throws RepositoryException, IOException {
        List<AssetInfo> allAssets = collectAssets(req, resolver);
        
        if (allAssets.isEmpty()) {
            throw new RuntimeException("No assets found in the specified JCR path");
        }
        
        return createZipPackages(allAssets, req.packageName);
    }
    
    private List<AssetInfo> collectAssets(PackageRequest req, ResourceResolver resolver) 
            throws RepositoryException {
        List<AssetInfo> assets = new ArrayList<>();
        
        if (req.includeSubfolders) {
            // Use JCR node iteration to find all assets recursively
            assets.addAll(findAssetsRecursively(req, resolver));
        } else {
            // Get assets from current folder only
            assets.addAll(findAssetsInFolder(req, resolver));
        }
        
        // Sort by size for better packing
        assets.sort(Comparator.comparingLong(a -> a.size));
        
        return assets;
    }
    
    private List<AssetInfo> findAssetsRecursively(PackageRequest req, ResourceResolver resolver) 
            throws RepositoryException {
        List<AssetInfo> assets = new ArrayList<>();
        Resource rootResource = resolver.getResource(req.jcrPath);
        
        if (rootResource != null) {
            traverseResourceTree(rootResource, assets, req);
        }
        
        return assets;
    }
    
    private void traverseResourceTree(Resource resource, List<AssetInfo> assets, PackageRequest req) {
        try {
            // Check if current resource is an asset
            Asset asset = resource.adaptTo(Asset.class);
            if (asset != null && isAllowedAssetType(asset, req.assetTypes)) {
                AssetInfo assetInfo = createAssetInfo(asset, req.renditionType);
                if (assetInfo != null) {
                    assets.add(assetInfo);
                }
            }
            
            // Recursively traverse child resources
            Iterator<Resource> children = resource.listChildren();
            while (children.hasNext()) {
                Resource child = children.next();
                
                // Skip jcr:content nodes and other system nodes
                if (child.getName().startsWith("jcr:") || 
                    child.getName().startsWith("rep:") ||
                    child.getName().equals("renditions")) {
                    continue;
                }
                
                traverseResourceTree(child, assets, req);
            }
        } catch (Exception e) {
            // Log error but continue traversal
            System.err.println("Error traversing resource: " + resource.getPath() + " - " + e.getMessage());
        }
    }
    
    private List<AssetInfo> findAssetsInFolder(PackageRequest req, ResourceResolver resolver) {
        List<AssetInfo> assets = new ArrayList<>();
        Resource folderResource = resolver.getResource(req.jcrPath);
        
        if (folderResource != null) {
            Iterator<Resource> children = folderResource.listChildren();
            while (children.hasNext()) {
                Resource child = children.next();
                
                // Skip system nodes
                if (child.getName().startsWith("jcr:") || 
                    child.getName().startsWith("rep:")) {
                    continue;
                }
                
                Asset asset = child.adaptTo(Asset.class);
                if (asset != null && isAllowedAssetType(asset, req.assetTypes)) {
                    AssetInfo assetInfo = createAssetInfo(asset, req.renditionType);
                    if (assetInfo != null) {
                        assets.add(assetInfo);
                    }
                }
            }
        }
        
        return assets;
    }
    
    private AssetInfo createAssetInfo(Asset asset, String renditionType) {
        try {
            Rendition rendition = getRendition(asset, renditionType);
            if (rendition == null) {
                return null;
            }
            
            AssetInfo info = new AssetInfo();
            info.asset = asset;
            info.rendition = rendition;
            info.path = asset.getPath();
            info.name = asset.getName();
            info.size = rendition.getSize();
            info.mimeType = asset.getMimeType();
            info.lastModified = asset.getLastModified();
            
            return info;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Rendition getRendition(Asset asset, String renditionType) {
        switch (renditionType.toLowerCase()) {
            case "original":
                return asset.getOriginal();
            case "thumbnail":
                return asset.getRendition("cq5dam.thumbnail.140.100.png");
            case "web":
                return asset.getRendition("cq5dam.web.1280.1280.jpeg");
            default:
                return asset.getRendition(renditionType);
        }
    }
    
    private boolean isAllowedAssetType(Asset asset, String[] allowedTypes) {
        if (allowedTypes == null || allowedTypes.length == 0) {
            return true;
        }
        
        String mimeType = asset.getMimeType();
        if (mimeType == null) {
            return false;
        }
        
        for (String type : allowedTypes) {
            if (mimeType.startsWith(type + "/")) {
                return true;
            }
        }
        
        return false;
    }
    
    private String getFormatPattern(String assetType) {
        switch (assetType.toLowerCase()) {
            case "image":
                return "image";
            case "video":
                return "video";
            case "audio":
                return "audio";
            case "document":
                return "application";
            default:
                return assetType;
        }
    }
    
    private List<PackageInfo> createZipPackages(List<AssetInfo> assets, String packageName) 
            throws IOException {
        List<PackageInfo> packages = new ArrayList<>();
        List<AssetInfo> currentBatch = new ArrayList<>();
        long currentSize = 0;
        int packageIndex = 1;
        
        for (AssetInfo asset : assets) {
            // If single asset exceeds limit, create separate package
            if (asset.size > MAX_PACKAGE_SIZE) {
                // Create package with current batch first
                if (!currentBatch.isEmpty()) {
                    packages.add(createSinglePackage(currentBatch, packageName, packageIndex++));
                    currentBatch.clear();
                    currentSize = 0;
                }
                
                // Create package for oversized asset
                List<AssetInfo> oversizedBatch = Arrays.asList(asset);
                packages.add(createSinglePackage(oversizedBatch, packageName, packageIndex++));
                continue;
            }
            
            // Check if adding this asset would exceed limit
            if (currentSize + asset.size > MAX_PACKAGE_SIZE && !currentBatch.isEmpty()) {
                // Create package with current batch
                packages.add(createSinglePackage(currentBatch, packageName, packageIndex++));
                currentBatch.clear();
                currentSize = 0;
            }
            
            currentBatch.add(asset);
            currentSize += asset.size;
        }
        
        // Create final package if there are remaining assets
        if (!currentBatch.isEmpty()) {
            packages.add(createSinglePackage(currentBatch, packageName, packageIndex));
        }
        
        return packages;
    }
    
    private PackageInfo createSinglePackage(List<AssetInfo> assets, String packageName, int index) 
            throws IOException {
        String zipFileName = String.format("%s_%03d.zip", packageName, index);
        String zipPath = TEMP_DIR + File.separator + zipFileName;
        
        long totalSize = 0;
        int assetCount = 0;
        
        try (FileOutputStream fos = new FileOutputStream(zipPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            
            zos.setLevel(Deflater.DEFAULT_COMPRESSION);
            
            for (AssetInfo assetInfo : assets) {
                // Create folder structure in ZIP
                String entryPath = createZipEntryPath(assetInfo.path);
                ZipEntry entry = new ZipEntry(entryPath);
                entry.setTime(assetInfo.lastModified);
                
                zos.putNextEntry(entry);
                
                // Copy asset content
                try (InputStream is = assetInfo.rendition.getStream();
                     BufferedInputStream bis = new BufferedInputStream(is)) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                }
                
                zos.closeEntry();
                totalSize += assetInfo.size;
                assetCount++;
            }
        }
        
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = zipFileName;
        packageInfo.filePath = zipPath;
        packageInfo.assetCount = assetCount;
        packageInfo.totalSize = totalSize;
        packageInfo.compressedSize = new File(zipPath).length();
        packageInfo.compressionRatio = (double) packageInfo.compressedSize / totalSize;
        packageInfo.createdAt = new Date();
        
        return packageInfo;
    }
    
    private String createZipEntryPath(String jcrPath) {
        // Remove /content/dam prefix and ensure proper path structure
        String relativePath = jcrPath.substring(DAM_ROOT.length());
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        return relativePath;
    }
    
    private void sendErrorResponse(SlingHttpServletResponse response, String message) 
            throws IOException {
        PackageResponse errorResponse = new PackageResponse();
        errorResponse.success = false;
        errorResponse.message = message;
        
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    // Data classes
    public static class PackageRequest {
        public String jcrPath;
        public String packageName;
        public boolean includeSubfolders = true;
        public String renditionType = "original";
        public String[] assetTypes;
    }
    
    public static class PackageResponse {
        public boolean success;
        public String message;
        public List<PackageInfo> packages;
        public int totalPackages;
    }
    
    public static class PackageInfo {
        public String packageName;
        public String filePath;
        public int assetCount;
        public long totalSize;
        public long compressedSize;
        public double compressionRatio;
        public Date createdAt;
    }
    
    private static class AssetInfo {
        public Asset asset;
        public Rendition rendition;
        public String path;
        public String name;
        public long size;
        public String mimeType;
        public long lastModified;
    }
                                                        }
