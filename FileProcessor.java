import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileProcessor {

    public static void main(String[] args) {
        // Specify the folder path here
        String folderPath = "C:/your/folder/path";

        // List to store the paths of matching files
        List<Path> fileList = new ArrayList<>();

        // Get the list of files
        try {
            Files.walk(new File(folderPath).toPath())
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        String parentFolderName = path.getParent().getFileName().toString();
                        return fileName.equals(parentFolderName) fileName.endsWith("_Page.xml");
                    })
                    .forEach(fileList::add);
        } catch (IOException e) {
            System.err.println("Error reading files: " + e.getMessage());
            return;
        }

        // Sort the list: parent paths first, then child paths
        Collections.sort(fileList, Comparator.comparing(Path::toString));

        // Print the sorted list
        System.out.println("Sorted list of matching files:");
        fileList.forEach(System.out::println);

        // You can now use the fileList for further processing
    }
}
