package com.davidr.secureft.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.router.RouterLink;

public class AppNavBarLayout extends AppLayout
{
    public AppNavBarLayout()
    {
        // H1 title = new H1("Spring-Demo App");

        HorizontalLayout navbarPanel = new HorizontalLayout(Alignment.BASELINE);
        navbarPanel.setWidthFull();
        navbarPanel.getStyle().setBackgroundColor("grey");
        navbarPanel.getStyle().setHeight("80px");
        
        
        navbarPanel.add(new H2(" 🔒 Secure File Transfer"));
        navbarPanel.add(new RouterLink("Home Page", HomeView.class));
        navbarPanel.add(" | ");
        navbarPanel.add(new RouterLink("Login Page", LoginView.class));
        navbarPanel.add(" | ");   
        navbarPanel.add(new RouterLink("Upload Encrypt", UploadView.class));
        navbarPanel.add(" | ");
        navbarPanel.add(new Anchor("/book","Book page"));
        navbarPanel.add(" | ");
        Span space = new Span(" ");
        navbarPanel.add(space);
        navbarPanel.expand(space);
        
        Avatar userAvatar = new Avatar("Ori Parhi","https://images.icon-icons.com/1879/PNG/512/iconfinder-7-avatar-2754582_120519.png");
        userAvatar.getStyle().setMargin("0px");
        userAvatar.getStyle().setMarginTop("10px");

        navbarPanel.add(userAvatar);
        addToNavbar(navbarPanel);
    }
}
