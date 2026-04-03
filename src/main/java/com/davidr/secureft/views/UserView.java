package com.davidr.secureft.views;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.services.UserService;
import org.springframework.dao.DataAccessException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "users", layout = AppNavBarLayout.class)
@PageTitle("Users")
public class UserView extends VerticalLayout {

    private final UserService userService;
    private final Grid<User> userGrid = new Grid<>(User.class, false);
    private final TextField usernameField = new TextField("Username");
    private final PasswordField passwordField = new PasswordField("Password");

    public UserView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Users");

        usernameField.setClearButtonVisible(true);
        passwordField.setClearButtonVisible(true);

        Button addUserButton = new Button("Add User", e -> addUser());
        Button refreshButton = new Button("Refresh", e -> refreshGrid());

        HorizontalLayout formRow = new HorizontalLayout(usernameField, passwordField, addUserButton, refreshButton);
        formRow.setAlignItems(Alignment.END);

        userGrid.addColumn(User::getUsername).setHeader("Username").setAutoWidth(true);
        userGrid.addColumn(user -> user.getHashedPassword())
                .setHeader("Hashed Password")
                .setAutoWidth(true);
        userGrid.addColumn(user -> user.getEmail())
                .setHeader("Email")
                .setAutoWidth(true);
        userGrid.addColumn(user -> user.getRole())
                .setHeader("role")
                .setAutoWidth(true);
        userGrid.addColumn(user -> user.getCreatedAt().atZone(ZoneId.of("Asia/Jerusalem")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .setHeader("Created at")
                .setAutoWidth(true);
        userGrid.addColumn(user -> user.getLastLoginAt().atZone(ZoneId.of("Asia/Jerusalem")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .setHeader("Last Login At")
                .setAutoWidth(true);
        userGrid.setSizeFull();

        add(title, formRow, userGrid);
        expand(userGrid);

        refreshGrid();
    }

    private void addUser() {
        String username = usernameField.getValue() == null ? "" : usernameField.getValue().trim();
        String password = passwordField.getValue() == null ? "" : passwordField.getValue();

        if (username.isBlank() || password.isBlank()) {
            Notification n = Notification.show("Username and password are required");
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        User newUser = userService.newUser(username, password, "test@gmail.com");
        boolean created;
        try {
            created = userService.addUserToDB(newUser);
        } catch (DataAccessException ex) {
            Notification n = Notification.show("Database is not reachable right now");
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (!created) {
            Notification n = Notification.show("User already exists");
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        usernameField.clear();
        passwordField.clear();
        refreshGrid();

        Notification n = Notification.show("User added successfully");
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void refreshGrid() {
        try {
            List<User> users = userService.getAllUsers();
            userGrid.setItems(users);
        } catch (DataAccessException ex) {
            userGrid.setItems(List.of());
            Notification n = Notification.show("Could not load users: database connection failed");
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
