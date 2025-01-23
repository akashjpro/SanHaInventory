package com.example.inventory

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.inventory.data.Item
import java.text.Normalizer

class ItemPagingSource(
    private val originalList: List<Item>,
//    private val query: String,
//    private var indexSelectedSpinner:  Int = 0
) : PagingSource<Int, Item>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val filteredList = originalList
        val start = page * pageSize
        val end = minOf(start + pageSize, filteredList.size)

        return try {
            LoadResult.Page(
                data = filteredList.subList(start, end),
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (end == filteredList.size) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Item>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

//    fun searchItems(keyword: String, items: List<Item>): List<Item> {
//        val normalizedKeyword = removeVietnameseAccents(keyword).lowercase()
//        return items.filter {
//            val normalizedItem =  if (indexSelectedSpinner == 0) removeVietnameseAccents(it.itemName).lowercase() else removeVietnameseAccents(it.index).lowercase()
//            normalizedItem.contains(normalizedKeyword)
//        }
//    }
//
//    fun removeVietnameseAccents(input: String): String {
//        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
//        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
//    }

}
