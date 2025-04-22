import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImagePropertiesExtractor {

    public static Map<String, Integer> extractProperties(String transformationString) {
        Map<String, Integer> properties = new HashMap<>();

        // Define regular expressions to find the properties
        Pattern xPattern = Pattern.compile("x=(\\d+)");
        Pattern yPattern = Pattern.compile("y=(\\d+)");
        Pattern widthPattern = Pattern.compile("width=(\\d+)");
        Pattern heightPattern = Pattern.compile("height=(\\d+)");

        Matcher matcher;

        // Extract x
        matcher = xPattern.matcher(transformationString);
        if (matcher.find()) {
            properties.put("x", Integer.parseInt(matcher.group(1)));
        }

        // Extract y
        matcher = yPattern.matcher(transformationString);
        if (matcher.find()) {
            properties.put("y", Integer.parseInt(matcher.group(1)));
        }

        // Extract width
        matcher = widthPattern.matcher(transformationString);
        if (matcher.find()) {
            properties.put("width", Integer.parseInt(matcher.group(1)));
        }

        // Extract height
        matcher = heightPattern.matcher(transformationString);
        if (matcher.find()) {
            properties.put("height", Integer.parseInt(matcher.group(1)));
        }

        return properties;
    }

    public static void main(String[] args) {
        String transformationString = "b;a=0;c=1/crop;x=161;y=0;width=396;height=495";
        Map<String, Integer> extractedProperties = extractProperties(transformationString);

        if (!extractedProperties.isEmpty()) {
            System.out.println("Extracted Properties:");
            if (extractedProperties.containsKey("x")) {
                System.out.println("x: " + extractedProperties.get("x"));
            }
            if (extractedProperties.containsKey("y")) {
                System.out.println("y: " + extractedProperties.get("y"));
            }
            if (extractedProperties.containsKey("width")) {
                System.out.println("width: " + extractedProperties.get("width"));
            }
            if (extractedProperties.containsKey("height")) {
                System.out.println("height: " + extractedProperties.get("height"));
            }
        } else {
            System.out.println("No x, y, width, or height properties found in the string.");
        }
    }
}
