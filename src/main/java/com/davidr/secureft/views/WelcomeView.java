package com.davidr.secureft.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "", layout = AppNavBarLayout.class)
@AnonymousAllowed
@PageTitle("Welcome | Secure File Transfer")
public class WelcomeView extends VerticalLayout {

    private final Button encryptMode = createModeButton("Encrypt", VaadinIcon.LOCK);
    private final Button verifyMode = createModeButton("Verify", VaadinIcon.CHECK_CIRCLE);
    private final Button sendMode = createModeButton("Send", VaadinIcon.PAPERPLANE);
    private final H2 previewTitle = new H2();
    private final Paragraph previewText = new Paragraph();
    private final Span previewMetric = new Span();

    public WelcomeView() {
        addClassName("welcome-page");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.STRETCH);

        add(buildHero(), buildFlowSection());
        selectMode(encryptMode, "Military-grade file encryption",
                "Protect files before they leave your browser, then download the processed result when it is ready.",
                "AES + HMAC ready");
    }

    private Div buildHero() {
        Div hero = new Div();
        hero.addClassName("welcome-hero");

        Div content = new Div();
        content.addClassName("welcome-hero-content");

        Span eyebrow = new Span("SecureFT");
        eyebrow.addClassName("welcome-eyebrow");

        H1 title = new H1("Welcome to Secure File Transfer");
        title.addClassName("welcome-title");

        Paragraph subtitle = new Paragraph(
                "Encrypt, verify, and move sensitive files through a focused workspace built for privacy and speed.");
        subtitle.addClassName("welcome-subtitle");

        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("welcome-actions");
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.setAlignItems(Alignment.CENTER);

        Button start = new Button("Start encrypting", new Icon(VaadinIcon.ARROW_RIGHT));
        start.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        start.addClickListener(event -> UI.getCurrent().navigate(UploadView.class));

        Button transfer = new Button("Open transfer", new Icon(VaadinIcon.EXCHANGE));
        transfer.addClickListener(event -> UI.getCurrent().navigate(WebRTCView.class));
        actions.add(start, transfer);

        Div stats = new Div();
        stats.addClassName("welcome-stats");
        stats.add(createStat("Local-first", "Keys stay with your session"),
                createStat("SHA-256", "Optional integrity check"),
                createStat("WebRTC", "Direct file handoff"));

        content.add(eyebrow, title, subtitle, actions, stats);

        Div visual = new Div();
        visual.addClassName("welcome-visual");
        Image image = new Image("/images/welcome-secure-transfer.png", "Secure encrypted file transfer");
        image.addClassName("welcome-visual-image");
        visual.add(image, buildPreviewPanel());

        hero.add(content, visual);
        return hero;
    }

    private Div buildPreviewPanel() {
        Div panel = new Div();
        panel.addClassName("welcome-preview");

        HorizontalLayout modes = new HorizontalLayout(encryptMode, verifyMode, sendMode);
        modes.addClassName("welcome-mode-tabs");
        modes.setPadding(false);
        modes.setSpacing(false);
        modes.setAlignItems(Alignment.CENTER);

        encryptMode.addClickListener(event -> selectMode(encryptMode, "Military-grade file encryption",
                "Protect files before they leave your browser, then download the processed result when it is ready.",
                "AES + HMAC ready"));
        verifyMode.addClickListener(event -> selectMode(verifyMode, "Integrity checks on demand",
                "Use HMAC-SHA256 mode to detect tampering before trusting a decrypted file.",
                "Tamper-aware"));
        sendMode.addClickListener(event -> selectMode(sendMode, "Peer-to-peer file transfer",
                "Use the transfer workspace to connect sessions and hand files directly between users.",
                "Realtime channel"));

        previewTitle.addClassName("welcome-preview-title");
        previewText.addClassName("welcome-preview-text");
        previewMetric.addClassName("welcome-preview-metric");

        panel.add(modes, previewTitle, previewText, previewMetric);
        return panel;
    }

    private Div buildFlowSection() {
        Div section = new Div();
        section.addClassName("welcome-flow-section");

        H2 heading = new H2("A clear path from file to verified result");
        heading.addClassName("welcome-section-heading");

        Div steps = new Div();
        steps.addClassName("welcome-flow-grid");
        steps.add(createStep(VaadinIcon.UPLOAD, "Upload", "Choose a file and the operation you need."),
                createStep(VaadinIcon.KEY, "Key", "Enter the shared encryption key for processing."),
                createStep(VaadinIcon.DOWNLOAD, "Download", "Save the encrypted or decrypted output."),
                createStep(VaadinIcon.SHIELD, "Trust", "Add HMAC verification when integrity matters."));

        section.add(heading, steps);
        return section;
    }

    private Div createStat(String value, String label) {
        Div stat = new Div();
        stat.addClassName("welcome-stat");
        Span valueSpan = new Span(value);
        valueSpan.addClassName("welcome-stat-value");
        Span labelSpan = new Span(label);
        labelSpan.addClassName("welcome-stat-label");
        stat.add(valueSpan, labelSpan);
        return stat;
    }

    private Div createStep(VaadinIcon iconType, String title, String text) {
        Div step = new Div();
        step.addClassName("welcome-step");
        Icon icon = new Icon(iconType);
        icon.addClassName("welcome-step-icon");
        Span titleSpan = new Span(title);
        titleSpan.addClassName("welcome-step-title");
        Paragraph textParagraph = new Paragraph(text);
        textParagraph.addClassName("welcome-step-text");
        step.add(icon, titleSpan, textParagraph);
        return step;
    }

    private Button createModeButton(String text, VaadinIcon iconType) {
        Button button = new Button(text, new Icon(iconType));
        button.addClassName("welcome-mode-button");
        return button;
    }

    private void selectMode(Button selected, String title, String text, String metric) {
        encryptMode.removeClassName("selected");
        verifyMode.removeClassName("selected");
        sendMode.removeClassName("selected");
        selected.addClassName("selected");
        previewTitle.setText(title);
        previewText.setText(text);
        previewMetric.setText(metric);
    }
}
