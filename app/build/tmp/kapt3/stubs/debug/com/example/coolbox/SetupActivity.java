package com.example.coolbox;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000b\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0004\n\u0002\u0010$\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010%\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J&\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00060\nH\u0002J\u0010\u0010\u000b\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\rH\u0002Jl\u0010\u000e\u001a\u00020\u00042\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00060\u00102\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00060\u00102\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00060\u00102\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00060\u00102\u0012\u0010\u0014\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00060\u00152\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u0006H\u0002J\u0012\u0010\u001b\u001a\u00020\u00042\b\u0010\u001c\u001a\u0004\u0018\u00010\u001dH\u0014J:\u0010\u001e\u001a\u00020\u00042\u0006\u0010\u001f\u001a\u00020\u00062\u0006\u0010 \u001a\u00020\u00062\u0006\u0010!\u001a\u00020\u00192\u0018\u0010\"\u001a\u0014\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00060\u0010\u0012\u0004\u0012\u00020\u00040#H\u0002Jf\u0010$\u001a\u00020\u00042\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00060\u00102\u0006\u0010%\u001a\u00020\r2\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00060\n2\u0012\u0010\u0014\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00060&2\f\u0010\'\u001a\b\u0012\u0004\u0012\u00020\u00060\u00102\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u0006H\u0002Jb\u0010(\u001a\u00020\u00042\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00060\u00102\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00060\u00102\u0012\u0010\u0014\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00060\u00152\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u00062\u0010\b\u0002\u0010)\u001a\n\u0012\u0004\u0012\u00020\u0006\u0018\u00010\u0010H\u0002\u00a8\u0006*"}, d2 = {"Lcom/example/coolbox/SetupActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "addFridgeRow", "", "name", "", "container", "Landroid/widget/LinearLayout;", "names", "", "getChineseOrdinal", "i", "", "handleCategoryMigrationAndFinish", "oldCategories", "", "newCategories", "allFinalLocations", "fridgeBaseNames", "allCapabilities", "", "viewModel", "Lcom/example/coolbox/ui/MainViewModel;", "syncEnabled", "", "syncUrl", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "promptForFreezerLayers", "baseName", "compartmentName", "isDeepFreezer", "onComplete", "Lkotlin/Function1;", "requestCapabilities", "index", "", "categories", "requestCategorySetup", "categoriesToStartWith", "app_debug"})
public final class SetupActivity extends androidx.appcompat.app.AppCompatActivity {
    
    public SetupActivity() {
        super();
    }
    
    @java.lang.Override
    protected void onCreate(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    private final void addFridgeRow(java.lang.String name, android.widget.LinearLayout container, java.util.List<java.lang.String> names) {
    }
    
    private final java.lang.String getChineseOrdinal(int i) {
        return null;
    }
    
    private final void requestCapabilities(java.util.List<java.lang.String> fridgeBaseNames, int index, java.util.List<java.lang.String> allFinalLocations, java.util.Map<java.lang.String, java.lang.String> allCapabilities, java.util.List<java.lang.String> categories, com.example.coolbox.ui.MainViewModel viewModel, boolean syncEnabled, java.lang.String syncUrl) {
    }
    
    private final void promptForFreezerLayers(java.lang.String baseName, java.lang.String compartmentName, boolean isDeepFreezer, kotlin.jvm.functions.Function1<? super java.util.List<java.lang.String>, kotlin.Unit> onComplete) {
    }
    
    private final void requestCategorySetup(java.util.List<java.lang.String> allFinalLocations, java.util.List<java.lang.String> fridgeBaseNames, java.util.Map<java.lang.String, java.lang.String> allCapabilities, com.example.coolbox.ui.MainViewModel viewModel, boolean syncEnabled, java.lang.String syncUrl, java.util.List<java.lang.String> categoriesToStartWith) {
    }
    
    private final void handleCategoryMigrationAndFinish(java.util.List<java.lang.String> oldCategories, java.util.List<java.lang.String> newCategories, java.util.List<java.lang.String> allFinalLocations, java.util.List<java.lang.String> fridgeBaseNames, java.util.Map<java.lang.String, java.lang.String> allCapabilities, com.example.coolbox.ui.MainViewModel viewModel, boolean syncEnabled, java.lang.String syncUrl) {
    }
}