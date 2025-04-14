// Java
import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

public class BreadthFirstFileListing {
    public static void main(String[] args) {
        // Specify the root directory to start the search
        File rootDirectory = new File("C:\\YourRootDirectoryPath");

        if (!rootDirectory.exists() !rootDirectory.isDirectory()) {
            System.out.println("Invalid directory path.");
            return;
        }

        // Use a queue for breadth-first traversal
        Queue<File> queue = new LinkedList<>();
        queue.add(rootDirectory);

        System.out.println("Files and directories in breadth-first order:");
        while (!queue.isEmpty()) {
            File current = queue.poll();

            // Print the current file or directory
            System.out.println(current.getAbsolutePath());

            // If the current file is a directory, add its contents to the queue
            if (current.isDirectory()) {
                File[] files = current.listFiles();
                if (files != null) {
                    for (File file : files) {
                        queue.add(file);
                    }
                }
            }
        }
    }
}
