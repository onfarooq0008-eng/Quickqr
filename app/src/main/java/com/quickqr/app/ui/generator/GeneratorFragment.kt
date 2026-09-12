package com.quickqr.app.ui.generator

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.quickqr.app.BuildConfig
import com.quickqr.app.databinding.FragmentGeneratorBinding
import com.quickqr.app.util.QRCodeGenerator
import java.io.File
import java.io.FileOutputStream

class GeneratorFragment : Fragment() {

    private var _binding: FragmentGeneratorBinding? = null
    private val binding get() = _binding!!
    private var currentBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeneratorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Chips pre-fill a starting template for that content type; the user can freely
        // edit it. This keeps one flexible text field instead of five separate forms.
        // Only applied when the field is empty, so switching chips never wipes out
        // something the user already typed.
        binding.chipUrl.setOnClickListener { fillTemplateIfEmpty("https://") }
        binding.chipWifi.setOnClickListener { fillTemplateIfEmpty("WIFI:S:NetworkName;T:WPA;P:Password;;") }
        binding.chipEmail.setOnClickListener { fillTemplateIfEmpty("mailto:someone@example.com") }
        binding.chipPhone.setOnClickListener { fillTemplateIfEmpty("tel:+1234567890") }

        binding.btnGenerate.setOnClickListener { generateCode() }
        binding.btnSave.setOnClickListener { saveToGallery() }
        binding.btnShare.setOnClickListener { shareCode() }
    }

    private fun fillTemplateIfEmpty(template: String) {
        if (binding.inputContent.text.isNullOrBlank()) {
            binding.inputContent.setText(template)
            binding.inputContent.setSelection(template.length)
        }
    }

    private fun generateCode() {
        val text = binding.inputContent.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            Toast.makeText(requireContext(), "Type or paste something first", Toast.LENGTH_SHORT).show()
            return
        }
        val bitmap = QRCodeGenerator.generate(text)
        if (bitmap == null) {
            Toast.makeText(requireContext(), "Couldn't generate a code for that text", Toast.LENGTH_SHORT).show()
            return
        }
        currentBitmap = bitmap
        binding.imageQr.setImageBitmap(bitmap)
        binding.resultGroup.visibility = View.VISIBLE
    }

    private fun saveToGallery() {
        val bitmap = currentBitmap ?: return
        try {
            val filename = "QuickQR_${System.currentTimeMillis()}.png"
            val resolver = requireContext().contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/QuickQR")
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                Toast.makeText(requireContext(), "Saved to Pictures/QuickQR", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Couldn't save image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Couldn't save image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCode() {
        val bitmap = currentBitmap ?: return
        try {
            val cacheDir = File(requireContext().cacheDir, "images").apply { mkdirs() }
            val file = File(cacheDir, "shared_qr.png")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            val uri = FileProvider.getUriForFile(requireContext(), "${BuildConfig.APPLICATION_ID}.fileprovider", file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share QR code"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Couldn't share image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
