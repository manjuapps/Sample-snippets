// CarouselItem.java (Model for each item in the carousel)
package com.myproject.core.models;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import com.adobe.cq.wcm.core.components.models.Image;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class CarouselItem {

    @Inject
    private String title;

    @Self
    private Resource resource;

    private Image image;

    @PostConstruct
    protected void init() {
        // Delegate to the OOTB Image model for image rendering.
        Resource imageResource = resource.getChild("image"); // Assumes image resource is named "image".
        if (imageResource != null) {
            image = imageResource.adaptTo(Image.class);
        }
    }

    public String getTitle() {
        return title;
    }

    public Image getImage() {
        return image;
    }
}
