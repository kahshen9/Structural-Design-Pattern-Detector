package org.vaadin.example;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.component.upload.receivers.FileData;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import elemental.json.Json;

import java.io.ByteArrayInputStream;
import java.io.File;


/**
 * A sample Vaadin view class.
 * <p>
 * To implement a Vaadin view just extend any Vaadin component and use @Route
 * annotation to announce it in a URL as a Spring managed bean.
 * <p>
 * A new instance of this class is created for every new user and every browser
 * tab/window.
 * <p>
 * The main view contains a text field for getting the user name and a button
 * that shows a greeting message in a notification.
 */
@PageTitle("Structural Design Pattern Detector")
@Route("")
public class MainView extends VerticalLayout
{
    File cdFile, javaFile;
    FileBuffer CDBuffer, JavaBuffer;
    Upload uploadCD, uploadJava;
    String javaFileName, cdFileName;
    Detector detector;
    String detectionResult;

    public MainView()
    {
        HorizontalLayout mainView = new HorizontalLayout();
        VerticalLayout functions = new VerticalLayout();

        functions.setPadding(true);
        functions.setJustifyContentMode(JustifyContentMode.CENTER);
        functions.setAlignItems(Alignment.CENTER);
        functions.setWidth("55%");
        detector = new Detector();

        functions.add(new H1("Structural Design Pattern Detector"));
        functions.add(new UploadBar());
        functions.add(new ButtonBar());

        VerticalLayout arrowManual = new VerticalLayout();

        Span arrow = new Span("Class Diagram Arrow Manuals");
        arrow.addClassName("arrow-manual");
        StreamResource imageResource = new StreamResource("Arrow manual.png",
                () -> getClass().getResourceAsStream("/Arrow manual.png"));
        Image image = new Image(imageResource, "Arrow Manual");
        arrowManual.add(arrow);
        arrowManual.add(image);
        arrowManual.setJustifyContentMode(JustifyContentMode.CENTER);
        arrowManual.setAlignItems(Alignment.CENTER);
        arrowManual.setWidth("30%");

        mainView.add(arrowManual);
        mainView.add(functions);
        mainView.setSizeFull();

        add(mainView);
        setPadding(true);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        setSizeFull();
    }

