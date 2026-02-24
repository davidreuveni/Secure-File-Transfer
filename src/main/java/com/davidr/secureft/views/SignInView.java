package com.davidr.secureft.views;

import com.vaadin.flow.component.notification.Notification;

import java.time.LocalDate;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.services.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route(value="signin", layout=AppNavBarLayout.class)
public class SignInView extends VerticalLayout {

    private UserService userService;

    public SignInView(UserService userService) {

        this.userService = userService;

        User newUser = new User();

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        DatePicker date = new DatePicker("Date Of Birth");
        

        Button DBAdder = new Button("add user to db", e -> addUserToDB(username.getValue(), password.getValue(), date.getValue()));

        // Center layout
        setSizeFull();
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        add(username, password, date, DBAdder);
    }

    public void addUserToDB(String un, String pw, LocalDate localDate){
        User user = new User(un,pw);
        user.setBirthDate(localDate);
        
        boolean res = userService.addUserToDB(user);

        if(res){
            Notification suc = Notification.show("user added succesfully!!!");
            suc.addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS);
            suc.open();
            UI.getCurrent().navigate("login");
            return;
        }
        if (!res) { 
            Notification error = Notification.show("user already exists!!!");
            error.addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
            error.open();
            return;
        }
        
    }
}
