package com.davidr.secureft.views;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.security.SecurityService;
import com.davidr.secureft.services.UserService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@JavaScript("./webrtc-file.js")
@JavaScript("./webrtc-connection.js")
@Route(value = "users/WebRTC", layout = AppNavBarLayout.class)
@PermitAll
@PageTitle("open a secure connection with another browser")
@SuppressWarnings("removal")
public class WebRTCView extends VerticalLayout implements BeforeEnterObserver {

    private final TextField username = new TextField("Username to open a connection with");
    private final TextField message = new TextField("Message");
    private final TextField secretField = new TextField("Secret");
    private final Paragraph status = new Paragraph();
    private final TextArea transcript = new TextArea("Connection log");
    private final Div localIdentity = new Div();
    private final Paragraph transferStatus = new Paragraph();
    private final ProgressBar transferProgress = new ProgressBar();
    private final Button callButton = new Button("Call", e -> startCall());
    private final Button sendButton = new Button("Send", e -> sendMessage());
    private final Button hangupButton = new Button("Hang Up", e -> hangUp());
    private final Button secretButton = new Button("Exchange Secret", e -> exchangeSecret());
    private User loggedUser;
    private final MemoryBuffer fileBuffer = new MemoryBuffer();
    private final Upload upload = new Upload(fileBuffer);
    private final SecurityService securityService;

    public WebRTCView(UserService userService, SecurityService securityService) {
        this.securityService = securityService;
        setWidthFull();
        setMaxWidth(null);
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.STRETCH);

        username.setWidth("24rem");
        message.setWidth("24rem");
        secretField.setWidth("24rem");
        secretButton.setWidth("24rem");

        upload.setMaxFiles(1);
        upload.setDropAllowed(false);

        status.getStyle().set("white-space", "pre-line");

        transferStatus.getStyle().set("margin", "0");

        transferProgress.setWidth("24rem");
        transferProgress.setMin(0);
        transferProgress.setMax(1);
        transferProgress.setValue(0);
        transferProgress.setVisible(false);

        transcript.setWidthFull();
        transcript.setMinHeight("22rem");
        transcript.setReadOnly(true);

        HorizontalLayout actions = new HorizontalLayout(callButton, sendButton, hangupButton);
        actions.setPadding(false);

        add(
                new H2("Secure peer connection"),
                localIdentity,
                username,
                message,
                secretField,
                upload,
                actions,
                secretButton,
                status,
                transferStatus,
                transferProgress,
                transcript);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        loggedUser = securityService.getCurrentUser();
        if (loggedUser == null) {
            event.forwardTo(LoginView.class);
            return;
        }

        localIdentity.setText("Logged in as: " + loggedUser.getUsername());
    }

    private void startCall() {
        if (loggedUser == null) {
            Notification.show("You must be logged in to start a connection.", 4000, Notification.Position.MIDDLE);
            return;
        }

        String target = username.getValue() == null ? "" : username.getValue().trim();
        if (target.isBlank()) {
            Notification.show("Enter a username to call.", 4000, Notification.Position.MIDDLE);
            return;
        }
        appendTranscript("me: calling " + target);
        getElement().executeJs("window.secureftRtc.startCall($0)", target);
    }

    private void sendMessage() {
        String outgoing = message.getValue() == null ? "" : message.getValue().trim();

        if (!outgoing.isBlank()) {
            appendTranscript("me: " + outgoing);
            getElement().executeJs("window.secureftRtc.sendMessage($0)", outgoing);
            message.clear();
            return;
        }

        sendSelectedFile();
    }

    private void hangUp() {
        getElement().executeJs("window.secureftRtc.hangUp()");
    }

    private void exchangeSecret() {
        String value = secretField.getValue();
        if (value != null && !value.trim().isEmpty()) {
            appendTranscript("me: sending secret encrypted with RSA: " + value);
            getElement().executeJs("window.secureftRtc.exchangeSecretWithRSA($0)", value);
            secretField.clear();
        }
    }

    @ClientCallable
    public void updateStatus(String newStatus, boolean error) {
        status.setText(newStatus == null ? "" : newStatus);
        status.getStyle().set("color", error ? "red" : "green");
    }

    @ClientCallable
    public void appendRemoteMessage(String author, String body) {
        appendTranscript((author == null || author.isBlank() ? "peer" : author) + ": " + (body == null ? "" : body));
    }

    @ClientCallable
    public void appendSystemMessage(String body) {
        appendTranscript("[system] " + (body == null ? "" : body));
    }

    @ClientCallable
    public void showTransferProgress(String label, boolean indeterminate) {
        transferStatus.setText(label == null || label.isBlank() ? "Working..." : label);
        transferProgress.setIndeterminate(indeterminate);
        if (!indeterminate) {
            transferProgress.setValue(0);
        }
        transferProgress.setVisible(true);
    }

    @ClientCallable
    public void updateTransferProgress(String label, double progress) {
        transferStatus.setText(label == null || label.isBlank() ? "Working..." : label);
        transferProgress.setIndeterminate(false);
        transferProgress.setValue(Math.max(0, Math.min(1, progress)));
        transferProgress.setVisible(true);
    }

    @ClientCallable
    public void hideTransferProgress(String label) {
        transferStatus.setText(label == null || label.isBlank() ? null : label);
        transferProgress.setIndeterminate(false);
        transferProgress.setValue(0);
        transferProgress.setVisible(false);
    }

    private void appendTranscript(String line) {
        String current = transcript.getValue();
        if (current == null || current.isBlank()) {
            transcript.setValue(line);
        } else {
            transcript.setValue(current + System.lineSeparator() + line);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (loggedUser != null) {
            getElement().executeJs(
                    "window.secureftRtc.init($0, $1)",
                    getElement(),
                    loggedUser.getUsername());
        }
    }

    private void sendSelectedFile() {
        if (fileBuffer.getFileName() == null || fileBuffer.getFileName().isBlank()) {
            Notification.show("Choose a file first.", 3000, Notification.Position.MIDDLE);
            return;
        }

        try (InputStream in = fileBuffer.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String fileName = fileBuffer.getFileName();
            String mime = fileBuffer.getFileData().getMimeType() == null ? "application/octet-stream"
                    : fileBuffer.getFileData().getMimeType();

            appendTranscript("me: sending file " + fileName + " (" + bytes.length + " bytes)");

            getElement().executeJs(
                    "window.secureftRtc.sendFile($0, $1, $2)",
                    fileName,
                    mime,
                    base64);
        } catch (IOException e) {
            Notification.show("Failed to read selected file.", 4000, Notification.Position.MIDDLE);
        }
    }
}
