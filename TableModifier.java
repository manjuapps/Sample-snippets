public class TableModifier {
    public static void main(String[] args) {
        // Sample rich text content with a table containing a class attribute
        String richTextContent = "<html><body><table class=\"myTable\"><tr><td>Cell 1</td><td>Cell 2</td></tr></table></body></html>";

        // Use a regular expression to remove the class attribute and add the desired attributes
        String modifiedContent = richTextContent.replaceAll(
            "<table[^>]*class=\"[^\"]*\"[^>]*>", 
            "<table cellspacing=\"5\" cellpadding=\"10\" border=\"1\">"
        );

        // Print the modified content
        System.out.println(modifiedContent);
    }
}
