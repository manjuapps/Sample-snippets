package com.adobe.aem.guides.wknd.core.models;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Model(adaptables = Resource.class)
public class SiteMapModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteMapModel.class);

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Default(values = "/content/wknd")
    protected String rootContentPath;

    @SlingObject
    private ResourceResolver resourceResolver;

    private List<SitemapPageHierarchy> siteMap;

    @PostConstruct
    protected void init() {
        if (rootContentPath == null || rootContentPath.isEmpty()) {
            LOGGER.warn("Root Content path is not defined.");
            return;
        }

        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        if (pageManager == null) {
            LOGGER.error("PageManager is null. Cannot fetch the site map.");
            return;
        }

        Page rootPage = pageManager.getPage(rootContentPath);
        if (rootPage == null) {
            LOGGER.warn("No page found for path: {}", rootContentPath);
            return;
        }

        siteMap = buildSiteMap(rootPage);
    }

    private List<SitemapPageHierarchy> buildSiteMap(Page rootPage) {

        if (rootPage != null) {
            Iterator<Page> childIterator = rootPage.listChildren();
            return StreamSupport.stream(((Iterable<Page>) () -> childIterator).spliterator(), false)
                    .filter(child -> !isPageExcluded(child))
                    .map(child -> new SitemapPageHierarchy(child, buildSiteMap(child)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }


    private boolean isPageExcluded(Page page) {
        if (page == null) return true;

        return page.getProperties().get("hideInSitemap", false);
    }


    public List<SitemapPageHierarchy> getSiteMap() {
        return siteMap;
    }
}
