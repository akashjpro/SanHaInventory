package com.example.inventory

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.inventory.data.Item
import com.example.inventory.databinding.ItemListItemBinding

class ItemAdapter(private val context: Context, private val listItem: List<Item>, private val onItemClicked: (Item) -> Unit)  : PagingDataAdapter<Item, ItemAdapter.ItemViewHolder>(ItemDiffCallback()) {
    private var  VIEW_TYPE_1 = 1
    private var  VIEW_TYPE_2 = 2


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemListItemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                )
            )
        )
    }


    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val current = getItem(position)
        current?.let {
            holder.itemView.setOnClickListener {
                onItemClicked(current)
            }
            holder.bind(context, current)
        }

    }

    class ItemViewHolder(private var binding: ItemListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(context: Context, item: Item) {
            binding.itemName.text = item.itemName
            var price = item.brcch
            if(!item.isFormat) {
                price = NumberUtils.formatNumberPrice(item.brcch.replace("VND","").replace(",", "").trim()) + " VND"
            }
            binding.itemPrice.text =  price
            binding.itemBarCode.text = item.barCode
            binding.itemId.text = if (item.index.length >= 8) item.index.substring(0, 8) else "N/A"
            binding.itemUom.text = item.uom
            binding.itemInStock.text = item.inStock
            binding.itemTime.text = DateUtils.convertMillisToDateModern(item.timestamp)
            binding.iBntCopy.setOnClickListener {
                if (item.barCode.isNotEmpty()) {
                    copyToClipboard(context, item.barCode)
                }

            }
            binding.iBntCopyItem.setOnClickListener {
                if (item.index.isNotEmpty()) {
                    copyToClipboard(context, if (item.index.length >= 8) item.index.substring(0, 8) else "")
                }

            }
            binding.itemEnd.visibility = View.GONE
        }

        // Hàm copy chuỗi vào clipboard
        private fun copyToClipboard(context: Context, text: String) {
            val clipboardManager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Copied Text", text)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(context, "Đã sao chép: $text", Toast.LENGTH_SHORT).show()
            Log.d("Clipboard", "Đã sao chép: $text")
        }
    }



//    companion object {
//        private val DiffCallback = object : DiffUtil.ItemCallback<Item>() {
//            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
//                return oldItem === newItem
//            }
//
//            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
//                return oldItem.id == newItem.id
//            }
//        }
//    }
}

class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
    override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem == newItem
    }
}
