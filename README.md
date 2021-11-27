# SimpleImagePicker
![device-2021-11-27-185548](https://user-images.githubusercontent.com/22664709/143690118-f67dc40d-3e9c-4ca8-9d1a-f828db3cde24.png)


1) add camera and files permissions to manifest file
```
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```
2) add Files Prioder in manifest file inside application tags

```
 <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.ronnie_image_provider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/image_provider_path" />
</provider>
        
```        
3) create **image_provider_path** xml file 

```
<paths>
    <external-cache-path
        name="image"
        path="/" />
</paths>
```
4) create **ImagePicker** class
```
import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class ImagePicker {
    private var galleryLauncher: ActivityResultLauncher<Intent>? = null
    private var cameraLauncher: ActivityResultLauncher<Uri>? = null
    private var storagePermission: ActivityResultLauncher<String>? = null
    private var cameraPermission: ActivityResultLauncher<String>? = null
    private var activity: AppCompatActivity? = null
    private var fragment: Fragment? = null
    private var context: Context?
    val _bitmapLivedata = MutableLiveData<Bitmap>()
    private var takenImageUri: Uri?=null
    private var callback: ((imageResult: ImageResult<Uri>) -> Unit)? = null

    constructor(activity: AppCompatActivity) {
        this.activity = activity
        context = activity.applicationContext
        registerActivityForResults()
    }

    constructor(fragment: Fragment) {
        this.fragment = fragment
        context = fragment.context
        registerActivityForResults()
    }

    private fun registerActivityForResults() {
        //Camera permission
        cameraPermission = (activity?: fragment)?.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            when {
                granted -> {
                    launchCamera()
                }
                else -> callback?.invoke(ImageResult.Failure("Camera Permission denied"))

            }
        }

        //Storage permission
        storagePermission = (activity ?: fragment)?.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            when {
                granted -> {
                    launchGallery()
                }

                else -> callback?.invoke(ImageResult.Failure("Storage Permission denied"))

            }
        }
        //Launch camera
        cameraLauncher =
            (activity ?: fragment)?.registerForActivityResult(
                ActivityResultContracts.TakePicture()
            ) { result ->
                if (result) {
                    callback?.invoke(ImageResult.Success(takenImageUri))
                } else {
                    callback?.invoke(ImageResult.Failure("Camera Launch Failed"))
                }
            }

        //launch gallery
        galleryLauncher = (activity ?: fragment)?.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
                val uri = result.data?.data
                if (uri != null) {
                    callback?.invoke(ImageResult.Success(uri))
                } else {
                    callback?.invoke(ImageResult.Failure("Gallery Launch Failed"))
                }
            } else {
                callback?.invoke(ImageResult.Failure("Gallery Launch Failed"))
            }
        }
    }

    private fun launchCamera() {
        try {
            val takenImageFile =
                File(context?.externalCacheDir, "takenImage${(1..1000).random()}.jpg")
            takenImageUri = context?.let {
                FileProvider.getUriForFile(
                    it, context?.packageName.plus(".ronnie_image_provider"), takenImageFile
                )
            }
            cameraLauncher!!.launch(takenImageUri)
        } catch (exception: Exception) {
            callback?.invoke(ImageResult.Failure("Camera Launch Failed"))
        }
    }

    private fun launchGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        Intent.createChooser(intent, "Select Image")
        galleryLauncher?.launch(intent)
    }
    private fun pickFromStorage(callback: ((imageResult: ImageResult<Uri>) -> Unit)) {
        this.callback = callback
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                (activity ?: fragment!!.requireActivity()),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showWhyPermissionNeeded(Manifest.permission.READ_EXTERNAL_STORAGE, "Storage")
        } else {
            storagePermission?.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

   private fun takeFromCamera(callback: ((imageResult: ImageResult<Uri>) -> Unit)) {
        this.callback = callback
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                (activity ?: fragment!!.requireActivity()),
                Manifest.permission.CAMERA
            )
        ) {
            showWhyPermissionNeeded(Manifest.permission.CAMERA, "Camera")
        } else {
            cameraPermission?.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showWhyPermissionNeeded(permission: String, name: String) {
        MaterialAlertDialogBuilder(activity ?: fragment!!.requireContext())
            .setMessage("Permission needed. $name permission is required")
            .setPositiveButton(
                "Okay"
            ) { _, _ ->
                if (permission == Manifest.permission.CAMERA) {
                    cameraPermission?.launch(permission)
                } else {
                    storagePermission?.launch(permission)
                }

            }.create().show()
    }
    fun takeFromCamera() {
        takeFromCamera { imageResult ->
            when (imageResult) {
                is ImageResult.Success -> {
                    val uri = imageResult.value
                    getLargeBitmap(uri)
                }
                is ImageResult.Failure -> {
                    val errorString = imageResult.errorString
                    Toast.makeText(context, errorString, Toast.LENGTH_LONG).show()
                }
            }

        }
    }
    private fun getLargeBitmap(uri: Uri?) {

        (activity ?: fragment)?.let {
            (if (activity != null)
                Glide.with(activity!!)
            else if (fragment != null)
                Glide.with(fragment!!)
            else null
                    )?.asBitmap()?.override(900, 900)?.load(uri)
                ?.diskCacheStrategy(DiskCacheStrategy.NONE)?.into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap?>?
                    ) {
                        _bitmapLivedata.postValue(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }
    }
    fun pickFromStorage() {
        pickFromStorage { imageResult ->
            when (imageResult) {
                is ImageResult.Success -> {
                    val uri = imageResult.value
                    getLargeBitmap(uri)
                }
                is ImageResult.Failure -> {
                    val errorString = imageResult.errorString
                    Toast.makeText(context, errorString, Toast.LENGTH_LONG).show()
                }
            }

        }
    }
}
sealed class ImageResult <out T>{
    data class Success<out T>(val value: T?) : ImageResult<T>()
    data class Failure(val errorString: String): ImageResult<Nothing>()
}
```

