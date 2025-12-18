package com.example.myproject

import android.content.res.AssetManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myproject.databinding.ActivityMainBinding
import com.google.firebase.database.DatabaseReference // Import ini
import com.google.firebase.database.FirebaseDatabase // Import ini
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var tflite: Interpreter? = null
    private val modelName = "model_harga_terbaru.tflite"

    // --- TAMBAHAN FIREBASE: Variabel Database ---
    private lateinit var database: DatabaseReference

    private val ramOptions = listOf(2, 3, 4, 6, 8, 12, 16)
    private val storageOptions = listOf(32, 64, 128, 256, 512, 1024)
    private val brandOptions = listOf("iPhone", "Samsung", "Xiaomi", "Oppo", "Vivo", "Infinix")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- TAMBAHAN FIREBASE: Inisialisasi Database ---
        // "riwayat_prediksi" adalah nama folder (node) di dalam database nanti
        database = FirebaseDatabase.getInstance().getReference("riwayat_prediksi")

        setupUI()
        loadModelAsync()

        binding.btnPredict.setOnClickListener {
            predictPrice()
        }
    }

    private fun loadModelAsync() {
        Executors.newSingleThreadExecutor().execute {
            try {
                tflite = Interpreter(loadModelFile(assets, modelName))
                runOnUiThread {
                    Toast.makeText(this, "Model AI Siap!", Toast.LENGTH_SHORT).show()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Gagal load model: ${ex.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupUI() {
        // ... (Kode Setup UI Anda sama persis, tidak ada perubahan) ...
        val brandAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, brandOptions)
        binding.spnBrand.adapter = brandAdapter

        binding.spnBrand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> binding.ivBrandImage.setImageResource(R.drawable.img_iphone)
                    1 -> binding.ivBrandImage.setImageResource(R.drawable.img_samsung)
                    2 -> binding.ivBrandImage.setImageResource(R.drawable.img_xiaomi)
                    3 -> binding.ivBrandImage.setImageResource(R.drawable.img_oppo)
                    4 -> binding.ivBrandImage.setImageResource(R.drawable.img_vivo)
                    5 -> binding.ivBrandImage.setImageResource(R.drawable.img_infinix)
                    else -> binding.ivBrandImage.setImageResource(R.drawable.img_default)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val ramDisplay = ramOptions.map { "$it GB" }
        val ramAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ramDisplay)
        binding.spnRam.adapter = ramAdapter

        val storageDisplay = storageOptions.map { "$it GB" }
        val storageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, storageDisplay)
        binding.spnStorage.adapter = storageAdapter

        binding.sbCondition.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvCondition.text = "Kondisi Fisik: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun predictPrice() {
        if (tflite == null) {
            Toast.makeText(this, "Model belum siap...", Toast.LENGTH_SHORT).show()
            return
        }

        val ageText = binding.etAge.text.toString()
        if (ageText.isEmpty()) {
            Toast.makeText(this, "Isi lama pemakaian!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 1. Ambil Data untuk Model TFLite (Angka)
            val brandPos = binding.spnBrand.selectedItemPosition
            val brandIndex = brandPos.toFloat()
            val ramValue = ramOptions[binding.spnRam.selectedItemPosition].toFloat()
            val storageValue = storageOptions[binding.spnStorage.selectedItemPosition].toFloat()
            val age = ageText.toFloat()
            val condition = binding.sbCondition.progress.toFloat()

            // 2. Jalankan Prediksi
            val input = floatArrayOf(brandIndex, ramValue, storageValue, age, condition)
            val output = Array(1) { FloatArray(1) }
            val inputBuffer = ByteBuffer.allocateDirect(4 * 5).apply {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().put(input)
            }

            tflite?.run(inputBuffer, output)

            // 3. Olah Hasil Harga
            val rawPrice = output[0][0] * 1_000_000
            val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            binding.tvResult.text = format.format(rawPrice)

            // --- TAMBAHAN FIREBASE: SIMPAN DATA KE DATABASE ---
            // Kita ambil nama merk (String) biar enak dibaca di database, bukan angkanya
            val brandName = brandOptions[brandPos]

            saveToFirebase(brandName, ramValue.toInt(), storageValue.toInt(), age, condition.toInt(), rawPrice.toDouble())

        } catch (ex: Exception) {
            Toast.makeText(this, "Gagal hitung: ${ex.message}", Toast.LENGTH_SHORT).show()
            ex.printStackTrace()
        }
    }

    // --- Fungsi Khusus Menyimpan ke Firebase ---
    private fun saveToFirebase(brand: String, ram: Int, storage: Int, age: Float, condition: Int, price: Double) {
        // 1. Buat ID unik (Primary Key) otomatis
        val id = database.push().key

        if (id != null) {
            // 2. Masukkan data ke dalam Data Class yang kita buat di Langkah 3
            val history = PredictionHistory(id, brand, ram, storage, age, condition, price)

            // 3. Kirim ke Firebase
            database.child(id).setValue(history).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Data tersimpan di Firebase!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Gagal simpan: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}