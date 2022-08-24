package com.example.blescanner

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.example.blescanner.databinding.MeshFragmentBinding

class MeshDialogFragment : DialogFragment() {

    private var _binding: MeshFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding =
            MeshFragmentBinding.inflate(LayoutInflater.from(context))


        return AlertDialog.Builder(
            requireActivity()
        ).setView(binding.root).create()
    }


}