package com.davidr.secureft.views;

import java.net.URI;
import java.net.URISyntaxException;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.services.AuthService;
import com.davidr.secureft.services.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "users/settings", layout = AppNavBarLayout.class)
@PageTitle("Edit User Settings")
public class UserSettingsView extends VerticalLayout implements BeforeEnterObserver{

    private final PasswordField oldPass = new PasswordField("Old Password (must)");
    private final PasswordField newPass = new PasswordField("New Password (opt)");
    private final PasswordField againNewPass = new PasswordField("New Password Again");
    private final RadioButtonGroup<String> role = new RadioButtonGroup<>();
    private final TextField email = new TextField("New E-Mail (opt)");
    private final TextField username = new TextField("New Username (opt)");
    private final TextField avatarUrlField = new TextField("Avatar Image URL (opt)");
    private final Paragraph status = new Paragraph();
    private String avatarPath;
    private User loggedUser;



    private final AuthService authService;
    private final UserService userService;

    public UserSettingsView(AuthService authService, UserService userService) {

        this.authService = authService;
        this.userService = userService;

        setWidthFull();
        setMaxWidth(null);
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.START);

        role.setLabel("New Role");
        role.setItems("user", "admin");
        role.setValue("user");

        // status.getStyle()

        newPass.setValueChangeMode(ValueChangeMode.EAGER);
        newPass.addValueChangeListener(e -> checkPassword());

        againNewPass.setValueChangeMode(ValueChangeMode.EAGER);
        againNewPass.addValueChangeListener(e -> checkPassword());

        status.getStyle().set("white-space", "pre-line");
        status.getStyle().set("color", "red");
        avatarUrlField.setWidthFull();
        avatarUrlField.setPlaceholder("https://example.com/avatar.png");

        Button go = new Button("Update", e -> update());

        add(
                new H2("Update User Settings And Cradentials"),
                oldPass,
                username,
                newPass,
                againNewPass,
                status,
                email,
                role,
                avatarUrlField,
                go
            );
    }

    private void update() {
        if (loggedUser == null) {
            Notification.show("You must be logged in to update your settings.", 4000, Notification.Position.MIDDLE);
            return;
        }

        if (oldPass.isEmpty()) {
            Notification.show("Enter your current password to update your settings.", 4000, Notification.Position.MIDDLE);
            oldPass.setInvalid(true);
            return;
        }
        oldPass.setInvalid(false);

        if (!newPass.isEmpty() || !againNewPass.isEmpty()) {
            checkPassword();
            if (newPass.isInvalid() || againNewPass.isInvalid()) {
                Notification.show("Fix the password fields before updating.", 4000, Notification.Position.MIDDLE);
                return;
            }
        }

        String avatarInput = avatarUrlField.getValue() == null ? "" : avatarUrlField.getValue().trim();
        if (!avatarInput.isEmpty() && !isValidImageUrl(avatarInput)) {
            avatarUrlField.setInvalid(true);
            Notification.show("Enter a valid image URL.", 4000, Notification.Position.MIDDLE);
            return;
        }
        avatarUrlField.setInvalid(false);

        String newUsername = username.isEmpty() ? loggedUser.getUsername() : username.getValue().trim();
        String updatedPassword = newPass.isEmpty() ? oldPass.getValue() : newPass.getValue();
        String newEmail = email.isEmpty() ? loggedUser.getEmail() : email.getValue().trim();
        String newRole = role.isEmpty() ? loggedUser.getRole() : role.getValue();
        String newAvatar = avatarInput.isEmpty() ? loggedUser.getAvatarURL() : avatarInput;

        try {
            loggedUser = userService.updateUser(
                    loggedUser.getUsername(),
                    oldPass.getValue(),
                    newUsername,
                    updatedPassword,
                    newEmail,
                    newRole,
                    newAvatar);

            oldPass.clear();
            newPass.clear();
            againNewPass.clear();
            username.clear();
            email.clear();
            avatarPath = loggedUser.getAvatarURL();
            avatarUrlField.clear();
            role.setValue(loggedUser.getRole());
            status.setVisible(false);

            Notification.show("User settings updated successfully.", 3000, Notification.Position.TOP_CENTER);
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void checkPassword(){
        if(!(userService.isValidPassword(newPass.getValue())||userService.isValidPassword(againNewPass.getValue()))){
            status.setVisible(true);
            newPass.setInvalid(true);
            againNewPass.setInvalid(true);
            status.setText("Password must:\n- be at least 8 characters\n- contain uppercase\n- contain lowercase\n- contain a digit\n- contain a symbol");
        }else{
            if(!(newPass.getValue().equals(againNewPass.getValue()))){
                status.setVisible(true);
                newPass.setInvalid(true);
                againNewPass.setInvalid(true);
                status.setText("Passwords must match!");
            }else{
                newPass.setInvalid(false);
                againNewPass.setInvalid(false);
                status.setVisible(false);
            }
        }
        if(newPass.isEmpty() && againNewPass.isEmpty()){
            newPass.setInvalid(false);
            againNewPass.setInvalid(false);
            status.setVisible(false);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent e) {
        loggedUser = ViewAuthSupport.requireLoggedUser(authService, e);
        if (loggedUser == null) {
            return;
        }

        avatarPath = loggedUser.getAvatarURL();
        role.setValue(loggedUser.getRole());
        avatarUrlField.setValue(avatarPath == null ? "" : avatarPath);
    }

    private boolean isValidImageUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }

            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return true;
            }

            String loweredPath = path.toLowerCase();
            return loweredPath.endsWith(".jpg")
                    || loweredPath.endsWith(".jpeg")
                    || loweredPath.endsWith(".png")
                    || loweredPath.endsWith(".gif")
                    || loweredPath.endsWith(".bmp")
                    || loweredPath.endsWith(".webp")
                    || loweredPath.endsWith(".svg");
        } catch (URISyntaxException ex) {
            return false;
        }
    }
}
