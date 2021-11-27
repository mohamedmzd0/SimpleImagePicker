package com.simpleimagepicker.app

import ImagePicker
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private val imagePicker by lazy {
        ImagePicker(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.imageView)

        imagePicker._bitmapLivedata.observe(this) {
            imageView.setImageBitmap(it)
        }
        val choosePickerButtonSheet = ChoosePickerButtonSheet.getInstance(imagePicker = imagePicker)
        imageView.setOnClickListener {
            choosePickerButtonSheet.show(supportFragmentManager, "tag")
        }

    }
}