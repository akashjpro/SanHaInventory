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

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.example.inventory.data.Item
import com.example.inventory.databinding.ActivityMainBinding
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import android.Manifest
import android.text.TextUtils.split
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.text.Normalizer


class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent

    private lateinit var navController: NavController

    private var itemMenuSelected: String = ""

    private var listItem = listOf<Item>()

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val AUDIO_PERMISSION_REQUEST_CODE = 101
        private const val ITEM_MENU_FILE_ITEM = "99"
        private const val ITEM_MENU_FILE_KK = "98"
    }


    private var loadingDialog: Dialog? = null

    private val viewModel: InventoryViewModel by viewModels {
        InventoryViewModelFactory(
            (this.application as InventoryApplication).database
                .itemDao()
        )
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.allItems.observe(this) { items ->
            items?.let {
                listItem = it
            }
        }



//        val navView: BottomNavigationView = binding.navView

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )

//        // Retrieve NavController from the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

//        val navController = findNavController(R.id.nav_host_fragment)

        // Set up the action bar for use with the NavController
        setupActionBarWithNavController(this, navController)

//        navView.setupWithNavController(navController)

        binding.floatingActionButton.setOnClickListener {
            startScanning()
        }

        // Khởi tạo SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        // Cấu hình Intent cho SpeechRecognizer
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN") // Chọn ngôn ngữ (Tiếng Việt)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói gì đó...") // (Tùy chọn) Thêm prompt
        }

        // Thiết lập RecognitionListener
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(applicationContext, "Sẵn sàng nhận giọng nói", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onBeginningOfSpeech() {
                // Khi bắt đầu nói
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Thay đổi mức độ âm thanh
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Khi dữ liệu âm thanh được nhận
            }

            override fun onEndOfSpeech() {
                Toast.makeText(applicationContext, "Kết thúc nói", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Lỗi: Hết thời gian mạng"
                    SpeechRecognizer.ERROR_NETWORK -> "Lỗi: Kết nối mạng"
                    SpeechRecognizer.ERROR_AUDIO -> "Lỗi: Âm thanh"
                    SpeechRecognizer.ERROR_SERVER -> "Lỗi: Máy chủ"
                    SpeechRecognizer.ERROR_CLIENT -> "Lỗi ứng dụng"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Lỗi: Không phát hiện giọng nói"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Lỗi: Không có kết quả khớp"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Lỗi: Bộ nhận diện đang bận"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Lỗi: Không đủ quyền"
                    else -> "Lỗi không xác định"
                }
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val query = matches?.get(0) ?: ""

                Toast.makeText(applicationContext, query, Toast.LENGTH_SHORT).show()
                // Thực hiện tìm kiếm với từ khóa nhận diện
                performSearch(query)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Khi có kết quả một phần
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Xử lý sự kiện khác
            }
        }

        // Gán listener cho SpeechRecognizer
        speechRecognizer.setRecognitionListener(listener)

    }

    /**
     * Handle navigation when the user chooses Up from the action bar.
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)


        // Lấy SearchView từ menu
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        // Lắng nghe thay đổi trong SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Xử lý khi người dùng nhấn Enter
                query?.let {
                    // val queryWithoutAccent = removeVietnameseAccents(it).lowercase()
                    performSearch(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Xử lý khi văn bản thay đổi
                newText?.let {
                    // val searchQuery = removeVietnameseAccents(it).lowercase()
                    performSearch(it)
                }
                return true
            }
        })


        searchView.setOnCloseListener {
            // Xử lý khi người dùng bấm dấu "X" để xóa nội dung tìm kiếm
            // Reset lại dữ liệu hoặc UI khi bấm "X"
            performSearch("")
            true // Trả về true để cho phép SearchView đóng
        }


        return true
    }

    fun removeVietnameseAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    private fun performSearch(query: String) {

        viewModel.updateQuery(query)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                itemMenuSelected = ITEM_MENU_FILE_ITEM
                CoroutineScope(Dispatchers.IO).launch {
                    openFilePicker()
                }

                true
            }

//            R.id.action_voice -> {
//                if (checkAudioPermission()) {
//                    speechRecognizer.startListening(recognizerIntent)
//                } else {
//                    requestAudioPermission()
//                }
//
//                true
//            }

            R.id.action_delete_all -> {
                viewModel.deleteAll()

                true
            }

            R.id.action_delete_all_scan -> {
                CoroutineScope(Dispatchers.IO).launch {
                    listItem.filter { it.isScan }.apply {
                        this.forEach { item ->
                            if(listItem.find { it.barCode == item.barCode } != null) {
                                viewModel.deleteAndAddItem(item)
                            } else {
                                viewModel.deleteItem(item)
                            }

                        }
                    }
                }

                true
            }

            R.id.action_kiem_ke -> {
                itemMenuSelected = ITEM_MENU_FILE_KK
                CoroutineScope(Dispatchers.IO).launch {
                    openFilePicker()
                }

                true
            }


            else -> super.onOptionsItemSelected(item)
        }
    }

    fun showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = Dialog(this@MainActivity).apply {
                setContentView(R.layout.loading_dialog)
                setCancelable(false) // Không cho người dùng thoát
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
        }
        loadingDialog?.show()

    }

    fun hideLoadingDialog() {
        loadingDialog?.dismiss()
    }

    private fun startScanning() {
        val integrator = IntentIntegrator(this)
        integrator.setCaptureActivity(CustomCaptureActivity::class.java)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrator.setPrompt("Scan a barcode or QR code")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(false)
        scanLauncher.launch(integrator.createScanIntent())
    }


    // Xử lý kết quả yêu cầu quyền
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền được cấp, tiếp tục quét
                // startScanning()
            } else {
                // Quyền bị từ chối, thông báo cho người dùng
                Toast.makeText(
                    this,
                    "Camera permission is required to scan QR/Barcode",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

//    // Xử lý kết quả yêu cầu quyền
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        when (requestCode) {
//            AUDIO_PERMISSION_REQUEST_CODE -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(this, "Quyền ghi âm đã được cấp", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(this, "Quyền ghi âm bị từ chối", Toast.LENGTH_SHORT).show()
//                }
//            }
//            CAMERA_PERMISSION_REQUEST_CODE -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(this, "Quyền camera đã được cấp", Toast.LENGTH_SHORT).show()
//                    startScanning() // Nếu quyền được cấp, bắt đầu quét
//                } else {
//                    Toast.makeText(this, "Quyền camera bị từ chối", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }

//
//    // Nhận kết quả quét
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        Log.d("8888888888", "888888888======")
//        val result: IntentResult =
//            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
//        if (result != null) {
//            if (result.contents != null) {
//                Toast.makeText(this, "Scanned: ${result.contents}", Toast.LENGTH_LONG).show()
//
//                val item = listItem.find { it.barCode == result.contents }
//                if (item != null) {
////                    viewModel.deleteItem(item)
////                    viewModel.addItem(
////                        Item(
////                            index = item.index,
////                            itemName = item.itemName,
////                            inStock = item.inStock,
////                            barCode = result.contents,
////                            group = item.group,
////                            lastPurchase = item.lastPurchase,
////                            brcch = item.brcch,
////                            isScan = true,
////                            timestamp = System.currentTimeMillis(),
////                        )
////                    )
////                    Toast.makeText(this, "UPdate 99999999999", Toast.LENGTH_LONG).show()
//
//                    viewModel.deleteItem(item)
//                    viewModel.addItem(
//                        item.copy(
//                            barCode = result.contents,
//                            isScan = true,
//                            timestamp = System.currentTimeMillis()
//                        )
//                    )
//                    Toast.makeText(this, "Updated successfully", Toast.LENGTH_LONG).show()
//
//                } else {
//                    viewModel.addItem(
//                        Item(
//                            isScan = true,
//                            barCode = result.contents,
//
//                            timestamp = System.currentTimeMillis(),
//                        )
//                    )
//                }
//
//            } else {
//                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (data == null) {
               // Toast.makeText(this, "No scan data received!", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val intentResult = IntentIntegrator.parseActivityResult(
                IntentIntegrator.REQUEST_CODE,
                result.resultCode,
                data
            )

            if (intentResult != null) {
                if (intentResult.contents != null) {

                    val item = listItem.find { it.barCode == intentResult.contents }
                    if (item != null) {
                        viewModel.deleteItem(item)
                        viewModel.addItem(
                            item.copy(
                                barCode = intentResult.contents,
                                isScan = true,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    } else {
                        if (intentResult.contents.length == 16) {
                            val index = intentResult.contents.substring(2, 10)
                            val endNumber = intentResult.contents.substring(12)
                            val item = listItem.find { it.index == index}
                            item?.let {
                                val brcchConver =  convertFormattedStringToNumber(it.brcch)
                                val formatEnd =  formatNumberToDouble(endNumber)

                                println("brcchConver: $brcchConver")

                                var price =
                                    ((formatEnd * brcchConver) ).toString()

                               // Toast.makeText(this@MainActivity, "price : $price", Toast.LENGTH_SHORT).show()

                                viewModel.addItem(
                                    it.copy(
                                        id =  System.currentTimeMillis(),
                                        index = index + " | "+System.currentTimeMillis().toString(),
                                        isFormat = true,
                                        inStock = formatEnd.toString() + " | " + it.inStock ,
                                        brcch = NumberUtils.formatNumberPrice(price) + " | " +  NumberUtils.formatNumberPrice(it.brcch.replace("VND","").replace(",", "").trim() )+ "  VND",
                                        barCode = intentResult.contents,
                                        isScan = true,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }

                        } else {
                            if (intentResult.contents.length == 8) {
                                val index = intentResult.contents
                                val item = listItem.find { it.index == index}
                                item?.let {
                                    viewModel.deleteItem(it)
                                    viewModel.addItem(
                                        it.copy(
                                            barCode = intentResult.contents,
                                            isScan = true,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                }

                            } else {
                                viewModel.addItem(
                                    Item(
                                        isScan = true,
                                        barCode = intentResult.contents,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No scan data received!", Toast.LENGTH_SHORT).show()
            }
        }

    private fun formatNumberToDouble(input: String): Double {
        println("formatEnd_START: $input")
        val result = "0.${input.substring(1)}".toDouble()
        println("formatEnd_END: $input")
        return result
    }

    private fun convertFormattedStringToNumber(input: String): Double {
        // Loại bỏ dấu phẩy và phần thập phân
        val cleanedInput = input.replace(",", "").replace("VND", "").trim().split(".")[0]
        Toast.makeText(this, "cleanedInput: $cleanedInput", Toast.LENGTH_SHORT).show()
        return cleanedInput.toDouble()
    }



    private var selectedFileUri: Uri? = null

    // Bộ chọn tệp (file picker)
    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->

            Log.d("FilePicker", "Đ9999999======")
            if (uri != null) {
                selectedFileUri = uri
                Log.d("FilePicker", "Đã chọn tệp: $uri")
                // Gọi hàm đọc tệp Excel từ URI

                if (itemMenuSelected == ITEM_MENU_FILE_ITEM) {
                    CoroutineScope(Dispatchers.IO).launch {
                        readExcelFileItemFromUri(uri)
                    }

                }
                if (itemMenuSelected == ITEM_MENU_FILE_KK) {
                    CoroutineScope(Dispatchers.IO).launch {
                        readExcelFileKKFromUri(uri)
                    }
                }

            } else {
                Log.e("FilePicker", "Người dùng không chọn tệp.")
            }
        }

    // Hàm mở trình chọn tệp
    private fun openFilePicker() {
        filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    }


    // Hàm đọc nội dung tệp Excel từ URI
    private suspend fun readExcelFileItemFromUri(uri: Uri) {
        withContext(Dispatchers.Main) {
            showLoadingDialog()
        }

        try {
            // Lấy InputStream từ URI
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val itemList = mutableListOf<Item>()

                // Dùng WorkbookFactory để đọc file Excel
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0) // Đọc sheet đầu tiên

                for (row in sheet) {
                    if (row.rowNum == 0) continue // Bỏ qua hàng tiêu đề

                    val index = row.getCell(0)?.toString()?.trim() ?: "N/A"
                    val description = row.getCell(1)?.toString() ?: ""
                    val inStock = ""
                    val barCode = row.getCell(3)?.toString()?.trim() ?: ""
                    val group = row.getCell(4)?.toString() ?: ""
                    val lastPurchase = row.getCell(8)?.toString() ?: ""
                    val brcch = row.getCell(9)?.toString() ?: ""

            println("index99999999: $index")
            println("description: $description")
            println("inStock: $inStock")
                    println("barCode: $barCode")
            println("group: $group")
            println("lastPurchase: $lastPurchase")
            println("brcch: $brcch")


                    var resultBarCode =
                        if (barCode.length > 13) barCode.substring(0, barCode.length - 3)
                            .replace(".", "")
                            .trim() else barCode
                    val resultLastPurchase =
                        if (lastPurchase.length > 3) lastPurchase.replace("VND", "")
                            .trim() + "  VND" else ""


                    val item = Item(
                        index = index,
                        itemName = description,
                        inStock = inStock,
                        barCode = resultBarCode,
                        group = group,
                        lastPurchase = resultLastPurchase,
                        brcch = brcch,
                        timestamp = System.currentTimeMillis(),
                    )
                    itemList.add(item)
                }

                viewModel.addAllItem(itemList)
                withContext(Dispatchers.Main) {
                    hideLoadingDialog()
                }
                workbook.close()
            } else {
                Log.e("ExcelReader", "Không thể mở InputStream từ Uri.")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                hideLoadingDialog()
            }
            e.printStackTrace()
            Log.e("ExcelReader", "Lỗi khi đọc tệp Excel: ${e.message}")
        }
    }

    // Hàm đọc nội dung tệp Excel từ URI
    private suspend fun readExcelFileKKFromUri(uri: Uri) {
        withContext(Dispatchers.Main) {
            showLoadingDialog()
        }

        try {
            // Lấy InputStream từ URI
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                var itemList = listItem

                // Dùng WorkbookFactory để đọc file Excel
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0) // Đọc sheet đầu tiên

                for (row in sheet) {
                    if (row.rowNum == 0) continue // Bỏ qua hàng tiêu đề

                    val index = row.getCell(1)?.toString() ?: ""
                    val uom = row.getCell(5)?.toString() ?: ""
                    val inStock = row.getCell(6)?.toString() ?: ""


                    println("index: $index")
                    println("inStock: ${inStock}")


                    itemList = itemList.map {
                        if (it.index == index) it.copy(uom = uom, inStock = inStock) else it
                    }

                }

                viewModel.deleteAndAddAllItem(itemList)
                withContext(Dispatchers.Main) {
                    hideLoadingDialog()
                }
                workbook.close()
            } else {
                Log.e("ExcelReader", "Không thể mở InputStream từ Uri.")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                hideLoadingDialog()
            }
            e.printStackTrace()
            Log.e("ExcelReader", "Lỗi khi đọc tệp Excel: ${e.message}")
        }
    }


    // Hàm kiểm tra quyền RECORD_AUDIO
    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Hàm yêu cầu quyền RECORD_AUDIO
    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            AUDIO_PERMISSION_REQUEST_CODE
        )
    }
}
