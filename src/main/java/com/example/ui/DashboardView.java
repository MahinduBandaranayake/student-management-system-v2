package com.example.ui;

import com.example.data.DatabaseHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Supplier;

public class DashboardView {
    private static DashboardView instance;
    public static DashboardView getInstance() { return instance; }
    private final Stage stage;
    private final BorderPane root;
    private Label brandLabel;
    
    private Button notificationBtn;
    private boolean hasUnreadNotifications = false;

    private final Map<String, Parent> viewCache = new HashMap<>();
    private final Stack<String> backStack = new Stack<>();
    private final Stack<String> forwardStack = new Stack<>();
    private String currentView = null;
    private VBox historyPanel;
    private TreeView<String> historyTree;
    private TreeItem<String> historyRoot;
    private boolean historyVisible = false;

    public DashboardView(Stage stage) {
        instance = this;
        this.stage = stage;
        this.root = new BorderPane();
        initialize();
    }

    private void initialize() {
        applyTheme();

        String navPos = DatabaseHandler.getSetting("navbar_pos");
        if (navPos == null || navPos.isEmpty()) {
            navPos = "Sidebar"; // Default
        }

        if ("Top Bar".equals(navPos)) {
            root.setLeft(null);
            VBox topContainer = new VBox();
            topContainer.getStyleClass().add("top-nav-container");

            HBox mainBar = createTopBar(true);

            HBox navLinks = new HBox(15);
            navLinks.setAlignment(Pos.CENTER);
            navLinks.setPadding(new Insets(10, 25, 15, 25));
            navLinks.getStyleClass().add("top-nav-links");
            navLinks.setStyle("-fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 0 0 1 0;");

            for (Button b : createNavButtons(false)) {
                b.getStyleClass().add("top-nav-button");
                navLinks.getChildren().add(b);
            }

            topContainer.getChildren().addAll(mainBar, navLinks);
            root.setTop(topContainer);
        } else {
            root.setTop(createTopBar(false));
            root.setLeft(createSidebar());
        }

        setupHistoryPanel();
        navigateTo("Dashboard", this::loadDashboardContent);
    }

