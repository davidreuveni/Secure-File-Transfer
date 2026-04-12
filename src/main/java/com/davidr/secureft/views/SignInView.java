package com.davidr.secureft.views;

import java.net.URI;
import java.net.URISyntaxException;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.services.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
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
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "signin", layout = AppNavBarLayout.class)
@AnonymousAllowed
@PageTitle("Create Account")
public class SignInView extends VerticalLayout {
    private static final String FORM_WIDTH = "23rem";

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

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        username.setRequired(true);
        username.setWidth(FORM_WIDTH);
        email.setRequiredIndicatorVisible(true);
        email.setClearButtonVisible(true);
        email.setWidth(FORM_WIDTH);

        password.setRequired(true);
        password.setWidth(FORM_WIDTH);
        confirmPassword.setRequired(true);
        confirmPassword.setWidth(FORM_WIDTH);

        avatarUrl.setPlaceholder("https://example.com/avatar.png");
        avatarUrl.setWidth(FORM_WIDTH);

        password.setValueChangeMode(ValueChangeMode.EAGER);
        password.addValueChangeListener(event -> validatePasswordFields());

        confirmPassword.setValueChangeMode(ValueChangeMode.EAGER);
        confirmPassword.addValueChangeListener(event -> validatePasswordFields());

        status.getStyle().set("white-space", "pre-line");
        status.getStyle().set("color", "red");
        status.setVisible(false);
        status.setWidth(FORM_WIDTH);

        Button googleLogin = createGoogleLoginButton();

        Button createAccount = new Button("Create Account", event -> registerUser());
        createAccount.setWidth(FORM_WIDTH);

        add(
                new H2("Create Account"),
                username,
                email,
                password,
                confirmPassword,
                avatarUrl,
                status,
                createAccount,
                googleLogin
            );
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
        googleLogin.setWidth(FORM_WIDTH);
        return googleLogin;
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
