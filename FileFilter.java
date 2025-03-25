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

    public static void main(String[] args) {
        String folderPath = "/path/to/your/folder"; // Replace with your folder path
        List<String> result = getFilteredFiles(folderPath);

        if (!result.isEmpty()) {
            for (String filePath : result) {
                System.out.println(filePath);
            }
        } else {
            System.out.println("No matching files found.");
        }
    }
}
