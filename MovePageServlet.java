package com.example.core.servlets;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.api.reference.Reference;
import com.day.cq.wcm.api.reference.ReferenceSearch;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

@Component(service = Servlet.class, property = {
        "sling.servlet.paths=/bin/movePageServlet",
        "sling.servlet.methods=GET"
})
public class MovePageServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(MovePageServlet.class);

    @Reference
    private ReferenceSearch referenceSearch;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String path = request.getParameter("path");
        if (path == null path.isEmpty()) {
            response.getWriter().write("Please provide a valid path.");
            return;
        }

        ResourceResolver resourceResolver = request.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        if (pageManager == null) {
            response.getWriter().write("PageManager is not available.");
            return;
        }

        try {
            Page rootPage = pageManager.getPage(path);
            if (rootPage != null) {
                iterateAndMovePages(rootPage, pageManager, resourceResolver);
                response.getWriter().write("Operation completed successfully.");
            } else {
                response.getWriter().write("Root page not found.");
            }
        } catch (Exception e) {
            LOG.error("Error occurred while processing pages", e);
            response.getWriter().write("An error occurred: " + e.getMessage());
        }
    }

    private void iterateAndMovePages(Page parentPage, PageManager pageManager, ResourceResolver resourceResolver) throws RepositoryException, WCMException {
        for (Page childPage : parentPage.listChildren()) {
            if (childPage.getName().equals(parentPage.getName())) {
                movePage(childPage, parentPage, pageManager, resourceResolver);
            }
            // Recursively iterate over child pages
            iterateAndMovePages(childPage, pageManager, resourceResolver);
        }
    }

    private void movePage(Page childPage, Page parentPage, PageManager pageManager, ResourceResolver resourceResolver) throws RepositoryException, WCMException {
        String newParentPath = parentPage.getParent().getPath();
        String newPagePath = newParentPath + "/" + childPage.getName();

        // Move the page
        Node childNode = childPage.adaptTo(Node.class);
        if (childNode != null) {
            JcrUtil.move(childNode, newPagePath, resourceResolver.adaptTo(Session.class));
            LOG.info("Moved page {} to {}", childPage.getPath(), newPagePath);

            // Update references
            updateReferences(childPage.getPath(), newPagePath, resourceResolver);
        }
    }

    private void updateReferences(String oldPath, String newPath, ResourceResolver resourceResolver) {
        List<Reference> references = referenceSearch.search(resourceResolver, oldPath);
        for (Reference reference : references) {
            try {
                Resource resource = reference.getResource();
                if (resource != null) {
                    Node node = resource.adaptTo(Node.class);
                    if (node != null) {
                        String propertyName = reference.getPropertyName();
                        if (node.hasProperty(propertyName)) {
                            node.setProperty(propertyName, newPath);
                            LOG.info("Updated reference in {} for property {}", resource.getPath(), propertyName);
                        }
                    }
                }
            } catch (RepositoryException e) {
                LOG.error("Error updating reference for {}", reference.getResource().getPath(), e);
            }
        }
    }
}
