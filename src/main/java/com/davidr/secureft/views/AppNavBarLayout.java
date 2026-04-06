package com.davidr.secureft.views;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.services.AuthService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.theme.lumo.Lumo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AppNavBarLayout extends AppLayout {
    private static final String THEME_SESSION_KEY = "secureft-theme";

    public AppNavBarLayout(AuthService authService) {
        applySavedTheme();

        HorizontalLayout navbarPanel = new HorizontalLayout();
        navbarPanel.addClassName("app-navbar");
        navbarPanel.setWidthFull();
        navbarPanel.setPadding(true);
        navbarPanel.setSpacing(true);
        navbarPanel.setAlignItems(Alignment.CENTER);

        RouterLink title = new RouterLink("Secure File Transfer", UploadView.class);
        title.addClassName("app-navbar-title");

        HorizontalLayout links = new HorizontalLayout();
        links.addClassName("app-navbar-links");
        links.setSpacing(true);
        links.setPadding(false);
        links.setAlignItems(Alignment.CENTER);
        links.add(
                new RouterLink("Upload", UploadView.class),
                new RouterLink("Login", LoginView.class),
                new RouterLink("Users", UserView.class),
                new Anchor("/book", "Book"),
                new RouterLink("File Transfer", WebRTCView.class));

        navbarPanel.add(title, links);

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
        userAvatar.addClassName("app-navbar-avatar");

        Div avatarButton = new Div(userAvatar);
        avatarButton.addClassName("app-navbar-avatar-button");
        avatarButton.getElement().setAttribute("aria-label", "Open user menu");
        avatarButton.getElement().setAttribute("role", "button");
        avatarButton.getElement().setAttribute("tabindex", "0");

        ContextMenu avatarMenu = createAvatarMenu(avatarButton, authService);

        navbarPanel.add(avatarButton);
        addToNavbar(navbarPanel);
        addToNavbar(avatarMenu);
    }

    private ContextMenu createAvatarMenu(Div avatarButton, AuthService authService) {
        ContextMenu menu = new ContextMenu();
        menu.setTarget(avatarButton);
        menu.setOpenOnClick(true);

        menu.addItem("Settings", event -> UI.getCurrent().navigate(UserSettingsView.class));

        final MenuItem[] themeItemRef = new MenuItem[1];
        themeItemRef[0] = menu.addItem("", event -> {
            boolean dark = !isDarkTheme();
            setDarkTheme(dark);
            updateThemeMenuText(themeItemRef[0], dark);
        });
        updateThemeMenuText(themeItemRef[0], isDarkTheme());

        menu.addItem("Log out", event -> logOut(authService));
        return menu;
    }

    private void updateThemeMenuText(MenuItem item, boolean dark) {
        item.setText(dark ? "Light theme" : "Dark theme");
    }

    private void applySavedTheme() {
        String savedTheme = (String) VaadinSession.getCurrent().getAttribute(THEME_SESSION_KEY);
        setDarkTheme(savedTheme == null || "dark".equals(savedTheme));
    }

    private boolean isDarkTheme() {
        return UI.getCurrent().getElement().getThemeList().contains(Lumo.DARK);
    }

    private void setDarkTheme(boolean dark) {
        if (dark) {
            UI.getCurrent().getElement().getThemeList().add(Lumo.DARK);
            VaadinSession.getCurrent().setAttribute(THEME_SESSION_KEY, "dark");
        } else {
            UI.getCurrent().getElement().getThemeList().remove(Lumo.DARK);
            VaadinSession.getCurrent().setAttribute(THEME_SESSION_KEY, "light");
        }
    }

    private void logOut(AuthService authService) {
        HttpServletRequest request = (HttpServletRequest) VaadinService.getCurrentRequest();
        HttpServletResponse response = null;
        if (VaadinService.getCurrentResponse() instanceof VaadinServletResponse vsr) {
            response = vsr.getHttpServletResponse();
        }

        if (request != null && response != null) {
            authService.logUserOut(request, response);
        }

        UI.getCurrent().getPage().setLocation("/login");
    }
}
