import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(service = JcrPropertyService.class)
public class JcrPropertyService {

    @Reference
    private ResourceResolverFactory resolverFactory;

    public Map<String, Object> getNodeProperties(String nodePath) {
        Map<String, Object> propertiesMap = new HashMap<>();
        ResourceResolver resourceResolver = null;
        Session session = null;

        try {
            resourceResolver = resolverFactory.getServiceResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
            if (session == null) {
                 // handle session null
                return propertiesMap;
            }
            Node node = session.getNode(nodePath);
            if (node != null) {
                PropertyIterator properties = node.getProperties();
                while (properties.hasNext()) {
                    Property property = properties.nextProperty();
                    String propertyName = property.getName();

                    if (property.isMultiple()) {
                        List<Object> values = new ArrayList<>();
                        Value[] propertyValues = property.getValues();
                        for (Value value : propertyValues) {
                            values.add(value.getString());
                        }
                        propertiesMap.put(propertyName, values);
                    } else {
                        propertiesMap.put(propertyName, property.getValue().getString());
                    }
                }
            }
        } catch (RepositoryException e) {
           // handle exception
        } finally {
            if (session != null) {
                session.logout();
            }
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
        return propertiesMap;
    }

    public void setNodeProperty(String nodePath, String propertyName, Object propertyValue) {
        ResourceResolver resourceResolver = null;
        Session session = null;

        try {
            resourceResolver = resolverFactory.getServiceResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
            if (session == null) {
                // handle session null
                return;
            }
            Node node = session.getNode(nodePath);
            if (node != null) {
                if (propertyValue instanceof List) {
                    List<?> listValue = (List<?>) propertyValue;
                    String[] stringValues = listValue.stream()
                            .map(Object::toString)
                            .toArray(String[]::new);
                    node.setProperty(propertyName, stringValues);
                } else {
                    node.setProperty(propertyName, propertyValue.toString());
                }
                 session.save();
            }
        } catch (RepositoryException e) {
            // handle exception
        } finally {
            if (session != null) {
                session.logout();
            }
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }
}
