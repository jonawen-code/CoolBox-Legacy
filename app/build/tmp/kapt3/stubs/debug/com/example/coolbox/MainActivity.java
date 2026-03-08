package com.example.coolbox;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001:\u0001\u001fB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0007\u001a\u00020\b2\b\u0010\t\u001a\u0004\u0018\u00010\nH\u0014J\u0014\u0010\u000b\u001a\u00020\b2\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\rH\u0002J\u0016\u0010\u000e\u001a\u00020\b2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00110\u0010H\u0002J\u000e\u0010\u0012\u001a\u00020\b2\u0006\u0010\u0013\u001a\u00020\u0011J\u0010\u0010\u0014\u001a\u00020\b2\u0006\u0010\u0013\u001a\u00020\u0011H\u0002J\b\u0010\u0015\u001a\u00020\bH\u0002J\u0010\u0010\u0016\u001a\u00020\b2\u0006\u0010\u0013\u001a\u00020\u0011H\u0002J\b\u0010\u0017\u001a\u00020\bH\u0002J\u0010\u0010\u0018\u001a\u00020\b2\u0006\u0010\u0013\u001a\u00020\u0011H\u0002J\"\u0010\u0019\u001a\u00020\b2\u000e\u0010\u000f\u001a\n\u0012\u0004\u0012\u00020\u0011\u0018\u00010\u00102\b\u0010\u001a\u001a\u0004\u0018\u00010\rH\u0002J\u001a\u0010\u001b\u001a\u00020\b2\u0006\u0010\u001c\u001a\u00020\u001d2\b\u0010\u001e\u001a\u0004\u0018\u00010\rH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006 "}, d2 = {"Lcom/example/coolbox/MainActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "adapter", "Lcom/example/coolbox/MainActivity$FoodAdapter;", "viewModel", "Lcom/example/coolbox/ui/MainViewModel;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "showAddFoodDialog", "categoryToSelect", "", "showExpiryStatusReport", "allFood", "", "Lcom/example/coolbox/data/FoodEntity;", "showIconPickerDialog", "entity", "showItemActionDialog", "showSettingsDialog", "showTakePortionDialog", "showTimeMachineDialog", "showTransferDialog", "updateList", "currentFridge", "updateTabHighlights", "container", "Landroid/widget/LinearLayout;", "activeFridge", "FoodAdapter", "app_debug"})
public final class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.example.coolbox.ui.MainViewModel viewModel;
    private com.example.coolbox.MainActivity.FoodAdapter adapter;
    
    public MainActivity() {
        super();
    }
    
    @java.lang.Override
    protected void onCreate(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    private final void showAddFoodDialog(java.lang.String categoryToSelect) {
    }
    
    public final void showIconPickerDialog(@org.jetbrains.annotations.NotNull
    com.example.coolbox.data.FoodEntity entity) {
    }
    
    private final void showItemActionDialog(com.example.coolbox.data.FoodEntity entity) {
    }
    
    private final void showTakePortionDialog(com.example.coolbox.data.FoodEntity entity) {
    }
    
    private final void showTransferDialog(com.example.coolbox.data.FoodEntity entity) {
    }
    
    private final void showExpiryStatusReport(java.util.List<com.example.coolbox.data.FoodEntity> allFood) {
    }
    
    private final void updateList(java.util.List<com.example.coolbox.data.FoodEntity> allFood, java.lang.String currentFridge) {
    }
    
    private final void showSettingsDialog() {
    }
    
    private final void showTimeMachineDialog() {
    }
    
    private final void updateTabHighlights(android.widget.LinearLayout container, java.lang.String activeFridge) {
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000X\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010$\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\t\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001&B\u00b5\u0001\u0012\u000e\u0010\u0003\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00050\u0004\u0012\u001e\u0010\u0006\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\u0005\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\b0\u00070\u0004\u0012\u0012\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\f0\n\u0012\u0012\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\f0\n\u0012\u0012\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\f0\n\u0012\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\f0\n\u0012\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u0004\u0012\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00130\u0004\u0012\u0012\u0010\u0014\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\b0\u0004\u00a2\u0006\u0002\u0010\u0015J\u0018\u0010\u0017\u001a\u00020\f2\u0006\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u000bH\u0002J\b\u0010\u001b\u001a\u00020\u001cH\u0016J\u0018\u0010\u001d\u001a\u00020\f2\u0006\u0010\u001e\u001a\u00020\u00022\u0006\u0010\u001f\u001a\u00020\u001cH\u0016J\u0018\u0010 \u001a\u00020\u00022\u0006\u0010!\u001a\u00020\"2\u0006\u0010#\u001a\u00020\u001cH\u0016J\u0014\u0010$\u001a\u00020\f2\f\u0010%\u001a\b\u0012\u0004\u0012\u00020\u000b0\bR\u0016\u0010\u0003\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R&\u0010\u0006\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\u0005\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\b0\u00070\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0014\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\b0\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00130\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u000b0\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\'"}, d2 = {"Lcom/example/coolbox/MainActivity$FoodAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/example/coolbox/MainActivity$FoodAdapter$ViewHolder;", "currentFridge", "Lkotlin/Function0;", "", "getCatalogItems", "", "", "onAction", "Lkotlin/Function1;", "Lcom/example/coolbox/data/FoodEntity;", "", "onDelete", "onTakeOne", "onTakeAll", "getFontScale", "", "getNowMs", "", "getFridgeBases", "(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V", "items", "applySafeIcon", "imageView", "Landroid/widget/ImageView;", "item", "getItemCount", "", "onBindViewHolder", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "submitList", "newItems", "ViewHolder", "app_debug"})
    public static final class FoodAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.example.coolbox.MainActivity.FoodAdapter.ViewHolder> {
        private final kotlin.jvm.functions.Function0<java.lang.String> currentFridge = null;
        private final kotlin.jvm.functions.Function0<java.util.Map<java.lang.String, java.util.List<java.lang.String>>> getCatalogItems = null;
        private final kotlin.jvm.functions.Function1<com.example.coolbox.data.FoodEntity, kotlin.Unit> onAction = null;
        private final kotlin.jvm.functions.Function1<com.example.coolbox.data.FoodEntity, kotlin.Unit> onDelete = null;
        private final kotlin.jvm.functions.Function1<com.example.coolbox.data.FoodEntity, kotlin.Unit> onTakeOne = null;
        private final kotlin.jvm.functions.Function1<com.example.coolbox.data.FoodEntity, kotlin.Unit> onTakeAll = null;
        private final kotlin.jvm.functions.Function0<java.lang.Float> getFontScale = null;
        private final kotlin.jvm.functions.Function0<java.lang.Long> getNowMs = null;
        private final kotlin.jvm.functions.Function0<java.util.List<java.lang.String>> getFridgeBases = null;
        private java.util.List<com.example.coolbox.data.FoodEntity> items;
        
        public FoodAdapter(@org.jetbrains.annotations.NotNull
        kotlin.jvm.functions.Function0<java.lang.String> currentFridge, @org.jetbrains.annotations.NotNull
        kotlin.jvm.functions.Function0<? extends java.util.Map<java.lang.String, ? extends java.util.List<java.lang.String>>> getCatalogItems, @org.jetbrains.annotations.NotNull
        kotlin.jvm.functions.Function1<? super com.example.coolbox.data.FoodEntity, kotlin.Unit> onAction, @org.jetbrains.annotations.NotNull
        kotlin.jvm.functions.Function1<? super com.example.coolbox.data.FoodEntity, kotlin.Unit> onDelete, @org.jetbrains.annotations.NotNull
        kotlin.jvm.functions.Function1<? super com.example.coolbox.data.FoodEntity, kotlin.Unit> onTakeOne, @org.jetbrains.annotations.NotNull
        kotlin.jvm.functions.Function1<? super com.example.coolbox.data.FoodEntity, kotlin.Unit> onTakeAll, @org.jetbrains.annotations.NotNull
        kotlin.jvm.functions.Function0<java.lang.Float> getFontScale, @org.jetbrains.annotations.NotNull
        kotlin.jvm.functions.Function0<java.lang.Long> getNowMs, @org.jetbrains.annotations.NotNull
        kotlin.jvm.functions.Function0<? extends java.util.List<java.lang.String>> getFridgeBases) {
            super();
        }
        
        public final void submitList(@org.jetbrains.annotations.NotNull
        java.util.List<com.example.coolbox.data.FoodEntity> newItems) {
        }
        
        @org.jetbrains.annotations.NotNull
        @java.lang.Override
        public com.example.coolbox.MainActivity.FoodAdapter.ViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull
        android.view.ViewGroup parent, int viewType) {
            return null;
        }
        
        @java.lang.Override
        public void onBindViewHolder(@org.jetbrains.annotations.NotNull
        com.example.coolbox.MainActivity.FoodAdapter.ViewHolder holder, int position) {
        }
        
        private final void applySafeIcon(android.widget.ImageView imageView, com.example.coolbox.data.FoodEntity item) {
        }
        
        @java.lang.Override
        public int getItemCount() {
            return 0;
        }
        
        @kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u000f\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\t\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\bR\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u000f\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0013\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u000eR\u0011\u0010\u0015\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u000eR\u0011\u0010\u0017\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u000eR\u0011\u0010\u0019\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u000eR\u0011\u0010\u001b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u000eR\u0011\u0010\u001d\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u000e\u00a8\u0006\u001f"}, d2 = {"Lcom/example/coolbox/MainActivity$FoodAdapter$ViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "view", "Landroid/view/View;", "(Landroid/view/View;)V", "btnTakeAll", "Landroid/widget/Button;", "getBtnTakeAll", "()Landroid/widget/Button;", "btnTakeOne", "getBtnTakeOne", "detail", "Landroid/widget/TextView;", "getDetail", "()Landroid/widget/TextView;", "icon", "Landroid/widget/ImageView;", "getIcon", "()Landroid/widget/ImageView;", "location", "getLocation", "name", "getName", "portions", "getPortions", "quantity", "getQuantity", "remark", "getRemark", "status", "getStatus", "app_debug"})
        public static final class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            @org.jetbrains.annotations.NotNull
            private final android.widget.ImageView icon = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView name = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView quantity = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView remark = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView portions = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView detail = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView location = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView status = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.Button btnTakeOne = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.Button btnTakeAll = null;
            
            public ViewHolder(@org.jetbrains.annotations.NotNull
            android.view.View view) {
                super(null);
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.ImageView getIcon() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getName() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getQuantity() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getRemark() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getPortions() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getDetail() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getLocation() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getStatus() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.Button getBtnTakeOne() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.Button getBtnTakeAll() {
                return null;
            }
        }
    }
}