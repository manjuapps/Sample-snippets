import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodePropertyStringReplacer {
    
    private static final Logger log = LoggerFactory.getLogger(NodePropertyStringReplacer.class);
    
    /**
     * Method 1: Using JCR API directly
     */
    public void replaceStringInNodeProperty(Node node, String propertyName, 
                                          String searchString, String replaceString) {
        try {
            // Check if property exists
            if (node.hasProperty(propertyName)) {
                Property property = node.getProperty(propertyName);
                String currentValue = property.getString();
                
                // Check if the property value contains the specific string
                if (currentValue != null && currentValue.contains(searchString)) {
                    // Replace the string
                    String newValue = currentValue.replace(searchString, replaceString);
                    
                    // Set the new value
                    node.setProperty(propertyName, newValue);
                    
                    // Save the node
                    node.getSession().save();
                    
                    log.info("Property '{}' updated from '{}' to '{}'", 
                            propertyName, currentValue, newValue);
                } else {
                    log.info("Property '{}' does not contain the search string '{}'", 
                            propertyName, searchString);
                }
            } else {
                log.warn("Property '{}' does not exist on the node", propertyName);
            }
        } catch (RepositoryException e) {
            log.error("Error updating node property", e);
        }
    }
    
    /**
     * Method 2: Using Sling Resource API (recommended for AEM)
     */
    public void replaceStringInResourceProperty(ResourceResolver resourceResolver, 
                                              String resourcePath, String propertyName,
                                              String searchString, String replaceString) {
        try {
            Resource resource = resourceResolver.getResource(resourcePath);
            if (resource != null) {
                ModifiableValueMap valueMap = resource.adaptTo(ModifiableValueMap.class);
                
                if (valueMap != null && valueMap.containsKey(propertyName)) {
                    String currentValue = valueMap.get(propertyName, String.class);
                    
                    // Check if the property value contains the specific string
                    if (currentValue != null && currentValue.contains(searchString)) {
                        // Replace the string
                        String newValue = currentValue.replace(searchString, replaceString);
                        
                        // Update the property
                        valueMap.put(propertyName, newValue);
                        
                        // Commit the changes
                        resourceResolver.commit();
                        
                        log.info("Property '{}' updated from '{}' to '{}'", 
                                propertyName, currentValue, newValue);
                    } else {
                        log.info("Property '{}' does not contain the search string '{}'", 
                                propertyName, searchString);
                    }
                } else {
                    log.warn("Property '{}' does not exist on the resource or resource is not modifiable", 
                            propertyName);
                }
            } else {
                log.warn("Resource not found at path: {}", resourcePath);
            }
        } catch (Exception e) {
            log.error("Error updating resource property", e);
        }
    }
    
    /**
     * Method 3: Handle multiple properties at once
     */
    public void replaceStringInMultipleProperties(Node node, String[] propertyNames,
                                                String searchString, String replaceString) {
        try {
            boolean hasChanges = false;
            
            for (String propertyName : propertyNames) {
                if (node.hasProperty(propertyName)) {
                    Property property = node.getProperty(propertyName);
                    String currentValue = property.getString();
                    
                    if (currentValue != null && currentValue.contains(searchString)) {
                        String newValue = currentValue.replace(searchString, replaceString);
                        node.setProperty(propertyName, newValue);
                        hasChanges = true;
                        
                        log.info("Property '{}' updated from '{}' to '{}'", 
                                propertyName, currentValue, newValue);
                    }
                }
            }
            
            // Save only if there are changes
            if (hasChanges) {
                node.getSession().save();
                log.info("Node saved with updated properties");
            }
            
        } catch (RepositoryException e) {
            log.error("Error updating multiple node properties", e);
        }
    }
    
    /**
     * Method 4: Case-insensitive string replacement
     */
    public void replaceStringCaseInsensitive(Node node, String propertyName,
                                           String searchString, String replaceString) {
        try {
            if (node.hasProperty(propertyName)) {
                Property property = node.getProperty(propertyName);
                String currentValue = property.getString();
                
                if (currentValue != null) {
                    // Case-insensitive check and replace
                    String newValue = currentValue.replaceAll("(?i)" + 
                                    java.util.regex.Pattern.quote(searchString), replaceString);
                    
                    // Only update if there was a change
                    if (!currentValue.equals(newValue)) {
                        node.setProperty(propertyName, newValue);
                        node.getSession().save();
                        
                        log.info("Property '{}' updated (case-insensitive) from '{}' to '{}'", 
                                propertyName, currentValue, newValue);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Error updating node property with case-insensitive replacement", e);
        }
    }
    
    /**
     * Example usage in a service or servlet
     */
    public void exampleUsage(ResourceResolver resourceResolver) {
        try {
            // Example 1: Direct node manipulation
            Session session = resourceResolver.adaptTo(Session.class);
            Node node = session.getNode("/content/mysite/page/jcr:content");
            replaceStringInNodeProperty(node, "jcr:title", "old-text", "new-text");
            
            // Example 2: Using resource API
            replaceStringInResourceProperty(resourceResolver, 
                "/content/mysite/page/jcr:content", 
                "jcr:description", 
                "old-description", 
                "new-description");
            
            // Example 3: Multiple properties
            String[] properties = {"jcr:title", "jcr:description", "customProperty"};
            replaceStringInMultipleProperties(node, properties, "oldValue", "newValue");
            
        } catch (RepositoryException e) {
            log.error("Error in example usage", e);
        }
    }
}
