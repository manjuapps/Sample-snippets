import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileFilterTraversal {

    public static void main(String[] args) {
        // Specify the directory path
        String folderPath = "your_folder_path_here"; // Change this to the target folder path

        File rootFolder = new File(folderPath);
        if (!rootFolder.exists() || !rootFolder.isDirectory()) {
            System.out.println("Invalid directory path: " + folderPath);
            return;
        }

        List<File> filteredFiles = new ArrayList<>();
        traverseDepthFirst(rootFolder, filteredFiles);

        // Print the filtered files
        System.out.println("Filtered files:");
        for (File file : filteredFiles) {
            System.out.println(file.getAbsolutePath());
        }
    }

    private static void traverseDepthFirst(File folder, List<File> filteredFiles) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                traverseDepthFirst(file, filteredFiles); // Recurse for subdirectories
            } else if (shouldInclude(file)) {
                filteredFiles.add(file);
            }
        }
    }

    private static boolean shouldInclude(File file) {
        String fileName = file.getName();
        File parentFolder = file.getParentFile();

        // Check if the path ends with "external"
        if (file.getAbsolutePath().contains(File.separator + "external")) {
            return true;
        }

        // Check if the file name matches its parent folder name
        if (parentFolder != null && fileName.equals(parentFolder.getName())) {
            return true;
        }

        return false;
    }
}