    private void setupHistoryPanel() {
        historyRoot = new TreeItem<>("Navigation Flow");
        historyRoot.setExpanded(true);
        historyTree = new TreeView<>(historyRoot);
        historyTree.setShowRoot(true);
        historyTree.getStyleClass().add("history-tree");

        historyTree.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null && !val.getValue().equals(currentView) && !val.getValue().equals("Navigation Flow")) {
                String viewName = val.getValue();
                if (viewCache.containsKey(viewName)) {
                    currentView = viewName;
                    root.setCenter(viewCache.get(viewName));
                }
            }
        });

        Label title = new Label("History Hierarchy");
        title.setStyle("-fx-font-weight: bold; -fx-padding: 10;");

        historyPanel = new VBox(5, title, historyTree);
        historyPanel.setPrefWidth(220);
        historyPanel.setStyle("-fx-background-color: #ffffff; -fx-border-color: #f1f2f6; -fx-border-width: 0 0 0 1;");
        VBox.setVgrow(historyTree, Priority.ALWAYS);
    }

    private void toggleHistory() {
        historyVisible = !historyVisible;
        root.setRight(historyVisible ? historyPanel : null);
    }

    private void applyTheme() {
        root.getStyleClass().remove("dark-theme");
        // Remove hardcoded white, let CSS handle .root
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(250);

        // Brand/Logo removed from Sidebar as per request

        Button btnToggle = createSidebarButton("🔁 Switch to Top Nav");
        btnToggle.setOnAction(e -> {
            DatabaseHandler.saveSetting("navbar_pos", "Top Bar");
            initialize();
        });
        sidebar.getChildren().add(btnToggle);
        sidebar.getChildren().add(new Separator());

        sidebar.getChildren().addAll(createNavButtons(true));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Button btnLogout = createSidebarButton("Logout / Exit");
        btnLogout.setStyle("-fx-text-fill: #f38ba8;");
        btnLogout.setOnAction(e -> handleLogout());
        sidebar.getChildren().add(btnLogout);

        return sidebar;
    }

    private List<Button> createNavButtons(boolean isSidebar) {
        String[][] items = {
                { "🏠 Dashboard", "Home Dashboard", "Dashboard" },
                { "👥 Students", "Student Directory", "Students" },
                { "📚 Programs", "Academic Programs", "Programs" },
                { "📖 Courses", "Course Modules", "Courses" },
                { "👨‍🏫 Faculty", "Staff & Faculty", "Lecturers" },
                { "📅 Events", "Campus Events", "Events" },
                { "🏆 Grades", "Academic Scales", "Grades" },
                { "💰 Finance", "Financial Records", "Finance" },
                { "📊 Alumni", "Performance Reports", "Reports" },
                { "📜 Archive", "Academic History", "History" },
                { "⚙️ Settings", "System Settings", "Settings" }
        };

        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            String label = items[i][0];
            String tooltip = items[i][1];
            String view = items[i][2];

            Button b;
            if (isSidebar) {
                b = createSidebarButton(label);
            } else {
                String icon = label.split(" ")[0];
                b = createIconButton(icon, tooltip);
            }

            b.setOnAction(e -> {
                Supplier<Parent> supplier = getSupplierForView(view);
                if (supplier != null)
                    navigateTo(view, supplier);
            });
            buttons.add(b);
        }

        // --- LOAD EXTERNAL EXTENSIONS (New Features Only) ---
        for (ExtensionModule module : ExtensionLoader.getLoadedModules()) {
            // Only add brand new buttons if it's NOT an upgrade/override
            if (module.getTargetFeature() == null) {
                Button b;
                if (isSidebar) {
                    b = createSidebarButton(module.getMenuLabel());
                } else {
                    String icon = module.getMenuLabel().split(" ")[0];
                    b = createIconButton(icon, module.getTooltip());
                }
                b.setOnAction(e -> navigateTo(module.getInternalName(), module::getView));
                buttons.add(b);
            }
        }

        return buttons;
    }

    private Supplier<Parent> getSupplierForView(String name) {
        // --- CHECK IF AN EXTENSION OVERRIDES THIS FEATURE ---
        for (ExtensionModule module : ExtensionLoader.getLoadedModules()) {
            String target = module.getTargetFeature();
            if (target != null) {
                // Check exact match OR fallback match for Lecturers/Faculty
                boolean isMatch = name.equalsIgnoreCase(target);

                // Fallback: If we are looking for Lecturers, match against "Faculty" as well
                if (!isMatch && name.equalsIgnoreCase("Lecturers") && target.equalsIgnoreCase("Faculty")) {
                    isMatch = true;
                }

                if (isMatch) {
                    String log = "Routing feature '" + name + "' to Functional Patch [" + module.getInternalName()
                            + "]";
                    System.out.println(log);
                    ExtensionLoader.getLoadLogs().add("🔀 " + log);
                    return module::getView;
                }
            }
        }

        // --- CHECK IF IT'S A STANDALONE EXTENSION VIEW ---
        for (ExtensionModule module : ExtensionLoader.getLoadedModules()) {
            if (name.equals(module.getInternalName())) {
                return module::getView;
            }
        }

        switch (name) {
            case "Dashboard":
                return this::loadDashboardContent;
            case "Students":
                return () -> new StudentsView().getView();
            case "Programs":
                return () -> new ProgramsView().getView();
            case "Courses":
                return () -> new CoursesView().getView();
            case "Lecturers":
                return () -> new LecturersView().getView();
            case "Events":
                return () -> new EventsView().getView();
            case "Grades":
                return () -> new GradesView().getView();
            case "Finance":
                return () -> new FinanceView().getView();
            case "Reports":
                return () -> new AlumniView().getView();
            case "History":
                return () -> new AcademicHistoryView().getView();
            case "Settings":
                return () -> new SettingsView().getView();
            default:
                return null;
        }
    }

    private Button createIconButton(String icon, String tooltip) {
        Button b = new Button(icon);
        b.setTooltip(new Tooltip(tooltip));
        b.getStyleClass().add("nav-icon-only");
        return b;
    }

    public void navigateTo(String viewName, Supplier<Parent> viewSupplier) {
        navigateTo(null, viewName, viewSupplier);
    }

    public void navigateTo(String parentName, String viewName, Supplier<Parent> viewSupplier) {
        if (viewName.equals(currentView))
            return;

        if (currentView != null) {
            backStack.push(currentView);
            forwardStack.clear();
        }

        currentView = viewName;

        // --- ONE-TIME EXTENSION NOTICE ---
        checkForExtensionNotice(viewName);

        Parent view = viewCache.computeIfAbsent(viewName, k -> viewSupplier.get());
        root.setCenter(view);

        if (parentName != null) {
            addToSubHistory(parentName, viewName);
        } else {
            addToHistory(viewName);
        }
    }

    private void checkForExtensionNotice(String viewName) {
        for (ExtensionModule module : ExtensionLoader.getLoadedModules()) {
            // Check if this view matches the extension name OR if the extension is
            // OVERRIDING this view
            boolean isMatch = viewName.equalsIgnoreCase(module.getInternalName()) ||
                    viewName.equalsIgnoreCase(module.getTargetFeature());

            if (isMatch) {
                String settingKey = "ext_notified_" + module.getInternalName();
                if (!"true".equals(DatabaseHandler.getSetting(settingKey))) {
                    boolean isUpgrade = module.getTargetFeature() != null;

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle(isUpgrade ? "🚀 Feature Upgraded" : "🧩 New Feature Added");
                    alert.setHeaderText(isUpgrade ? "Existing Module Enhanced" : "New Functionality Integrated");

                    String msg = isUpgrade
                            ? "The '" + module.getTargetFeature()
                                    + "' module has been successfully upgraded with a new functional patch."
                            : "A new feature '" + module.getMenuLabel()
                                    + "' has been added to your system via an external patch.";

                    alert.setContentText(msg + "\n\nThis is a one-time notice to confirm the patch is active.");
                    alert.showAndWait();

                    DatabaseHandler.saveSetting(settingKey, "true");
                }
                break;
            }
        }
    }

    public static void showView(String parent, String name, Supplier<Parent> supplier) {
        if (instance != null)
            instance.navigateTo(parent, name, supplier);
    }

    public void clearCache() {
        viewCache.clear();
    }

    public static void clearViewCache() {
        if (instance != null) {
            instance.clearCache();
        }
    }

    public void reloadCurrentView() {
        if (currentView == null)
            return;
        String name = currentView;
        Supplier<Parent> supplier = getSupplierForView(name);
        if (supplier != null) {
            clearViewCache();
            Parent view = supplier.get();
            viewCache.put(name, view);
            root.setCenter(view);
        }
    }

    private void addToHistory(String name) {
        if (historyRoot == null)
            return;

        boolean exists = false;
        for (TreeItem<String> item : historyRoot.getChildren()) {
            if (item.getValue().equals(name)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            TreeItem<String> newItem = new TreeItem<>(name);
            historyRoot.getChildren().add(newItem);
            historyTree.getSelectionModel().select(newItem);
        }
    }

    private void addToSubHistory(String parentName, String childName) {
        if (historyRoot == null)
            return;

        TreeItem<String> parentItem = null;
        for (TreeItem<String> item : historyRoot.getChildren()) {
            if (item.getValue().equals(parentName)) {
                parentItem = item;
                break;
            }
        }

        if (parentItem == null) {
            parentItem = new TreeItem<>(parentName);
            historyRoot.getChildren().add(parentItem);
        }

        boolean childExists = false;
        for (TreeItem<String> child : parentItem.getChildren()) {
            if (child.getValue().equals(childName)) {
                childExists = true;
                historyTree.getSelectionModel().select(child);
                break;
            }
        }

        if (!childExists) {
            TreeItem<String> newChild = new TreeItem<>(childName);
            parentItem.getChildren().add(newChild);
            parentItem.setExpanded(true);
            historyTree.getSelectionModel().select(newChild);
        }
    }

    private void goBack() {
        if (!backStack.isEmpty()) {
            forwardStack.push(currentView);
            currentView = backStack.pop();
            root.setCenter(viewCache.get(currentView));
        }
    }

    private void goForward() {
        if (!forwardStack.isEmpty()) {
            backStack.push(currentView);
            currentView = forwardStack.pop();
            root.setCenter(viewCache.get(currentView));
        }
    }

    private HBox createTopBar(boolean isTopNavMode) {
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(10, 25, 10, 25));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("top-nav-container");

        brandLabel = new Label("Student Management System");
        brandLabel.getStyleClass().add("top-nav-branding-label");
        brandLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800;");

        HBox brandBox = new HBox(10);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.getChildren().add(brandLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_RIGHT);

        Button btnBack = createIconBtn("⬅", "Go Back");
        btnBack.setOnAction(e -> goBack());
        Button btnForward = createIconBtn("➡", "Go Forward");
        btnForward.setOnAction(e -> goForward());

        if (!isTopNavMode) {
            Button btnToggleNav = createIconBtn("🔁", "Switch to Top Nav");
            btnToggleNav.setOnAction(e -> {
                DatabaseHandler.saveSetting("navbar_pos", "Top Bar");
                initialize();
            });
            controls.getChildren().add(btnToggleNav);
        } else {
            Button btnToggleNav = createIconBtn("🔁", "Switch to Sidebar");
            btnToggleNav.setOnAction(e -> {
                DatabaseHandler.saveSetting("navbar_pos", "Sidebar");
                initialize();
            });
            controls.getChildren().add(btnToggleNav);
        }

        Button btnHistoryNav = createIconBtn("📜", "Navigation History");
        btnHistoryNav.setOnAction(e -> toggleHistory());

        Button btnRefresh = createIconBtn("🔄", "Refresh View");
        btnRefresh.setOnAction(e -> reloadCurrentView());

        // Notification Bell
        notificationBtn = createIconBtn("🔔", "Notifications");
        notificationBtn.setOnAction(e -> {
            hasUnreadNotifications = false;
            updateNotificationIcon();
            new Alert(Alert.AlertType.INFORMATION, "No new notifications.").show();
        });

        Button btnLogout = createIconBtn("🚪", "Logout");
        btnLogout.setOnAction(e -> handleLogout());

        controls.getChildren().addAll(btnRefresh, btnBack, btnForward, btnHistoryNav,
                notificationBtn,
                new Separator(Orientation.VERTICAL),
                btnLogout);
        topBar.getChildren().addAll(brandBox, spacer, controls);
        return topBar;
    }

    private void updateNotificationIcon() {
        if (notificationBtn != null) {
            if (hasUnreadNotifications) {
                notificationBtn.setText("🔔 🟢");
                notificationBtn.setStyle("-fx-text-fill: #2ecc71;"); // Green dot indicator
            } else {
                notificationBtn.setText("🔔");
                notificationBtn.setStyle(""); 
            }
        }
    }

    public void notifyUpdate(String message) {
        javafx.application.Platform.runLater(() -> {
            hasUnreadNotifications = true;
            updateNotificationIcon();
            
            // Show a quick tooltip or toast if desired, but for now we update the icon
            notificationBtn.setOnAction(e -> {
                hasUnreadNotifications = false;
                updateNotificationIcon();
                new Alert(Alert.AlertType.INFORMATION, "System Update:\n" + message).show();
            });
        });
    }

    public void refreshBranding() {
        // Branding is now fixed to 'Student Management System' as per request.
        if (brandLabel != null) {
            brandLabel.setText("Student Management System");
        }
    }

    private Parent loadDashboardContent() {
        VBox content = new VBox(30);
        content.setPadding(new Insets(35));
        content.setUserData("Dashboard");

        Label welcome = new Label("Operational Overview");
        welcome.getStyleClass().add("header-label");
        welcome.setStyle("-fx-font-size: 28px;");

        // Use a grid with a responsive structure (TilePane or FlowPane can also work)
        TilePane tiles = new TilePane();
        tiles.setHgap(20);
        tiles.setVgap(20);
        tiles.setPrefColumns(3); // 3 cards per row

        tiles.getChildren().addAll(
                createStatCard("Total Programs", "4 Active"),
                createStatCard("Enrolled Students", "342 Active"),
                createStatCard("Faculty Members", "12 Active"));

        HBox recentActivity = new HBox(20);
        recentActivity.getChildren().addAll(
                createActivityList("Recent Enrollments", "Sarah Jenkins - BSc Computer Science\n" +
                        "David Miller - BA Business Admin\n" +
                        "Emily Chen - BSc Data Science"),
                createActivityList("Upcoming Deadlines", "Term Fee Payments - Nov 15\n" +
                        "Exam Registration - Nov 20\n" +
                        "Project Submissions - Dec 01"));
        HBox.setHgrow(recentActivity.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(recentActivity.getChildren().get(1), Priority.ALWAYS);

        content.getChildren().addAll(welcome, tiles, recentActivity);
        return content;
    }

    private VBox createStatCard(String title, String value) {
        VBox card = new VBox(10);
        card.getStyleClass().add("sub-section");
        card.setStyle("-fx-background-color: #f8fafc;");
        card.setPrefWidth(250);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #0d3b4c;");

        card.getChildren().addAll(titleLbl, valLbl);
        return card;
    }

    private VBox createActivityList(String title, String contentText) {
        VBox list = new VBox(15);
        list.getStyleClass().add("sub-section");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("section-header");
        titleLbl.setStyle("-fx-font-size: 18px;");

        Label content = new Label(contentText);
        content.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155; -fx-line-spacing: 0.5em;");

        list.getChildren().addAll(titleLbl, content);
        return list;
    }

    private Button createSidebarButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.getStyleClass().add("sidebar-button");
        return b;
    }

    public Parent getView() {
        return root;
    }

    private void handleLogout() {
        LoginView login = new LoginView(stage);
        Scene scene = new Scene(login.getView(), 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
    }
}