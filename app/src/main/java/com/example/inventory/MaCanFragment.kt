package com.example.inventory

import android.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
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

class MaCanFragment : Fragment() {

    companion object {
        fun newInstance() = MaCanFragment()
    }

    private val viewModel: InventoryViewModel by activityViewModels {
        InventoryViewModelFactory(
            (activity?.application as InventoryApplication).database.itemDao()
        )
    }

    private var _binding: ItemListFragmentBinding? = null
    private val binding get() = _binding!!

    private var copyContent:  String = ""
    private var listItem = listOf<Item>()

    private var indexSelectedSpinner:  Int = 0



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


        // List of items for the spinner
        val items = listOf("Theo tên", "Theo id")

        // Creating an ArrayAdapter to display the items in the spinner
        val adapterSpinner = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, items)
        adapterSpinner.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)

        // Set the adapter to the spinner
        binding.spinner.adapter = adapterSpinner

        // Set up the ItemSelectedListener to capture the selected value
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                // Get the selected value
                indexSelectedSpinner = position
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Do something if nothing is selected (optional)
            }
        }

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
                adapter.submitList(it)
                binding.txtSum.text = it.size.toString()
                listItem = it
            }
        }

        viewModel.query.observe(this.viewLifecycleOwner) { query ->
            CoroutineScope(Dispatchers.IO).launch {
                query?.let {
                    if(query.isNotEmpty()) {
                        // Thực hiện tìm kiếm (ví dụ: lọc danh sách hoặc gọi API)
                        // val filteredList = listItem.filter { removeVietnameseAccents(it.itemName).contains(query, ignoreCase = true) }
                        val filteredList = searchItems(query, listItem)
                        withContext(Dispatchers.Main) {
                            adapter.submitList(filteredList)
                            binding.txtSum.text = filteredList.size.toString()
                        }

                    } else {
                        withContext(Dispatchers.Main) {
                            adapter.submitList(listItem)
                            binding.txtSum.text = listItem.size.toString()
                        }
                    }

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
                    listItem.map { if (it.index.length >= 8) it.index.substring(0, 8) else "" }.forEach {
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
            val normalizedItem =    if (indexSelectedSpinner == 0) removeVietnameseAccents(it.itemName).lowercase() else removeVietnameseAccents(it.index).lowercase()
            normalizedItem.contains(normalizedKeyword)
        }
    }


}