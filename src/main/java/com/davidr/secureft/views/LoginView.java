package com.davidr.secureft.views;

import com.davidr.secureft.services.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route(value="/login", layout=AppNavBarLayout.class)
public class LoginView extends VerticalLayout {

    private LoginForm login = new LoginForm();

    public LoginView(UserService userService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Dialog dialog = new Dialog();
        dialog.add("I DONT CARE!\n");
        Button close = new Button("OK", e -> dialog.close());
        dialog.add(close);       
        
        com.vaadin.flow.component.html.Paragraph info = new com.vaadin.flow.component.html.Paragraph();
        info.add(new com.vaadin.flow.component.html.Span("Don't have an account? "));
        info.add(new com.vaadin.flow.component.html.Anchor("signin", "Create one"));
        
        login.addLoginListener(e -> {
            String username = e.getUsername();
            String password = e.getPassword();

            if (userService.checkLogin(username, password)) {
                login.getUI().ifPresent(ui -> ui.navigate("upload"));
            } else {
                login.getUI().ifPresent(ui -> ui.navigate("wrong"));
            }
        });
        login.addForgotPasswordListener(e -> {
            dialog.open();
        });

        add(login, info);
    }

    private void checkCoockie(){
        if(VaadinSession.getCurrent().getSession().getId()!=null){
            
        }
    }
}

