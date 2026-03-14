// Version: V2.9.0-RC2 (Refactor)
package com.example.coolbox.ui

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.example.coolbox.MainActivity
import com.example.coolbox.data.FoodEntity
import com.example.coolbox.legacy.R

class FoodActionHelper(private val context: Context, private val viewModel: MainViewModel) {

    fun showIconPickerDialog(entity: FoodEntity) {
        val iconList = listOf(
            "ic_food_beef", "ic_food_pork", "ic_food_chicken", "ic_food_egg",
            "ic_food_fish", "ic_food_shrimp", "ic_food_crab", "ic_food_ribs",
            "ic_food_apple", "ic_food_tomato", "ic_food_watermelon", "ic_food_grapes",
            "ic_food_strawberry", "ic_food_onion", "ic_food_lettuce", "ic_food_milk",
            "ic_food_yogurt", "ic_food_juice", "ic_food_cola", "ic_food_beer",
            "ic_food_icecream", "ic_food_durian", "ic_food_blueberries", "ic_food_broccoli", "ic_food_butter",
            "ic_food_cheese", "ic_food_dumpling", "ic_food_green_apple", "ic_food_pepper",
            "ic_food_lemon", "ic_food_mango", "ic_food_pear", "ic_food_tangerine",
            "ic_food_shellfish", "ic_food_mussel",
            "cat_cooked", "cat_meat", "cat_veg", "cat_drink", "cat_snack"
        )
        
        val grid = android.widget.GridLayout(context).apply {
            columnCount = 5
            rowCount = (iconList.size + 4) / 5
            setPadding(16, 16, 16, 16)
        }
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("更换图标")
            .setView(grid)
            .create()

        iconList.forEach { iconName ->
            val img = ImageView(context).apply {
                val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                if (resId != 0) setImageResource(resId)
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = (64 * context.resources.displayMetrics.density).toInt()
                    height = (64 * context.resources.displayMetrics.density).toInt()
                    setMargins(4, 4, 4, 4)
                }
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setOnClickListener {
                    viewModel.updateFoodIcon(entity, iconName)
                    dialog.dismiss()
                }
            }
            grid.addView(img)
        }
        dialog.show()
    }

    fun showItemActionDialog(entity: FoodEntity) {
        val options = arrayOf("编辑信息", "仅拿走 1 份", "拿走多份...", "转移到其他冰箱")
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(entity.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> (context as MainActivity).editHelper.showAddOrEditFoodDialog(entity)
                    1 -> viewModel.takePortion(entity)
                    2 -> showTakePortionDialog(entity)
                    3 -> showTransferDialog(entity)
                }
            }
            .show()
    }

    fun showTakePortionDialog(entity: FoodEntity) {
        val input = EditText(context)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "最多可拿走 ${entity.portions} 份"
        
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("取走多少份？")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val p = input.text.toString().toIntOrNull() ?: 0
                if (p >= entity.portions) {
                    viewModel.deleteFood(entity)
                } else if (p > 0) {
                    viewModel.takePortions(entity, p)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    fun showTransferDialog(entity: FoodEntity) {
        val fridges = (viewModel.fridges.value ?: emptyList()).filter { it != entity.fridgeName }
        if (fridges.isEmpty()) {
            android.widget.Toast.makeText(context, "没有其他冰箱可供转移", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        val fridgArray = fridges.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("转移到...")
            .setItems(fridgArray) { _, fridgeIdx ->
                val targetFridge = fridgArray[fridgeIdx]
                
                // If and only if portions > 1, ask how many to transfer
                if (entity.portions > 1) {
                    val transferOptions = arrayOf("全取 (${entity.portions}份)", "转移 1 份", "自定义份数")
                    androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("转移份数")
                        .setItems(transferOptions) { _, optionIdx ->
                            when (optionIdx) {
                                0 -> viewModel.transferItem(entity, targetFridge, entity.portions)
                                1 -> viewModel.transferItem(entity, targetFridge, 1)
                                2 -> {
                                    val input = EditText(context)
                                    input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                                    input.hint = "最大 ${entity.portions}"
                                    androidx.appcompat.app.AlertDialog.Builder(context)
                                        .setTitle("输入转移份数")
                                        .setView(input)
                                        .setPositiveButton("转移") { _, _ ->
                                            val p = input.text.toString().toIntOrNull() ?: 0
                                            if (p > 0 && p <= entity.portions) {
                                                viewModel.transferItem(entity, targetFridge, p)
                                            }
                                        }
                                        .setNegativeButton("取消", null)
                                        .show()
                                }
                            }
                        }
                        .show()
                } else {
                    viewModel.transferItem(entity, targetFridge, 1)
                }
            }
            .show()
    }
}
// Version: V2.9.0-RC2 (Refactor)
