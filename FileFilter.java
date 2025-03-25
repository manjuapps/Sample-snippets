import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileFilter {

    public static List<String> getFilteredFiles(String folderPath) {
        List<String> filteredFiles = new ArrayList<>();
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("Error: Folder '" + folderPath + "' not found.");
            return filteredFiles; // Return empty list on error
        }

        String folderName = folder.getName().toLowerCase(); // Get folder name in lowercase

        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".xml")) {
                    String fileNameWithoutExtension = file.getName().substring(0, file.getName().lastIndexOf(".")).toLowerCase();

                    if (fileNameWithoutExtension.endsWith("_page") || fileNameWithoutExtension.equals(folderName)) {
                        filteredFiles.add(file.getAbsolutePath());
                    }
                }
            }
        }

        return filteredFiles;
    }

    public static List<String> processAllFolders(String rootFolderPath) {
        List<String> allFilteredFiles = new ArrayList<>();
        File rootFolder = new File(rootFolderPath);

        if (!rootFolder.exists() || !rootFolder.isDirectory()) {
            System.err.println("Error: Root folder '" + rootFolderPath + "' not found.");
            return allFilteredFiles;
        }

        File[] subFolders = rootFolder.listFiles(File::isDirectory); // Filter for directories

        if (subFolders != null) {
            for (File subFolder : subFolders) {
                List<String> filteredFiles = getFilteredFiles(subFolder.getAbsolutePath());
                allFilteredFiles.addAll(filteredFiles); // Add all results from subfolder
            }
        }

        return allFilteredFiles;
    }

    public static void main(String[] args) {
        String rootFolderPath = "/path/to/your/root/folder"; // Replace with your root folder path
        List<String> result = processAllFolders(rootFolderPath);

        if (!result.isEmpty()) {
            for (String filePath : result) {
                System.out.println(filePath);
            }
        } else {
            System.out.println("No matching files found in any subfolders.");
        }
    }
}
