package com.example.ui;

import com.example.data.DatabaseHandler;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.ProgressBar;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import com.example.util.LicenseManager;
import com.example.util.UpdateManager;

public class SettingsView {

    private static final String LOGO_DIR = System.getProperty("user.home") + "/StudentManagement/Logos";

    public SettingsView() {
        new File(LOGO_DIR).mkdirs();
    }

    public Parent getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f8f9fa;");

        Label header = new Label("System Settings");
        header.getStyleClass().add("header-label");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab genTab = new Tab("General", createGeneralSettings());
        Tab secTab = new Tab("Security", createSecuritySettings());
        Tab stgTab = new Tab("Storage", createStorageSettings());

        Tab migTab = new Tab("Full Migration");
        migTab.setContent(createMigrationSettings(migTab));

        Tab patchTab = new Tab("🧩 Upgrades & Patches", createPatchSettings());

        Tab syncTab = new Tab("☁ Cloud Sync", createCloudSyncSettings());

        tabs.getTabs().addAll(genTab, secTab, stgTab, migTab, patchTab, syncTab);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        root.getChildren().addAll(header, tabs);
        return root;
    }

    private VBox createGeneralSettings() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));

        Label header = new Label("General System Settings");
        header.getStyleClass().add("header-label");

        // Institution Name
        Label nameLabel = new Label("Institution Name:");
        TextField nameField = new TextField();
        String currentName = DatabaseHandler.getSetting("institution_name");
        if (currentName != null)
            nameField.setText(currentName);

        // Logo Upload
        Label logoLabel = new Label("Institute Logo:");
        ImageView logoView = new ImageView();
        logoView.setFitHeight(100);
        logoView.setFitWidth(100);
        logoView.setPreserveRatio(true);
        loadLogo(logoView);

        Button uploadBtn = new Button("Upload Logo");
        uploadBtn.getStyleClass().add("secondary-button");
        uploadBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"));
            File f = fc.showOpenDialog(null);
            if (f != null)
                saveLogo(f, logoView);
        });

        // Save Button
        Button saveBtn = new Button("Save Settings");
        saveBtn.getStyleClass().add("action-button");
        saveBtn.setOnAction(e -> {
            DatabaseHandler.saveSetting("institution_name", nameField.getText());
            new Alert(Alert.AlertType.INFORMATION, "Settings Saved.").show();
        });

        root.getChildren().addAll(header, nameLabel, nameField, logoLabel, logoView, uploadBtn, new Separator(),
                saveBtn);
        return root;
    }

    private VBox createStorageSettings() {
        VBox storageBox = new VBox(20);
        storageBox.setPadding(new Insets(30));

        Label storageHeader = new Label("Database & System Storage Status");
        storageHeader.getStyleClass().add("header-label");

        File dbFile = new File("student_management.db");
        long dbSize = dbFile.exists() ? dbFile.length() : 0;
        long freeSpace = new File(".").getUsableSpace();
        long totalSpace = new File(".").getTotalSpace();

        String dbSizeStr = String.format("%.2f MB", dbSize / (1024.0 * 1024.0));
        String freeStr = String.format("%.2f GB", freeSpace / (1024.0 * 1024.0 * 1024.0));

        HBox statsBox = new HBox(20);
        VBox dbStat = createStatBox("Database Size", dbSizeStr, "#6c5ce7");
        VBox freeStat = createStatBox("Free Space", freeStr, "#00b894");
        statsBox.getChildren().addAll(dbStat, freeStat);

        Label diskLabel = new Label("System Disk Usage (Overall)");
        diskLabel.setStyle("-fx-text-fill: #636e72; -fx-font-size: 14px;");

        ProgressBar storageBar = new ProgressBar();
        storageBar.setMaxWidth(Double.MAX_VALUE);
        storageBar.setPrefHeight(15);
        double progress = totalSpace > 0 ? (double) (totalSpace - freeSpace) / totalSpace : 0;
        storageBar.setProgress(progress);

        storageBox.getChildren().addAll(storageHeader, statsBox, new Separator(), diskLabel, storageBar);

        ScrollPane scroll = new ScrollPane(storageBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private void saveLogo(File f, ImageView view) {
        try {
            Path dest = Paths.get(LOGO_DIR, "logo_" + System.currentTimeMillis() + ".png");
            Files.copy(f.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            DatabaseHandler.saveSetting("logo_path", dest.toString());
            view.setImage(new Image("file:" + dest.toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadLogo(ImageView view) {
        String path = DatabaseHandler.getSetting("logo_path");
        if (path != null && new File(path).exists()) {
            view.setImage(new Image("file:" + path));
        }
    }

    private VBox createStatBox(String label, String value, String color) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: " + color + "11; -fx-background-radius: 10; -fx-border-color: " + color
                + "; -fx-border-width: 1; -fx-min-width: 150;");

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label labLbl = new Label(label);
        labLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #636e72;");

        box.getChildren().addAll(valLbl, labLbl);
        return box;
    }

    private VBox createSecuritySettings() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(20));

        // Change Password
        Label passHeader = new Label("Offline Access Password");
        passHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        PasswordField oldPass = new PasswordField();
        oldPass.setPromptText("Old Password");
        oldPass.setMaxWidth(300);
        PasswordField newPass = new PasswordField();
        newPass.setPromptText("New Password");
        newPass.setMaxWidth(300);
        PasswordField confirmPass = new PasswordField();
        confirmPass.setPromptText("Confirm New Password");
        confirmPass.setMaxWidth(300);

        Button changePassBtn = new Button("Update Password");
        changePassBtn.getStyleClass().add("action-button");
        changePassBtn.setOnAction(e -> {
            String stored = DatabaseHandler.getSetting("admin_password");
            if (stored == null || stored.isEmpty())
                stored = "admin"; // Default

            if (!stored.equals(oldPass.getText())) {
                new Alert(Alert.AlertType.ERROR, "Incorrect Old Password").show();
                return;
            }
            if (!newPass.getText().equals(confirmPass.getText())) {
                new Alert(Alert.AlertType.ERROR, "Passwords do not match").show();
                return;
            }
            DatabaseHandler.saveSetting("admin_password", newPass.getText());
            new Alert(Alert.AlertType.INFORMATION, "Password Updated Successfully").show();
            oldPass.clear();
            newPass.clear();
            confirmPass.clear();
        });

        // Security Questions
        Label secHeader = new Label("Security Questions (Recovery)");
        secHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        TextField q1 = new TextField();
        q1.setPromptText("Question 1");
        q1.setMaxWidth(400);
        TextField a1 = new TextField();
        a1.setPromptText("Answer 1");
        a1.setMaxWidth(400);
        TextField q2 = new TextField();
        q2.setPromptText("Question 2");
        q2.setMaxWidth(400);
        TextField a2 = new TextField();
        a2.setPromptText("Answer 2");
        a2.setMaxWidth(400);

        String sq1 = DatabaseHandler.getSetting("sec_q1");
        if (sq1 != null)
            q1.setText(sq1);
        String sa1 = DatabaseHandler.getSetting("sec_a1");
        if (sa1 != null)
            a1.setText(sa1);
        String sq2 = DatabaseHandler.getSetting("sec_q2");
        if (sq2 != null)
            q2.setText(sq2);
        String sa2 = DatabaseHandler.getSetting("sec_a2");
        if (sa2 != null)
            a2.setText(sa2);

        Button saveSecBtn = new Button("Save Security Answers");
        saveSecBtn.getStyleClass().add("action-button");
        saveSecBtn.setOnAction(e -> {
            DatabaseHandler.saveSetting("sec_q1", q1.getText());
            DatabaseHandler.saveSetting("sec_a1", a1.getText());
            DatabaseHandler.saveSetting("sec_q2", q2.getText());
            DatabaseHandler.saveSetting("sec_a2", a2.getText());
            new Alert(Alert.AlertType.INFORMATION, "Security Questions Saved").show();
        });

        box.getChildren().addAll(passHeader, new Label("Default password is 'admin'"), oldPass, newPass, confirmPass,
                changePassBtn,
                new Separator(), secHeader,
                new Label("Question 1:"), q1, a1,
                new Label("Question 2:"), q2, a2, saveSecBtn);

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private VBox createMigrationSettings(Tab parentTab) {
        VBox migrationBox = new VBox(15);
        migrationBox.setPadding(new Insets(30));

        Label migrationHeader = new Label("🔄 Full System Migration / Transfer");
        migrationHeader.getStyleClass().add("header-label");

        Label migrationInfo = new Label(
                "Use these options to move your entire system including students, programs, financial records, and photos to a new computer.");
        migrationInfo.setWrapText(true);
        migrationInfo.setStyle("-fx-font-size: 13px; -fx-text-fill: #636e72;");

        VBox packageBox = new VBox(15);
        packageBox.getStyleClass().add("card");
        packageBox.setPadding(new Insets(20));

        Button exportPackBtn = new Button("Export Full System Package (.zip)");
        exportPackBtn.getStyleClass().add("action-button");
        exportPackBtn.setMaxWidth(Double.MAX_VALUE);

        Button importPackBtn = new Button("Import System Package");
        importPackBtn.getStyleClass().add("info-button");
        importPackBtn.setMaxWidth(Double.MAX_VALUE);

        packageBox.getChildren().addAll(
                new Label("📦 System Package Backup"),
                new Label("Recommended for large systems with many student photos."),
                exportPackBtn, importPackBtn);

        migrationBox.getChildren().addAll(migrationHeader, migrationInfo, packageBox);

        VBox wipeBox = new VBox(15);
        wipeBox.getStyleClass().add("card");
        wipeBox.setPadding(new Insets(20));
        wipeBox.setStyle("-fx-border-color: #ff7675; -fx-border-width: 1; -fx-background-color: #fff5f5;");

        Button wipeBtn = new Button("⚠️ Wipe All System Data");
        wipeBtn.getStyleClass().add("delete-button");
        wipeBtn.setMaxWidth(Double.MAX_VALUE);

        Label wipeHint = new Label(
                "Dangerous: This will permanently delete all student, course, lecturer, and transaction data. Use with extreme caution.");
        wipeHint.setWrapText(true);
        wipeHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #d63031;");

        wipeBox.getChildren().addAll(new Label("🗑️ Data Cleanup"), wipeHint, wipeBtn);
        migrationBox.getChildren().add(wipeBox);

        // Logic
        exportPackBtn.setOnAction(e -> handleExportPackage());
        importPackBtn.setOnAction(e -> handleImportPackage(parentTab));
        wipeBtn.setOnAction(e -> handleWipeData(parentTab));

        return migrationBox;
    }

    private void handleWipeData(Tab parentTab) {
        Alert migrateAlert = new Alert(Alert.AlertType.CONFIRMATION);
        migrateAlert.setTitle("Migration Check");
        migrateAlert.setHeaderText("Has the system data been migrated?");
        migrateAlert.setContentText(
                "Wiping will permanently delete all current records. Have you exported a migration package or backup first?");
        ButtonType yesBtn = new ButtonType("Yes, Proceed", ButtonBar.ButtonData.OK_DONE);
        ButtonType noBtn = new ButtonType("No, Go Back", ButtonBar.ButtonData.CANCEL_CLOSE);
        migrateAlert.getButtonTypes().setAll(yesBtn, noBtn);

        if (migrateAlert.showAndWait().orElse(noBtn) != yesBtn)
            return;

        Alert firstCheck = new Alert(Alert.AlertType.CONFIRMATION);
        firstCheck.setTitle("Final System Warning");
        firstCheck.setHeaderText("PERMANENT DELETION WARNING");
        firstCheck.setContentText(
                "This action will DELETE all students, programs, marks, attendance, and financial records.\n"
                        + "This cannot be undone. Do you want to proceed to final verification?");

        if (firstCheck.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            TextInputDialog secondCheck = new TextInputDialog();
            secondCheck.setTitle("Final Verification");
            secondCheck.setHeaderText("Security Check Required");
            secondCheck.setContentText("Please type 'WIPE' in the box below to confirm permanent deletion:");

            secondCheck.showAndWait().ifPresent(input -> {
                if ("WIPE".equalsIgnoreCase(input.trim())) {
                    DatabaseHandler.wipeAllData();
                    new Alert(Alert.AlertType.INFORMATION, "✅ All system data has been wiped successfully.").show();
                    parentTab.setContent(createMigrationSettings(parentTab));
                } else {
                    new Alert(Alert.AlertType.ERROR, "Verification failed. Wipe cancelled.").show();
                }
            });
        }
    }

    private void handleExportPackage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Migration Package");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Package", "*.zip"));
        fc.setInitialFileName("SMS_Full_Migration_" + java.time.LocalDate.now() + ".zip");
        File file = fc.showSaveDialog(null);
        if (file == null)
            return;

        try {
            File dbFile = new File(DatabaseHandler.getDatabaseFilePath());
            File photosDir = new File(System.getProperty("user.home") + "/StudentManagement/Photos");

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
                if (dbFile.exists()) {
                    zos.putNextEntry(new ZipEntry("student_management.db"));
                    Files.copy(dbFile.toPath(), zos);
                    zos.closeEntry();
                }
                if (photosDir.exists() && photosDir.isDirectory()) {
                    File[] photos = photosDir.listFiles();
                    if (photos != null) {
                        for (File p : photos) {
                            zos.putNextEntry(new ZipEntry("Photos/" + p.getName()));
                            Files.copy(p.toPath(), zos);
                            zos.closeEntry();
                        }
                    }
                }
            }
            new Alert(Alert.AlertType.INFORMATION, "✅ System Package Exported Successfully!").show();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Export Failed: " + ex.getMessage()).show();
        }
    }

    private void handleImportPackage(Tab parentTab) {
        Alert confirm = new Alert(Alert.AlertType.WARNING,
                "CRITICAL: Importing will OVERWRITE your current system data.\nContinue?",
                ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES)
            return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Select Migration Package");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Package", "*.zip"));
        File file = fc.showOpenDialog(null);
        if (file == null)
            return;

        try {
            String dbPath = DatabaseHandler.getDatabaseFilePath();
            String photosDirPath = System.getProperty("user.home") + "/StudentManagement/Photos";

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("student_management.db")) {
                        Files.copy(zis, Paths.get(dbPath), StandardCopyOption.REPLACE_EXISTING);
                    } else if (entry.getName().startsWith("Photos/")) {
                        File pDir = new File(photosDirPath);
                        pDir.mkdirs();
                        String fName = entry.getName().substring(7);
                        if (!fName.isEmpty()) {
                            Files.copy(zis, Paths.get(photosDirPath, fName), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    zis.closeEntry();
                }
            }
            DatabaseHandler.saveSetting("system_migrated", "true");
            new Alert(Alert.AlertType.INFORMATION,
                    "✅ System Restored! The 'Wipe' option is now enabled. Please restart the application for full effect.")
                    .show();
            parentTab.setContent(createMigrationSettings(parentTab));
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Import Failed: " + ex.getMessage()).show();
        }
    }

    private VBox createPatchSettings() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        Label title = new Label("🧩 Functional Extensions & System Patches");
        title.getStyleClass().add("header-label");

        Label desc = new Label(
                "Upload '.jar' files provided by developers to add new features or patch system logic without needing an internet connection.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #64748b;");

        VBox uploadBox = new VBox(15);
        uploadBox.getStyleClass().add("card");
        uploadBox.setPadding(new Insets(25));
        uploadBox.setAlignment(Pos.CENTER);

        Label iconLbl = new Label("📥");
        iconLbl.setStyle("-fx-font-size: 40px;");

        Button uploadBtn = new Button("Upload Functional Patch (.jar)");
        uploadBtn.getStyleClass().add("action-button");
        uploadBtn.setPrefWidth(300);

        Label statusLbl = new Label("Current System Extensions: " + ExtensionLoader.getLoadedModules().size());
        statusLbl.setStyle("-fx-font-style: italic;");

        uploadBox.getChildren().addAll(iconLbl, new Label("Functional Upgrade Terminal"), uploadBtn, statusLbl);

        VBox logBox = new VBox(5);
        Label logTitle = new Label("Patch Diagnostic Log (Technical):");
        logTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(120);
        logArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px;");

        StringBuilder sb = new StringBuilder();
        for (String log : ExtensionLoader.getLoadLogs()) {
            sb.append(log).append("\n");
        }
        logArea.setText(sb.toString());

        logBox.getChildren().addAll(logTitle, logArea);

        VBox downgradeBox = new VBox(15);
        downgradeBox.getStyleClass().add("card");
        downgradeBox.setPadding(new Insets(25));
        downgradeBox.setAlignment(Pos.CENTER);

        Label dgTitle = new Label("Installed Patches & Downgrades");
        dgTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        ListView<String> patchesList = new ListView<>();
        patchesList.setPrefHeight(100);
        Runnable refreshPatches = () -> {
            patchesList.getItems().clear();
            File dir = new File(ExtensionLoader.getExtensionPath());
            if (dir.exists()) {
                File[] files = dir.listFiles((d, n) -> n.endsWith(".jar"));
                if (files != null) {
                    for (File f : files)
                        patchesList.getItems().add(f.getName());
                }
            }
        };
        refreshPatches.run();

        Button removeBtn = new Button("Remove Selected Patch (Downgrade)");
        removeBtn.getStyleClass().add("danger-button");
        removeBtn.setDisable(true);
        patchesList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            removeBtn.setDisable(newV == null);
        });

        removeBtn.setOnAction(e -> {
            String selected = patchesList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Downgrade system and remove " + selected + "?",
                        ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        try {
                            // Release locks
                            ExtensionLoader.clearModules();

                            Files.deleteIfExists(Paths.get(ExtensionLoader.getExtensionPath(), selected));

                            // Re-init other patches
                            ExtensionLoader.reload();
                            DashboardView.clearViewCache();

                            refreshPatches.run();
                            statusLbl
                                    .setText("Current System Extensions: " + ExtensionLoader.getLoadedModules().size());

                            new Alert(Alert.AlertType.INFORMATION,
                                    "Patch removed successfully. System has been downgraded to the original structure.")
                                    .showAndWait();
                        } catch (Exception ex) {
                            String errorMsg = ex.getMessage();
                            if (errorMsg.contains("used by another process")) {
                                errorMsg += "\n\nPlease close the application first and manually delete the file from: "
                                        + ExtensionLoader.getExtensionPath();
                            }
                            new Alert(Alert.AlertType.ERROR, "Failed to remove patch: " + errorMsg).show();

                            // Try to recover state
                            ExtensionLoader.reload();
                        }
                    }
                });
            }
        });

        downgradeBox.getChildren().addAll(dgTitle, patchesList, removeBtn);

        uploadBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Functional Patch JAR");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Archive", "*.jar"));
            File file = chooser.showOpenDialog(null);
            if (file != null) {
                try {
                    Path dest = Paths.get(ExtensionLoader.getExtensionPath(), file.getName());
                    Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                    statusLbl.setText("✅ Patch '" + file.getName() + "' installed successfully.");

                    // Reload and Refresh Cache
                    ExtensionLoader.reload();
                    DashboardView.clearViewCache();

                    refreshPatches.run();

                    // Update logArea if possible (need to re-render the whole thing or make it
                    // accessible)
                    // For now, simpler:
                    new Alert(Alert.AlertType.INFORMATION,
                            "Upgrade successful! The new functionality is now active. Please click the sidebar buttons to see changes.")
                            .show();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Failed to install patch: " + ex.getMessage()).show();
                }
            }
        });

        box.getChildren().addAll(title, desc, uploadBox, logBox, downgradeBox);

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private VBox createCloudSyncSettings() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        Label header = new Label("☁ Cloud Synchronization");
        header.getStyleClass().add("header-label");

        Label desc = new Label("Push all your local database records to Supabase Cloud for real-time access by the LMS.");
        desc.setWrapText(true);

        // --- License & Status Section ---
        VBox licenseBox = new VBox(10);
        licenseBox.getStyleClass().add("card");
        licenseBox.setPadding(new Insets(15));
        licenseBox.setStyle("-fx-background-color: #f1f2f6; -fx-border-color: #dfe4ea; -fx-border-radius: 5;");

        Label licenseTitle = new Label("📄 License Information");
        licenseTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        String currentKey = LicenseManager.getLicenseKey();
        boolean isReg = LicenseManager.isRegistered();
        
        Label keyLabel = new Label("License Key: " + currentKey);
        keyLabel.setStyle("-fx-font-family: 'Consolas';");
        
        Label statusBadge = new Label(isReg ? "✅ REGISTERED" : "⚠️ GUEST / TRIAL");
        statusBadge.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (isReg ? "#27ae60" : "#e67e22") + ";");

        licenseBox.getChildren().addAll(licenseTitle, keyLabel, statusBadge);
        // ---------------------------------

        // --- Connection Check Section ---
        HBox connBox = new HBox(15);
        connBox.setAlignment(Pos.CENTER_LEFT);
        
        Button checkConnBtn = new Button("Check Connection");
        checkConnBtn.getStyleClass().add("secondary-button");
        
        Label connStatusLbl = new Label("Status: Unknown");
        connStatusLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #636e72;");
        
        checkConnBtn.setOnAction(e -> {
            connStatusLbl.setText("Checking...");
            connStatusLbl.setStyle("-fx-text-fill: #e17055;");
            new Thread(() -> {
                boolean isConnected = false;
                try {
                    // Try to open a quick connection to Supabase to verify internet and DB status
                    try (java.sql.Connection conn = com.example.data.SupabaseConnection.getConnection()) {
                        isConnected = (conn != null && !conn.isClosed());
                    }
                } catch (Exception ex) {
                    isConnected = false;
                }
                
                final boolean result = isConnected;
                javafx.application.Platform.runLater(() -> {
                    if (result) {
                        connStatusLbl.setText("🟢 ONLINE - Connected to Supabase Cloud");
                        connStatusLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #00b894;");
                    } else {
                        connStatusLbl.setText("🔴 OFFLINE - No Internet or DB unreachable");
                        connStatusLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #d63031;");
                    }
                });
            }).start();
        });
        
        connBox.getChildren().addAll(checkConnBtn, connStatusLbl);
        // --------------------------------

        Button syncSchemaBtn = new Button("Sync Database Schema Only");
        syncSchemaBtn.getStyleClass().add("secondary-button");

        Button syncBtn = new Button("Transfer Data to Cloud");
        syncBtn.getStyleClass().add("action-button");
        syncBtn.setStyle("-fx-background-color: #0984e3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");

        HBox actionBox = new HBox(15);
        actionBox.getChildren().addAll(syncSchemaBtn, syncBtn);

        Label statusLbl = new Label("Status: Ready");
        statusLbl.setStyle("-fx-font-weight: bold;");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        ListView<String> logList = new ListView<>();
        logList.getItems().addAll(DatabaseHandler.getCloudSyncLogs());
        VBox.setVgrow(logList, Priority.ALWAYS);

        syncBtn.setOnAction(e -> {
            syncBtn.setDisable(true);
            progressBar.setVisible(true);
            progressBar.setProgress(-1); // Indeterminate
            statusLbl.setText("Status: Transferring data... Please wait.");
            statusLbl.setStyle("-fx-text-fill: #e17055; -fx-font-weight: bold;");

            new Thread(() -> {
                boolean success = com.example.data.CloudMigrationService.migrateAllData();
                javafx.application.Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    syncBtn.setDisable(false);
                    if (success) {
                        statusLbl.setText("Status: Done! Data Transferred.");
                        statusLbl.setStyle("-fx-text-fill: #00b894; -fx-font-weight: bold;");
                        new Alert(Alert.AlertType.INFORMATION, "Data has been successfully transferred to Supabase Cloud!").show();
                    } else {
                        statusLbl.setText("Status: Failed. Check logs.");
                        statusLbl.setStyle("-fx-text-fill: #d63031; -fx-font-weight: bold;");
                        new Alert(Alert.AlertType.ERROR, "Data transfer failed. Check the console for details.").show();
                    }
                    logList.getItems().clear();
                    logList.getItems().addAll(DatabaseHandler.getCloudSyncLogs());
                });
            }).start();
        });

        syncSchemaBtn.setOnAction(e -> {
            syncSchemaBtn.setDisable(true);
            progressBar.setVisible(true);
            progressBar.setProgress(-1);
            statusLbl.setText("Status: Syncing schema... Please wait.");
            
            new Thread(() -> {
                boolean success = com.example.data.CloudMigrationService.syncSchemaDynamically();
                javafx.application.Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    syncSchemaBtn.setDisable(false);
                    if (success) {
                        statusLbl.setText("Status: Done! Schema Synchronized.");
                        statusLbl.setStyle("-fx-text-fill: #00b894; -fx-font-weight: bold;");
                        new Alert(Alert.AlertType.INFORMATION, "Schema structure synced successfully!").show();
                    } else {
                        statusLbl.setText("Status: Schema Sync Failed.");
                    }
                    logList.getItems().clear();
                    logList.getItems().addAll(DatabaseHandler.getCloudSyncLogs());
                });
            }).start();
        });

        box.getChildren().addAll(header, desc, licenseBox, new Separator(), connBox, new Separator(), actionBox, progressBar, statusLbl, new Label("Cloud Sync History:"), logList);

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        
        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }
}
