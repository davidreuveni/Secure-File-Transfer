package com.davidr.secureft.views;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.services.AuthService;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import jakarta.servlet.http.HttpServletRequest;

public final class ViewAuthSupport {
    public static final String POST_LOGIN_REDIRECT_KEY = "postLoginRedirect";

    private ViewAuthSupport() {
    }

    public static User requireLoggedUser(AuthService authService, BeforeEnterEvent event) {
        HttpServletRequest request = (HttpServletRequest) VaadinService.getCurrentRequest();
        User loggedUser = authService.getLoggedUser(request);

        if (loggedUser == null) {
            String intendedRoute = event.getLocation().getPathWithQueryParameters();
            VaadinSession.getCurrent().setAttribute(POST_LOGIN_REDIRECT_KEY, intendedRoute);
            event.rerouteTo(LoginView.class);
            return null;
        }

        return loggedUser;
    }
}