    class UploadBar extends HorizontalLayout
    {
        public UploadBar()
        {
            HorizontalLayout uploadBar = new HorizontalLayout();
            uploadBar.setPadding(true);
            uploadBar.setJustifyContentMode(JustifyContentMode.CENTER);
            uploadBar.setWidthFull();
            uploadBar.setHeightFull();
            addClassName("Upload-bar");

            // CD upload
            CDBuffer = new FileBuffer();
            uploadCD = new Upload(CDBuffer);
            uploadCD.setUploadButton(new Button("Upload Class Diagrams"));
            uploadCD.setDropLabel(new Span("Drop ZIP or UXF file here"));
            uploadCD.setDropAllowed(true);  // Drag & drop

            uploadCD.addSucceededListener(event -> {
                // Get information about the uploaded file
                cdFileName = event.getFileName();
                FileData fileData = CDBuffer.getFileData();
                cdFile = fileData.getFile();
            });

            // Remove file
            uploadCD.getElement().addEventListener("file-remove", event ->
            {
                if (cdFile != null) {
                    try {
                        cdFile.delete(); // Delete file in buffer
                        cdFile = null;
                        uploadCD.getElement().setPropertyJson("files", Json.createArray()); // Clear front-end file lists
                    } catch (Exception ignore) {
                    }
                }
            }).addEventData("event.detail.file.name");

            // Restrict upload file type
            uploadCD.setAcceptedFileTypes(".zip", ".uxf");

            uploadCD.addFileRejectedListener(event -> {
                String errorMessage = event.getErrorMessage();

                Notification notification = Notification.show(errorMessage, 5000,
                        Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            });

            // Java upload
            JavaBuffer = new FileBuffer();
            uploadJava = new Upload(JavaBuffer);
            uploadJava.setUploadButton(new Button("Upload Java Programs"));
            uploadJava.setDropLabel(new Span("Drop ZIP or JAVA file here"));
            uploadJava.setDropAllowed(true);    // Drag & drop

            uploadJava.addSucceededListener(event -> {
                // Get information about the uploaded file
                javaFileName = event.getFileName();
                System.out.println(javaFileName);
                FileData fileData = JavaBuffer.getFileData();
                javaFile = fileData.getFile();
            });

            // Remove file
            uploadJava.getElement().addEventListener("file-remove", event ->
            {
                if (javaFile != null) {
                    try {
                        javaFile.delete();
                        javaFile = null;
                        uploadJava.getElement().setPropertyJson("files", Json.createArray());
                    } catch (Exception ignore) {
                    }
                }
            }).addEventData("event.detail.file.name");

            // Restrict upload file type
            uploadJava.setAcceptedFileTypes(".zip", ".java");

            uploadJava.addFileRejectedListener(event -> {
                String errorMessage = event.getErrorMessage();

                Notification notification = Notification.show(errorMessage, 5000,
                        Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            });

            add(uploadCD, uploadJava);
        }
    }

    class ButtonBar extends HorizontalLayout
    {
        public ButtonBar()
        {
            HorizontalLayout buttonBar = new HorizontalLayout();
            buttonBar.setPadding(true);
            buttonBar.setJustifyContentMode(JustifyContentMode.CENTER);
            buttonBar.addClassName("Button-bar");

            // Error message (empty file upload)
            Notification emptyFileNotification = new Notification();
            emptyFileNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);

            Div text = new Div(new Text("No file uploaded."));
            text.addClassName("text");

            Button closeButton = new Button(new Icon("lumo", "cross"));
            closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            closeButton.setAriaLabel("Close");
            closeButton.addClickListener(event -> {
                emptyFileNotification.close();
            });
            closeButton.addClassName("closeNotification");

            HorizontalLayout errorMessageLayout = new HorizontalLayout(text, closeButton);
            errorMessageLayout.setAlignItems(Alignment.CENTER);

            emptyFileNotification.add(errorMessageLayout);
            emptyFileNotification.setPosition(Notification.Position.TOP_CENTER);

            // Detection result dialog
            Dialog detectionDialog = new Dialog();
            detectionDialog.setHeaderTitle("Detection Result");

            Span span = new Span(detectionResult);
            span.setWidth("100%");
            span.getStyle()
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis");
            detectionDialog.add(span);

            Button downloadButton = new Button("Download");
            Anchor downloadLink = new Anchor();

            Button doneButton = new Button("Done", e -> detectionDialog.close());
            detectionDialog.getFooter().add(doneButton);

            add(detectionDialog);

            // Detect button
            // Button click listeners can be defined as lambda expressions
            Button detectButton = new Button("Detect", e -> {
                if (cdFile != null && javaFile != null)
                {
                    try {
                        detectionResult = detector.detectMostSimilarCode (cdFile, cdFileName, javaFile, javaFileName);

                        // Remove previous download link if exists
                        detectionDialog.getFooter().remove(downloadLink);

                        // Add download result button in dialog
                        StreamResource detectionResource = new StreamResource("Pattern_Detection_Result.txt",
                                () -> new ByteArrayInputStream(detectionResult.getBytes()));
                        downloadLink.setHref(detectionResource);

                        // Update the anchor and add it to the dialog footer
                        downloadLink.setHref(detectionResource);
                        downloadLink.getElement().setAttribute("download", true);
                        downloadLink.getElement().appendChild(downloadButton.getElement());
                        detectionDialog.getFooter().add(downloadLink);

                        // Display result in dialog
                        span.setText(detectionResult); // Update the span with the result
                        span.getElement().setProperty("innerHTML", detectionResult.replaceAll("\n", "<br>"));
                        detectionDialog.open();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
                else
                    emptyFileNotification.open();
            });
            detectButton.addClickShortcut(Key.ENTER);

            // Cancel button
            Button cancelButton = new Button("Cancel", e -> {
                if (cdFile != null) {
                    try {
                        cdFile.delete(); // Delete file in buffer
                        cdFile = null;
                        uploadCD.getElement().setPropertyJson("files", Json.createArray()); // Clear front-end file lists
                    } catch (Exception ignore) {
                    }
                }

                if (javaFile != null) {
                    javaFile.delete();
                    javaFile = null;
                    uploadJava.getElement().setPropertyJson("files", Json.createArray());
                }
            });

            add(detectButton, cancelButton);
        }
    }
}
