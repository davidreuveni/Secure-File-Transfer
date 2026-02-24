package com.davidr.secureft.views;

import java.time.LocalTime;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route(value = "", layout = AppNavBarLayout.class)
public class HomeView extends VerticalLayout {

    private TextArea textArea;

    public HomeView() {

        add(new H1("WEBSITE!!!!!!!!!!!!!!!!!!!!!!!"));
        add(new H4("Session id: " + VaadinSession.getCurrent().getSession().getId()));
        add(new Anchor("/dog", "Dogs! How PHUN!!"));
        add(new Anchor("/login", "To upload a dog image LOGIN!"));
        add(new Anchor("/signin", "Dont have an account?? SIGN IN only 9999.99$ a day!"));

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.add(new Button("Add Time Stamp", e -> textArea.setValue(textArea.getValue() + "\n" + LocalTime.now())));
        buttons.add(new Button("Clear", e -> textArea.clear()));
        add(buttons);

        textArea = new TextArea();
        textArea.setHeight(300, Unit.PIXELS);
        textArea.setWidth(600, Unit.PIXELS);
        textArea.setLabel("Tiredness or not to be");
        add(textArea);
    }
}
