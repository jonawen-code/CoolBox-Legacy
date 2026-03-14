// Version: V2.9.0-RC1 (Refactor)
package com.example.coolbox
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.coolbox.data.FoodEntity
import com.example.coolbox.legacy.R
import com.example.coolbox.util.formatQuantity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FoodAdapter(
private val currentFridge: () -> String?,
private val getCatalogItems: () -> Map<String, List<String>>,
private val onAction: (FoodEntity) -> Unit,
private val onDelete: (FoodEntity) -> Unit,
private val onTakeOne: (FoodEntity) -> Unit,
private val onTakeAll: (FoodEntity) -> Unit,
private val getFontScale: () -> Float,
private val getNowMs: () -> Long,
private val getFridgeBases: () -> List<String>
) : RecyclerView.Adapter<FoodAdapter.ViewHolder>() {
private var items = emptyList<FoodEntity>()

fun submitList(newItems: List<FoodEntity>) {
    items = newItems
    notifyDataSetChanged()
}
override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
    return ViewHolder(view)
}
override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val item = items[position]
    val scale = getFontScale()
    val nowMs = getNowMs()
    
    holder.name.textSize = 20f * scale
    holder.detail.textSize = 14f * scale
    holder.quantity.textSize = 16f * scale
    holder.portions.textSize = 14f * scale
    holder.remark.textSize = 14f * scale
    holder.location.textSize = 14f * scale
    // Build 29: Render clean display name, strip ic_food_ / cat_ prefixes
    holder.name.text = (holder.itemView.context as MainActivity).viewModel.getDisplayName(item.name)
    val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dateStr = "至 ：" + df.format(Date(item.expiryDateMs))
    holder.detail.text = dateStr
    holder.quantity.text = "${formatQuantity(item.quantity)} ${item.unit}"
    holder.portions.text = "${item.portions} 份，${formatQuantity(item.weightPerPortion)}${item.unit}/份"
    
    holder.remark.visibility = View.VISIBLE
    if (item.remark.isNotBlank()) {
        holder.remark.text = "注：${item.remark}"
    } else {
        holder.remark.text = " " // Stable empty line for height consistency
    }
    // Build 44: 保持与底层数据及其他对话框完全一致的显示格式，不遮蔽横杠
    val shortLoc = item.fridgeName.replace("室", "").replace("第", "")
    holder.location.text = "在：$shortLoc"
    applySafeIcon(holder.icon, item)
    holder.icon.setOnClickListener {
        (holder.itemView.context as MainActivity).actionHelper.showIconPickerDialog(item)
    }
    
    val container = holder.itemView.findViewById<View>(R.id.itemContainer)
    
    // Near Expiry Logic
    val totalDuration = item.expiryDateMs - item.inputDateMs
    val currentNowMs = getNowMs()
    val nearExpiryThreshold = if (totalDuration > 0) item.expiryDateMs - (totalDuration / 4) else currentNowMs + 3 * 24 * 60 * 60 * 1000L
    if (currentNowMs >= item.expiryDateMs) {
        container.setBackgroundResource(R.drawable.bg_item_expired)
        holder.status.visibility = View.VISIBLE
        holder.status.text = "已过期"
        holder.status.setTextColor(android.graphics.Color.RED)
    } else if (currentNowMs >= nearExpiryThreshold) {
        container.setBackgroundResource(R.drawable.bg_item_near)
        holder.status.visibility = View.VISIBLE
        val daysRemaining = Math.max(0, Math.ceil((item.expiryDateMs - currentNowMs).toDouble() / (24 * 60 * 60 * 1000)).toInt())
        holder.status.text = "临期提醒: 剩余 $daysRemaining 天"
        holder.status.setTextColor(android.graphics.Color.parseColor("#FF9800"))
    } else {
        container.setBackgroundResource(R.drawable.bg_item_normal)
        holder.status.visibility = View.GONE
    }
    // Surface Actions
    if (item.portions > 1) {
        holder.btnTakeOne.visibility = View.VISIBLE
        holder.btnTakeOne.setOnClickListener { onTakeOne(item) }
    } else {
        holder.btnTakeOne.visibility = View.GONE
    }
    
    holder.btnTakeAll.setOnClickListener { onTakeAll(item) }
    holder.itemView.setOnClickListener { onAction(item) }
}
private fun applySafeIcon(imageView: ImageView, item: FoodEntity) {
    val name = item.name
    val iconName = item.icon
    val context = imageView.context
    
    if (name.contains("蛋")) {
        imageView.setImageResource(R.drawable.ic_food_egg)
        return
    }

    if (iconName.isNotEmpty()) {
        val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
        if (resId != 0) {
            imageView.setImageResource(resId)
            return
        }
    }
    val category = getCatalogItems().entries.find { it.value.contains(name) }?.key ?: ""
    val resId = when {
        name.contains("熟食") || category.contains("熟食") -> R.drawable.cat_cooked
        name.contains("肉") || name.contains("鱼") || name.contains("虾") || name.contains("螃蟹") || category.contains("肉") -> R.drawable.cat_meat
        name.contains("菜") || name.contains("果") || name.contains("苹果") || category.contains("蔬菜") -> R.drawable.cat_veg
        name.contains("奶") || name.contains("汁") || category.contains("饮料") -> R.drawable.cat_drink
        else -> R.drawable.cat_snack
    }
    imageView.setImageResource(resId)
}
override fun getItemCount() = items.size
class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val icon: ImageView = view.findViewById(R.id.itemIcon)
    val name: TextView = view.findViewById(R.id.itemName)
    val quantity: TextView = view.findViewById(R.id.itemQuantity)
    val remark: TextView = view.findViewById(R.id.itemRemark)
    val portions: TextView = view.findViewById(R.id.itemPortions)
    val detail: TextView = view.findViewById(R.id.itemDetail)
    val location: TextView = view.findViewById(R.id.itemLocation)
    val status: TextView = view.findViewById(R.id.itemStatus)
    val btnTakeOne: Button = view.findViewById(R.id.btnTakeOne)
    val btnTakeAll: Button = view.findViewById(R.id.btnTakeAll)
}
}
// Version: V2.9.0-RC1 (Refactor)
