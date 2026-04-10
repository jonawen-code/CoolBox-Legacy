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
private val getFridgeBases: () -> List<String>,
private val getCurrentSpace: () -> Int // Added space awareness
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
    val space = getCurrentSpace()
    val context = holder.itemView.context
    
    val activeColor = if (space == 1) context.resources.getColor(R.color.med_primary) else context.resources.getColor(R.color.food_primary)
    val lightColor = if (space == 1) context.resources.getColor(R.color.blue_light) else context.resources.getColor(R.color.green_light)

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
    if (item.portions > 1) {
        holder.portions.text = "${item.portions} 份，${formatQuantity(item.weightPerPortion)}${item.unit}/份"
    } else {
        holder.portions.text = "1 份"
    }
    
    holder.remark.visibility = View.VISIBLE
    if (item.remark.isNotBlank()) {
        holder.remark.text = "注：${item.remark}"
    } else {
        holder.remark.text = " " // Stable empty line for height consistency
    }
    // Build 44: 保持与底层数据及其他对话框完全一致的显示格式，不遮蔽横杠
    val shortLoc = item.fridgeName.replace("室", "").replace("第", "")
    holder.location.text = "在：$shortLoc"
    applySafeIcon(holder.icon, holder.iconEmoji, item)
    holder.icon.setOnClickListener {
        (holder.itemView.context as MainActivity).actionHelper.showIconPickerDialog(item)
    }
    holder.iconEmoji.setOnClickListener {
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
        holder.status.setTextColor(android.graphics.Color.WHITE)
        
        // Build 4.0: White text on Red background for visibility
        val white = android.graphics.Color.WHITE
        holder.name.setTextColor(white)
        holder.detail.setTextColor(white)
        holder.quantity.setTextColor(white)
        holder.portions.setTextColor(white)
        holder.remark.setTextColor(white)
        holder.location.setTextColor(white)
    } else if (currentNowMs >= nearExpiryThreshold) {
        container.setBackgroundResource(R.drawable.bg_item_near)
        holder.status.visibility = View.VISIBLE
        val daysRemaining = Math.max(0, Math.ceil((item.expiryDateMs - currentNowMs).toDouble() / (24 * 60 * 60 * 1000)).toInt())
        holder.status.text = "临期提醒: 剩余 $daysRemaining 天"
        holder.status.setTextColor(android.graphics.Color.parseColor("#FF9800"))
        
        // Reset colors for non-expired
        val black = android.graphics.Color.parseColor("#333333")
        holder.name.setTextColor(black)
        holder.detail.setTextColor(android.graphics.Color.GRAY)
        holder.quantity.setTextColor(black)
        holder.portions.setTextColor(android.graphics.Color.GRAY)
        holder.remark.setTextColor(android.graphics.Color.GRAY)
        holder.location.setTextColor(android.graphics.Color.GRAY)
    } else {
        container.setBackgroundResource(R.drawable.bg_item_normal)
        holder.status.visibility = View.GONE
        
        val black = android.graphics.Color.parseColor("#333333")
        holder.name.setTextColor(black)
        holder.detail.setTextColor(android.graphics.Color.GRAY)
        holder.quantity.setTextColor(black)
        holder.portions.setTextColor(android.graphics.Color.GRAY)
        holder.remark.setTextColor(android.graphics.Color.GRAY)
        holder.location.setTextColor(android.graphics.Color.GRAY)
    }
    
    // Action Button Colors
    val btnOneBg = if (space == 1) R.drawable.bg_btn_item_one else R.drawable.bg_btn_item_one_green
    val btnAllBg = if (space == 1) R.drawable.bg_btn_item_all else R.drawable.bg_btn_item_all_green
    val btnOneTextColor = if (space == 1) android.graphics.Color.parseColor("#FF1976D2") else android.graphics.Color.parseColor("#FF388E3C")
    val btnAllTextColor = if (space == 1) android.graphics.Color.parseColor("#FF1976D2") else android.graphics.Color.parseColor("#FF388E3C")

    holder.btnTakeOne.setBackgroundResource(btnOneBg)
    holder.btnTakeOne.setTextColor(btnOneTextColor)
    holder.btnTakeAll.setBackgroundResource(btnAllBg)
    holder.btnTakeAll.setTextColor(btnAllTextColor)

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
private fun applySafeIcon(imageView: ImageView, emojiView: TextView, item: FoodEntity) {
    val name = item.name
    val iconName = item.icon
    val context = imageView.context
    
    // Build 4.0: Handle Emoji Icons
    if (iconName.length < 4 && iconName.any { Character.isSurrogate(it) || it.code > 127 }) {
        imageView.visibility = View.GONE
        emojiView.visibility = View.VISIBLE
        emojiView.text = iconName
        return
    }

    imageView.visibility = View.VISIBLE
    emojiView.visibility = View.GONE

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
    val iconEmoji: TextView = view.findViewById(R.id.itemIconEmoji)
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
