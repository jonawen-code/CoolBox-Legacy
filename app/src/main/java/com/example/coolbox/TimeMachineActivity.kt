package com.example.coolbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coolbox.data.FoodEntity
import com.example.coolbox.ui.MainViewModel
import com.example.coolbox.legacy.R
import java.text.SimpleDateFormat
import java.util.*

class TimeMachineActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private var firstItem: FoodEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_machine)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        val btnSimulateExpiry = findViewById<Button>(R.id.btnSimulateExpiry)
        val btnSimulateExpiring = findViewById<Button>(R.id.btnSimulateExpiring)
        val recyclerView = findViewById<RecyclerView>(R.id.testFoodList)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = TestAdapter()
        recyclerView.adapter = adapter

        viewModel.allFood.observe(this) { food ->
            firstItem = food.firstOrNull()
            adapter.submitList(food)
        }

        btnSimulateExpiry.setOnClickListener {
            firstItem?.let {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val updated = it.copy(expiryDateMs = cal.timeInMillis)
                viewModel.updateItem(updated)
                Toast.makeText(this, "已模拟过期，返回首页查看警告", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "冰箱里没有食物", Toast.LENGTH_SHORT).show()
        }

        btnSimulateExpiring.setOnClickListener {
            firstItem?.let {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val updated = it.copy(expiryDateMs = cal.timeInMillis)
                viewModel.updateItem(updated)
                Toast.makeText(this, "已模拟临期，返回首页查看警告", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "冰箱里没有食物", Toast.LENGTH_SHORT).show()
        }
    }

    class TestAdapter : RecyclerView.Adapter<TestAdapter.ViewHolder>() {
        private var items = emptyList<FoodEntity>()

        fun submitList(newItems: List<FoodEntity>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            holder.text1.text = "${item.icon} ${item.name}"
            holder.text2.text = "过期时间: ${df.format(Date(item.expiryDateMs))}"
            
            val now = System.currentTimeMillis()
            if (item.expiryDateMs < now) {
                holder.text2.setTextColor(android.graphics.Color.RED)
            } else if (item.expiryDateMs < now + 3 * 24 * 60 * 60 * 1000L) {
                holder.text2.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            } else {
                holder.text2.setTextColor(android.graphics.Color.GRAY)
            }
        }

        override fun getItemCount() = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text1: TextView = view.findViewById(android.R.id.text1)
            val text2: TextView = view.findViewById(android.R.id.text2)
        }
    }
}
