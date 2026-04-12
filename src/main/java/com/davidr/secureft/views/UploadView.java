package com.davidr.secureft.views;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.davidr.secureft.interfaces.CryptListener;
import com.davidr.secureft.services.UploadCryptService;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = AppNavBarLayout.class)
@PermitAll
@PageTitle("Encrypt and Download")
@SuppressWarnings("removal")
public class UploadView extends HorizontalLayout implements CryptListener {

    private static final String DEFAULT_STATUS = "Enter an encryption key and upload a file.";
    private static final String DEFAULT_EXPLANATION = "Upload a file, choose the mode you need, enter your key, and download the processed result when it is ready.";

    private final UploadCryptService uploadCryptService;
    private final Paragraph status = new Paragraph(DEFAULT_STATUS);
    private final ProgressBar bar = new ProgressBar();
    private final Pre explanation = new Pre(loadUploadExplanation());
    private final VerticalLayout formColumn = new VerticalLayout();
    private final FileBuffer buffer = new FileBuffer();
    private final Upload upload = new Upload(buffer);
    private final PasswordField keyField = new PasswordField("Encryption key");
    private final RadioButtonGroup<String> modeGroup = new RadioButtonGroup<>();
    private final RadioButtonGroup<String> integrityGroup = new RadioButtonGroup<>();

    private Anchor downloadLink;
    private Path encryptedTempFile;

    public UploadView(UploadCryptService uploadCryptService) {
        this.uploadCryptService = uploadCryptService;

        setWidthFull();
        setMaxWidth(null);
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.START);

        formColumn.setWidth("50%");
        explanation.setWidth("50%");
        explanation.addClassName("upload-explanation");
        formColumn.addClassName("upload-form-column");

        setFlexGrow(1, explanation);
        setFlexGrow(1, formColumn);

        upload.setMaxFiles(1);
        upload.addSucceededListener(event -> processUpload(event.getFileName()));
        upload.addFailedListener(event -> {
            bar.setVisible(false);
            status.setText("Upload failed.");
            Notification.show("Upload failed. Please try again.", 5000, Notification.Position.MIDDLE);
        });

        keyField.setWidthFull();

        modeGroup.setLabel("Encryption mode");
        modeGroup.setItems("Encrypt", "Decrypt");
        modeGroup.setValue("Encrypt");

        integrityGroup.setLabel("Integrity checking mode");
        integrityGroup.setItems("Regular", "HMAC-SHA256");
        integrityGroup.setValue("Regular");

        bar.setIndeterminate(true);
        bar.setVisible(false);

        explanation.getStyle().set("white-space", "pre-wrap");

        formColumn.add(
                new H2("Encrypt and Download"),
                modeGroup,
                integrityGroup,
                keyField,
                bar,
                upload,
                buildButtons(),
                status);

        add(formColumn, explanation);
    }

    private HorizontalLayout buildButtons() {
        Button clear = new Button("Clear", event -> clearForm());
        Button retry = new Button("Retry", event -> retryForm());
        return new HorizontalLayout(clear, retry);
    }

    private void processUpload(String fileName) {
        if (keyField.isEmpty()) {
            showValidationFailure("Enter an encryption key before uploading a file.");
            return;
        }

        boolean encrypt = "Encrypt".equals(modeGroup.getValue());
        boolean hmac = "HMAC-SHA256".equals(integrityGroup.getValue());

        resetDownloadLink();
        status.setText("Preparing file for processing...");
        bar.setVisible(true);

        try (InputStream in = buffer.getInputStream()) {
            deleteEncryptedTempFile();
            encryptedTempFile = Files.createTempFile("secureft-", ".enc");

            try (OutputStream out = Files.newOutputStream(encryptedTempFile)) {
                uploadCryptService.cryptStream(encrypt, hmac, in, out, keyField.getValue(), this);
            }

            showSuccess(fileName, encrypt);
        } catch (IllegalArgumentException ex) {
            handleProcessingFailure("Invalid input", getUserMessage(ex));
        } catch (SecurityException ex) {
            handleProcessingFailure(
                    "Verification failed",
                    "The file could not be verified. Check the key and make sure the file was not modified.");
        } catch (IllegalStateException ex) {
            handleProcessingFailure(
                    "Cryptography unavailable",
                    "The required cryptography support is unavailable on the server.");
        } catch (IOException ex) {
            handleProcessingFailure(ex);
        }
    }

    private void showSuccess(String fileName, boolean encrypt) {
        String processedName = uploadCryptService.getFileName(encrypt, fileName);
        StreamResource resource = new StreamResource(processedName, this::openProcessedFile);

        downloadLink = new Anchor(resource, "Download processed file: " + processedName);
        downloadLink.getElement().setAttribute("download", true);

        status.setText("Processing complete. Click the link to download.");
        Notification.show("File processed successfully.", 2500, Notification.Position.TOP_CENTER);
        formColumn.add(downloadLink);
    }

    private InputStream openProcessedFile() {
        try {
            if (encryptedTempFile == null || !Files.exists(encryptedTempFile)) {
                throw new IOException("Processed file is no longer available.");
            }
            return Files.newInputStream(encryptedTempFile);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void clearForm() {
        upload.clearFileList();
        keyField.clear();
        modeGroup.setValue("Encrypt");
        integrityGroup.setValue("Regular");
        bar.setVisible(false);
        status.setText(DEFAULT_STATUS);
        resetDownloadLink();
        deleteEncryptedTempFile();
    }

    private void retryForm() {
        upload.clearFileList();
        bar.setVisible(false);
        status.setText(DEFAULT_STATUS);
        resetDownloadLink();
        deleteEncryptedTempFile();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        deleteEncryptedTempFile();
        super.onDetach(detachEvent);
    }

    @Override
    public void start() {
        bar.setVisible(true);
        status.setText("Processing started...");
    }

    @Override
    public void progress(int percent) {
        status.setText("Processing... " + percent + "%");
    }

    @Override
    public void end() {
        status.setText("Processing complete.");
        bar.setVisible(false);
    }

    private void handleProcessingFailure(Exception ex) {
        handleProcessingFailure("Processing failed", getUserMessage(ex));
    }

    private void handleProcessingFailure(String title, String message) {
        bar.setVisible(false);
        status.setText(message);
        deleteEncryptedTempFile();
        resetDownloadLink();
        Notification.show(title + ": " + message, 5000, Notification.Position.MIDDLE);
    }

    private void resetDownloadLink() {
        if (downloadLink == null) {
            return;
        }
        if (downloadLink.getParent().isPresent()) {
            formColumn.remove(downloadLink);
        }
        downloadLink = null;
    }

    private void deleteEncryptedTempFile() {
        if (encryptedTempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(encryptedTempFile);
        } catch (IOException ignored) {
        }
        encryptedTempFile = null;
    }

    private void showValidationFailure(String message) {
        bar.setVisible(false);
        status.setText(message);
        Notification.show(message, 3000, Notification.Position.MIDDLE);
    }

    private String getUserMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Processing failed due to an unexpected error.";
        }
        if (ex instanceof IOException) {
            return "The file could not be processed. " + message;
        }
        return message;
    }

    private static String loadUploadExplanation() {
        try (InputStream in = UploadView.class.getResourceAsStream("/static/UploadExplanation.txt")) {
            if (in == null) {
                return DEFAULT_EXPLANATION;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return DEFAULT_EXPLANATION;
        }
    }
}
