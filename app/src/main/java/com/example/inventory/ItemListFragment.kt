/*
 * Copyright (C) 2021 The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.inventory

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventory.data.Item
import com.example.inventory.databinding.ItemListFragmentBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

/**
 * Main fragment displaying details for all items in the database.
 */
class ItemListFragment : Fragment() {
    private val viewModel: InventoryViewModel by activityViewModels {
        InventoryViewModelFactory(
            (activity?.application as InventoryApplication).database.itemDao()
        )
    }

    private var _binding: ItemListFragmentBinding? = null
    private val binding get() = _binding!!

    private var copyContent:  String = ""
    private var listItem = listOf<Item>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ItemListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ItemListAdapter(requireContext(), listItem) {
            val action =
                ItemListFragmentDirections.actionItemListFragmentToItemDetailFragment(it.id)
            this.findNavController().navigate(action)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)
        binding.recyclerView.adapter = adapter
        // Attach an observer on the allItems list to update the UI automatically when the data
        // changes.
        viewModel.allItems.observe(this.viewLifecycleOwner) { items ->
            items?.let {
                it.filter { it.isScan }.apply {
                    adapter.submitList(this)
                    binding.txtSum.text = size.toString()
                    listItem = this
                }
            }
        }

        viewModel.query.observe(this.viewLifecycleOwner) { query ->
            query?.let {
                if(query.isNotEmpty()) {
                    // Thực hiện tìm kiếm (ví dụ: lọc danh sách hoặc gọi API)
                   // val filteredList = listItem.filter { removeVietnameseAccents(it.itemName).contains(query, ignoreCase = true) }
                    val filteredList = searchItems(query, listItem)
                    adapter.submitList(filteredList)
                } else {
                    adapter.submitList(listItem)
                }

            }

        }

        binding.iBntCopyAllIItem.setOnClickListener {

            listItem.forEach {
                println("Item: ${it.index}")
            }

            if(listItem.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val stringBuilder = StringBuilder()
                    listItem.map { it.index }.forEach {
                        stringBuilder.append("\n$it")
                    }
                    val copyContent = stringBuilder.toString() // Chuyển StringBuilder thành String
                    if(copyContent.isNotEmpty()) {
                        copyToClipboard(requireContext(), copyContent)
                    }

                }
            }
        }

        binding.iBntCopyAll.setOnClickListener {

            if(listItem.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val stringBuilder = StringBuilder()
                    listItem.map { it.barCode }.forEach {
                        stringBuilder.append("\n$it")
                    }
                    val copyContent = stringBuilder.toString() // Chuyển StringBuilder thành String
                    if(copyContent.isNotEmpty()) {
                        copyToClipboard(requireContext(), copyContent)
                    }

                }
            }
        }

    }
    // Hàm copy chuỗi vào clipboard
    private suspend fun copyToClipboard(context: Context, text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Copied Text", text)
        clipboardManager.setPrimaryClip(clipData)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Đã sao chép: $text", Toast.LENGTH_SHORT).show()
        }

        Log.d("Clipboard", "Đã sao chép: $text")
    }


    fun removeVietnameseAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    fun searchItems(keyword: String, items: List<Item>): List<Item> {
        val normalizedKeyword = removeVietnameseAccents(keyword).lowercase()
        return items.filter {
            val normalizedItem = removeVietnameseAccents(it.itemName).lowercase()
            normalizedItem.contains(normalizedKeyword)
        }
    }



}
