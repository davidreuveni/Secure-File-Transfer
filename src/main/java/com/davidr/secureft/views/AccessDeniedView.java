package com.davidr.secureft.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@ParentLayout(AppNavBarLayout.class)
@AnonymousAllowed
public class AccessDeniedView extends VerticalLayout implements HasErrorParameter<AccessDeniedException> {

    public AccessDeniedView() {
        addClassName("access-denied-page");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Icon icon = new Icon(VaadinIcon.LOCK);
        icon.addClassName("access-denied-icon");

        H1 title = new H1("Access denied");
        title.addClassName("access-denied-title");

        Paragraph message = new Paragraph(
                "Your account does not have permission to open this page. Sign in with an authorized account or return to the welcome page.");
        message.addClassName("access-denied-message");

        Button home = new Button("Go to welcome", new Icon(VaadinIcon.HOME));
        home.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        home.addClickListener(event -> UI.getCurrent().navigate(WelcomeView.class));

        Button login = new Button("Switch account", new Icon(VaadinIcon.SIGN_IN));
        login.addClickListener(event -> UI.getCurrent().navigate(LoginView.class));

        HorizontalLayout actions = new HorizontalLayout(home, login);
        actions.addClassName("access-denied-actions");
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.setAlignItems(Alignment.CENTER);

        add(icon, title, message, actions);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        return HttpStatusCode.FORBIDDEN.getCode();
    }
}
