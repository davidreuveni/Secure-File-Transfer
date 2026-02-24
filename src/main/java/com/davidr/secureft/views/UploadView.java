package com.davidr.secureft.views;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.davidr.secureft.services.UploadCryptService;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

@Route(value = "upload", layout = AppNavBarLayout.class)
@PageTitle("Encrypt and Download")
public class UploadView extends VerticalLayout {

    private final Paragraph status = new Paragraph("Upload a file and enter an encryption key.");
    private Anchor downloadLink = new Anchor();
    private Path encryptedTempFile;
    private boolean mode, hmac;

    public UploadView(UploadCryptService uploadCryptService) {
        setWidthFull();
        setMaxWidth("720px");

        H2 title = new H2("Encrypt and Download");

        PasswordField keyField = new PasswordField("Encryption key");
        keyField.setWidthFull();

        FileBuffer buffer = new FileBuffer();
        Upload upload = new Upload(buffer);
        upload.setMaxFiles(1);

        downloadLink.setVisible(false);

        RadioButtonGroup<String> radioGroup = new RadioButtonGroup<>();
        radioGroup.setLabel("Encryption mode");
        radioGroup.setItems("Encrypt", "Decrypt");
        radioGroup.setValue("Encrypt");

        RadioButtonGroup<String> radioGroup2 = new RadioButtonGroup<>();
        radioGroup2.setLabel("Integrity checking mode");
        radioGroup2.setItems("Regular", "HMAC-SHA256");
        radioGroup2.setValue("Regular");

        upload.addSucceededListener(e -> {
            if (keyField.getValue() == null || keyField.getValue().isBlank()) {
                Notification.show("Enter an encryption key first.", 3000, Notification.Position.MIDDLE);
                return;
            }

            if(radioGroup.getValue() == "Encrypt"){mode = true;}
            else {mode = false;}
            if(radioGroup.getValue() == "Regular"){hmac = false;}
            else {hmac = true;}
            try (InputStream in = buffer.getInputStream()) {
                deleteEncryptedTempFile();
                encryptedTempFile = Files.createTempFile("secureft-", ".enc");

                try (OutputStream out = Files.newOutputStream(encryptedTempFile)) {
                    uploadCryptService.cryptStream(mode, hmac, in, out, keyField.getValue());
                }

                if (downloadLink != null && getChildren().anyMatch(component -> component == downloadLink)) {
                    remove(downloadLink);
                }


                System.out.println(mode);
                String encryptedName = uploadCryptService.getFileName(mode, e.getFileName());
                StreamResource resource = new StreamResource(
                        encryptedName,
                        () -> {
                            try {
                                return Files.newInputStream(encryptedTempFile);
                            } catch (IOException ioEx) {
                                throw new RuntimeException(ioEx);
                            }
                        });

                downloadLink = new Anchor(resource, "Download processed file: " + encryptedName);
                downloadLink.getElement().setAttribute("download", true);

                status.setText("Processing complete. Click the link to download.");
                Notification.show("File processed successfully.", 2500, Notification.Position.TOP_CENTER);

                add(downloadLink);
            } catch (Exception ex) {
                status.setText("Processing failed.");
                Notification.show("Processing failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });

        upload.addFailedListener(e -> Notification.show("Upload failed", 5000, Notification.Position.MIDDLE));

        Button clear = new Button("Clear", click -> {
            upload.clearFileList();
            keyField.clear();
            status.setText("Upload a file and enter an encryption / decryption key.");
            if (downloadLink != null && getChildren().anyMatch(component -> component == downloadLink)) {
                remove(downloadLink);
            }
            deleteEncryptedTempFile();
        });

        add(title, radioGroup, radioGroup2, keyField, upload, clear, status);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        deleteEncryptedTempFile();
        super.onDetach(detachEvent);
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

}
