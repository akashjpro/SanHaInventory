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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.example.inventory.data.Item
import com.example.inventory.data.ItemDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.text.Normalizer

/**
 * View Model to keep a reference to the Inventory repository and an up-to-date list of all items.
 *
 */
class InventoryViewModel(private val itemDao: ItemDao) : ViewModel() {

    // Cache all items form the database using LiveData.
    val allItems: LiveData<List<Item>> = itemDao.getItems().asLiveData()

    // Private MutableLiveData để thay đổi giá trị
    private val _query: MutableLiveData<String> = MutableLiveData("")

    // Public LiveData để quan sát, không thể thay đổi từ bên ngoài
    val query: LiveData<String> get() = _query

    private var listItem = listOf<Item>()
    private val _itemList = MutableStateFlow<List<Item>>(emptyList())
    val itemList: StateFlow<List<Item>> get() = _itemList

    private val searchQuery = MutableStateFlow("")

    private val indexSelectedSpinner = MutableStateFlow(0)



    fun search(query: String) {
        searchQuery.value = query
    }

    fun updateIndexSelected(index: Int) {
        indexSelectedSpinner.value = index
    }

//    val items = searchQuery
//        .debounce(300) // Chống spam tìm kiếm
//        .flatMapLatest { query ->
//            Pager(PagingConfig(pageSize = 20)) {
//                ItemPagingSource(listItem, query)
//            }.flow
//        }
//        .cachedIn(viewModelScope)

    val items = combine(
        indexSelectedSpinner,
        searchQuery.debounce(300) // Thêm debounce 300ms vào query
    ) { index, query ->
        Pager(PagingConfig(pageSize = 20)) {
            val originalList =  searchItems(query, listItem, index)
            _itemList.value = originalList
            ItemPagingSource(originalList)
        }.flow
    }.flatMapLatest { it }
        .cachedIn(viewModelScope)

    fun updateItemList(items: List<Item>){
        listItem = items
    }

    fun getListItemSize()  = listItem.size

    // Hàm thay đổi giá trị của query
    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }
    /**
     * Returns true if stock is available to sell, false otherwise.
     */
    fun isStockAvailable(item: Item): Boolean {
        return (item.quantityInStock > 0)
    }

    /**
     * Updates an existing Item in the database.
     */
    fun updateItem(
        itemId: Long,
        itemName: String,
        itemPrice: String,
        itemCount: String
    ) {
        val updatedItem = getUpdatedItemEntry(itemId, itemName, itemPrice, itemCount)
        updateItem(updatedItem)
    }


    /**
     * Launching a new coroutine to update an item in a non-blocking way
     */
    fun updateItem(item: Item) {
        viewModelScope.launch {
            itemDao.update(item)
        }
    }

    /**
     * Decreases the stock by one unit and updates the database.
     */
    fun sellItem(item: Item) {
        if (item.quantityInStock > 0) {
            // Decrease the quantity by 1
            val newItem = item.copy(quantityInStock = item.quantityInStock - 1)
            updateItem(newItem)
        }
    }

    /**
     * Inserts the new Item into database.
     */
    fun addNewItem(itemName: String, itemPrice: String, itemCount: String) {
        val newItem = getNewItemEntry(itemName, itemPrice, itemCount)
        insertItem(newItem)
    }

    /**
     * Inserts the new Item into database.
     */
    fun addItem(
        item :Item
    ) {
        insertItem(item)
    }

    /**
     * Inserts the new Item into database.
     */
    suspend fun addAllItem(
        items: List<Item>
    ) {
        viewModelScope.launch {
            itemDao.insertAll(items)
        }
    }

    suspend fun deleteAndAddAllItem(
        items: List<Item>
    ) {
        viewModelScope.launch {
            itemDao.deleteAllItems()
            itemDao.insertAll(items)
        }
    }

    suspend fun deleteAndAddItem(
        item: Item
    ) {
        viewModelScope.launch {
            itemDao.delete(item)
            itemDao.insert(item.copy(isScan = false))
        }

    }

    /**
     * Launching a new coroutine to insert an item in a non-blocking way
     */
    private fun insertItem(item: Item) {
        viewModelScope.launch {
            itemDao.insert(item)
        }
    }

    private fun insertAllItem(items: List<Item>) {
        viewModelScope.launch {
            itemDao.insertAll(items)
        }
    }

    /**
     * Launching a new coroutine to delete an item in a non-blocking way
     */
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            itemDao.delete(item)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            itemDao.deleteAllItems()
        }
    }


    /**
     * Retrieve an item from the repository.
     */
    fun retrieveItem(id: Long): LiveData<Item> {
        return itemDao.getItem(id).asLiveData()
    }

    /**
     * Returns true if the EditTexts are not empty
     */
    fun isEntryValid(itemName: String, itemPrice: String, itemCount: String): Boolean {
        if (itemName.isBlank() || itemPrice.isBlank() || itemCount.isBlank()) {
            return false
        }
        return true
    }

    /**
     * Returns an instance of the [Item] entity class with the item info entered by the user.
     * This will be used to add a new entry to the Inventory database.
     */
    private fun getNewItemEntry(itemName: String, itemPrice: String, itemCount: String): Item {
        return Item(
            itemName = itemName,
            itemPrice = itemPrice.toDouble(),
            quantityInStock = itemCount.toInt(),
        )
    }


    /**
     * Called to update an existing entry in the Inventory database.
     * Returns an instance of the [Item] entity class with the item info updated by the user.
     */
    private fun getUpdatedItemEntry(
        itemId: Long,
        itemName: String,
        itemPrice: String,
        itemCount: String
    ): Item {
        return Item(
            id = itemId,
            itemName = itemName,
            itemPrice = itemPrice.toDouble(),
            quantityInStock = itemCount.toInt()
        )
    }

    fun searchItems(keyword: String, items: List<Item>,  indexSelectedSpinner:  Int = 0): List<Item> {
        val normalizedKeyword = removeVietnameseAccents(keyword).lowercase()
        return items.filter {
            val normalizedItem =  if (indexSelectedSpinner == 0) removeVietnameseAccents(it.itemName).lowercase() else removeVietnameseAccents(it.index).lowercase()
            normalizedItem.contains(normalizedKeyword)
        }
    }

    fun removeVietnameseAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}

/**
 * Factory class to instantiate the [ViewModel] instance.
 */
class InventoryViewModelFactory(private val itemDao: ItemDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(itemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

