package com.davidr.secureft.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "login", layout = AppNavBarLayout.class)
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();
    private final Button googleLogin = createGoogleLoginButton();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Dialog forgotPass = new Dialog();
        forgotPass.add("I DONT CARE!\n");
        Button close = new Button("OK", e -> forgotPass.close());
        forgotPass.add(close);

        com.vaadin.flow.component.html.Paragraph info = new com.vaadin.flow.component.html.Paragraph();
        info.add(new com.vaadin.flow.component.html.Span("Don't have an account? "));
        info.add(new com.vaadin.flow.component.html.Anchor("signin", "Create one"));
        login.setAction("login");
        login.addForgotPasswordListener(e -> {
            forgotPass.open();
        });

        add(login, info, googleLogin);
    }

    private Button createGoogleLoginButton() {
        Image googleLogo = new Image(
                "https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg",
                "Google logo");
        googleLogo.setWidth("18px");
        googleLogo.setHeight("18px");

        Button googleLogin = new Button();
        googleLogin.setText("Sign in with Google");
        googleLogin.setIcon(googleLogo);
        googleLogin.addClickListener(event -> UI.getCurrent().getPage().setLocation("/oauth2/authorization/google"));
        googleLogin.getStyle().set("font-weight", "600");
        googleLogin.getStyle().set("width", "23rem");
        return googleLogin;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        login.setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
    }
}
