package com.davidr.secureft.views;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.security.SecurityService;
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
import com.vaadin.flow.theme.lumo.Lumo;

public class AppNavBarLayout extends AppLayout {
    private static final String THEME_SESSION_KEY = "secureft-theme";
    private final SecurityService securityService;

    public AppNavBarLayout(SecurityService securityService) {
        this.securityService = securityService;
        applySavedTheme();

        HorizontalLayout navbarPanel = new HorizontalLayout();
        navbarPanel.addClassName("app-navbar");
        navbarPanel.setWidthFull();
        navbarPanel.setPadding(true);
        navbarPanel.setSpacing(true);
        navbarPanel.setAlignItems(Alignment.CENTER);

        RouterLink title = new RouterLink("Secure File Transfer", UploadView.class);
        title.addClassName("app-navbar-title");

        User loggedUser = securityService.getCurrentUser();

        HorizontalLayout links = new HorizontalLayout();
        links.addClassName("app-navbar-links");
        links.setSpacing(true);
        links.setPadding(false);
        links.setAlignItems(Alignment.CENTER);
        links.add(new RouterLink("Upload", UploadView.class));
        links.add(new Anchor("/book", "Book"));
        if (loggedUser == null) {
            links.add(new RouterLink("Login", LoginView.class));
            links.add(new RouterLink("Sign Up", SignInView.class));
        } else {
            links.add(new RouterLink("Users", UserView.class));
            links.add(new RouterLink("File Transfer", WebRTCView.class));
        }

        navbarPanel.add(title, links);

        Span space = new Span(" ");
        navbarPanel.add(space);
        navbarPanel.expand(space);

        Avatar userAvatar = new Avatar();
        if (loggedUser == null) {
            userAvatar.setName("Guest user");
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

        ContextMenu avatarMenu = createAvatarMenu(avatarButton, loggedUser);

        navbarPanel.add(avatarButton);
        addToNavbar(navbarPanel);
        addToNavbar(avatarMenu);
    }

    private ContextMenu createAvatarMenu(Div avatarButton, User loggedUser) {
        ContextMenu menu = new ContextMenu();
        menu.setTarget(avatarButton);
        menu.setOpenOnClick(true);

        if (loggedUser == null) {
            menu.addItem("Login", event -> UI.getCurrent().navigate(LoginView.class));
            menu.addItem("Create account", event -> UI.getCurrent().navigate(SignInView.class));
        } else {
            menu.addItem("Settings", event -> UI.getCurrent().navigate(UserSettingsView.class));
            menu.addItem("Log out", event -> logOut());
        }

        final MenuItem[] themeItemRef = new MenuItem[1];
        themeItemRef[0] = menu.addItem("", event -> {
            boolean dark = !isDarkTheme();
            setDarkTheme(dark);
            updateThemeMenuText(themeItemRef[0], dark);
        });
        updateThemeMenuText(themeItemRef[0], isDarkTheme());
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

    private void logOut() {
        UI.getCurrent().getPage().setLocation("/logout");
    }
}
