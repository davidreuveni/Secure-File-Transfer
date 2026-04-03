package com.davidr.secureft.views;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.services.AuthService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinService;

import jakarta.servlet.http.HttpServletRequest;

public class AppNavBarLayout extends AppLayout {

    public AppNavBarLayout(AuthService authService) {
        // H1 title = new H1("Spring-Demo App");

        HorizontalLayout navbarPanel = new HorizontalLayout(Alignment.BASELINE);
        navbarPanel.setWidthFull();
        navbarPanel.getStyle().setBackgroundColor("grey");
        navbarPanel.getStyle().setHeight("80px");

        navbarPanel.add(new H2(" 🔒 Secure File Transfer"));
        navbarPanel.add(new RouterLink("Login Page", LoginView.class));
        navbarPanel.add(" | ");
        navbarPanel.add(new RouterLink("Upload Encrypt", UploadView.class));
        navbarPanel.add(" | ");
        navbarPanel.add(new RouterLink("Users", UserView.class));
        navbarPanel.add(" | ");
        navbarPanel.add(new Anchor("/book", "Book page"));
        navbarPanel.add(" | ");
        navbarPanel.add(new RouterLink("User settings", UserSettingsView.class));
        navbarPanel.add(" | ");
        Span space = new Span(" ");
        navbarPanel.add(space);
        navbarPanel.expand(space);

        HttpServletRequest request = (HttpServletRequest) VaadinService.getCurrentRequest();
        User loggedUser = authService.getLoggedUser(request);

        Avatar userAvatar = new Avatar();
        if (loggedUser == null) {
            userAvatar.setName("guest user");
            userAvatar.setImage("https://ohsobserver.com/wp-content/uploads/2022/12/Guest-user.png");
        } else {
            userAvatar.setName(loggedUser.getUsername());
            userAvatar.setImage(loggedUser.getAvatarURL());
        }
        userAvatar.getStyle().setMargin("0px");
        userAvatar.getStyle().setMarginTop("10px");

        navbarPanel.add(userAvatar);
        addToNavbar(navbarPanel);
    }
}