------------------------------
## now you can create new instance of **ImagePicker** class and pass fragment or activity or constractor , add observe to bitmapLiveData and call **takeFromCamera** or **pickFromStorage** 
-----------
### Optional 
#### you can create your own dialog or bottom sheet fragment and add two buttons (Pick From camera or Gallery) ...
###### for example
```
import ImagePicker
import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChoosePickerButtonSheet() : BottomSheetDialogFragment(),
    View.OnClickListener {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var imagePicker: ImagePicker? = null
        fun getInstance(imagePicker: ImagePicker): ChoosePickerButtonSheet {
            this.imagePicker = imagePicker
            return ChoosePickerButtonSheet()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_picker_dialog, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setDimAmount(0.2f)

        setTransparentBackground()

        view.findViewById<TextView>(R.id.cancelTv).setOnClickListener(this)
        view.findViewById<TextView>(R.id.cameraTv).setOnClickListener(this)
        view.findViewById<TextView>(R.id.galleryTv).setOnClickListener(this)
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.galleryTv -> imagePicker?.pickFromStorage()
            R.id.cameraTv -> imagePicker?.takeFromCamera()
        }
        dismiss()
    }

    fun BottomSheetDialogFragment.setTransparentBackground() {
        dialog?.apply {
            setOnShowListener {
                val bottomSheet = findViewById<View?>(R.id.design_bottom_sheet)
                bottomSheet?.setBackgroundResource(android.R.color.transparent)
            }
        }
    }
}
```
### XML File
 ```
 <?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@android:color/transparent"
    android:orientation="vertical">

    <com.google.android.material.textview.MaterialTextView
        android:elevation="12dp"
        android:id="@+id/galleryTv"
        android:background="@drawable/shape_round_white"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="12dp"
        android:layout_marginStart="12dp"
        android:layout_marginTop="8dp"
        android:layout_marginEnd="12dp"
        android:gravity="center"
        android:text="Gallery"
        android:textColor="@color/black" />


    <com.google.android.material.textview.MaterialTextView
        android:background="@drawable/shape_round_white"
        android:id="@+id/cameraTv"
        android:layout_width="match_parent"
        android:elevation="12dp"
        android:layout_height="wrap_content"
        android:padding="12dp"
        android:gravity="center"
        android:layout_marginStart="12dp"
        android:layout_marginTop="8dp"
        android:layout_marginEnd="12dp"
        android:text="Camera"
        android:textColor="@color/black" />


    <com.google.android.material.textview.MaterialTextView
        android:background="@drawable/shape_round_white"
        android:id="@+id/cancelTv"
        android:elevation="12dp"
        android:layout_width="match_parent"
        android:layout_marginBottom="20sp"
        android:layout_height="wrap_content"
        android:padding="12dp"
        android:layout_marginStart="12dp"
        android:layout_marginTop="16dp"
        android:layout_marginEnd="12dp"
        android:gravity="center"
        android:text="Cancel"
        android:textColor="@color/black" />
</LinearLayout>
 ```

### drawable folder
```
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <corners android:radius="15dp" />
    <solid android:color="@color/white" />
</shape>
```

### activity or fragment 
```
 val imagePicker =  ImagePicker(this)
        imagePicker._bitmapLivedata.observe(this) {
            imageView.setImageBitmap(it)
        }
        val choosePickerButtonSheet = ChoosePickerButtonSheet.getInstance(imagePicker = imagePicker)
        imageView.setOnClickListener {
            choosePickerButtonSheet.show(supportFragmentManager, "tag")
        }
```


##### reference https://github.com/ronnieotieno/Ronnie-Image-Picker
