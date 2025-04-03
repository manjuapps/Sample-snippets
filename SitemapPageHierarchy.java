package com.adobe.aem.guides.wknd.core.models;

import com.day.cq.wcm.api.Page;

import java.util.List;

public class SitemapPageHierarchy {
    private final Page page;
    private final List<SitemapPageHierarchy> children;

    public SitemapPageHierarchy(Page page, List<SitemapPageHierarchy> children) {
        this.page = page;
        this.children = children;
    }

    public Page getPage() {
        return page;
    }

    public List<SitemapPageHierarchy> getChildren() {
        return children;
    }
}
