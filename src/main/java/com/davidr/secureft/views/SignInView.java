package com.davidr.secureft.views;

import java.net.URI;
import java.net.URISyntaxException;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.services.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "signin", layout = AppNavBarLayout.class)
@PageTitle("Create Account")
public class SignInView extends VerticalLayout {

    private final UserService userService;

    private final TextField username = new TextField("Username");
    private final EmailField email = new EmailField("E-Mail");
    private final PasswordField password = new PasswordField("Password");
    private final PasswordField confirmPassword = new PasswordField("Password Again");
    private final TextField avatarUrl = new TextField("Avatar Image URL (opt)");
    private final Paragraph status = new Paragraph();
    private final boolean BYPASS_PASS_CHECK = true;

    public SignInView(UserService userService) {
        this.userService = userService;

        setWidthFull();
        setMaxWidth("34rem");
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.STRETCH);

        username.setRequired(true);
        email.setRequiredIndicatorVisible(true);
        email.setClearButtonVisible(true);

        password.setRequired(true);
        confirmPassword.setRequired(true);

        avatarUrl.setPlaceholder("https://example.com/avatar.png");

        password.setValueChangeMode(ValueChangeMode.EAGER);
        password.addValueChangeListener(event -> validatePasswordFields());

        confirmPassword.setValueChangeMode(ValueChangeMode.EAGER);
        confirmPassword.addValueChangeListener(event -> validatePasswordFields());

        status.getStyle().set("white-space", "pre-line");
        status.getStyle().set("color", "red");
        status.setVisible(false);

        Button createAccount = new Button("Create Account", event -> registerUser());

        add(
                new H2("Create Account"),
                username,
                email,
                password,
                confirmPassword,
                avatarUrl,
                status,
                createAccount);
    }

    private void registerUser() {
        if (username.isEmpty()) {
            username.setInvalid(true);
            showError("Enter a username.");
            return;
        }
        username.setInvalid(false);

        if (email.isEmpty()) {
            email.setInvalid(true);
            showError("Enter your email.");
            return;
        }
        email.setInvalid(false);

        validatePasswordFields();
        if (password.isInvalid() || confirmPassword.isInvalid()) {
            showError("Fix the password fields before creating the account.");
            return;
        }

        String avatarInput = avatarUrl.getValue() == null ? "" : avatarUrl.getValue().trim();
        if (!avatarInput.isEmpty() && !isValidImageUrl(avatarInput)) {
            avatarUrl.setInvalid(true);
            showError("Enter a valid image URL.");
            return;
        }
        avatarUrl.setInvalid(false);

        try {
            User user = userService.newUser(
                    username.getValue().trim(),
                    password.getValue(),
                    email.getValue().trim());

            if (!avatarInput.isEmpty()) {
                user.setAvatarURL(avatarInput);
            }

            boolean created = userService.addUserToDB(user);
            if (!created) {
                showError("User already exists.");
                return;
            }

            Notification success = Notification.show("Account created successfully.", 3000,
                    Notification.Position.TOP_CENTER);
            success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate(LoginView.class);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void validatePasswordFields() {
        if (!BYPASS_PASS_CHECK) {
            if (password.isEmpty() && confirmPassword.isEmpty()) {
                password.setInvalid(false);
                confirmPassword.setInvalid(false);
                status.setVisible(false);
                return;
            }

            if (!userService.isValidPassword(password.getValue())
                    || !userService.isValidPassword(confirmPassword.getValue())) {
                password.setInvalid(true);
                confirmPassword.setInvalid(true);
                status.setText(
                        "Password must:\n- be at least 8 characters\n- contain uppercase\n- contain lowercase\n- contain a digit\n- contain a symbol");
                status.setVisible(true);
                return;
            }

            if (!password.getValue().equals(confirmPassword.getValue())) {
                password.setInvalid(true);
                confirmPassword.setInvalid(true);
                status.setText("Passwords must match!");
                status.setVisible(true);
                return;
            }

            password.setInvalid(false);
            confirmPassword.setInvalid(false);
            status.setVisible(false);
        }
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

    private void showError(String message) {
        status.setText(message);
        status.setVisible(true);
        Notification error = Notification.show(message, 4000, Notification.Position.MIDDLE);
        error.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
