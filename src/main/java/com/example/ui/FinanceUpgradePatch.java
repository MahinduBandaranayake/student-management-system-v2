package com.example.ui;

import javafx.scene.Parent;

public class FinanceUpgradePatch implements ExtensionModule {
    @Override public String getMenuLabel() { return "Finance Upgrade"; }
    @Override public String getTooltip() { return "Enhanced Financials"; }
    @Override public String getInternalName() { return "FinanceUpgrade"; }
    @Override public String getTargetFeature() { return "Finance"; }
    @Override public Parent getView() { return new FinanceUpgradeView().getView(); }
}
