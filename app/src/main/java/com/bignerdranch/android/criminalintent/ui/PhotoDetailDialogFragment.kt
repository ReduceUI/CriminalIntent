package com.bignerdranch.android.criminalintent.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.criminalintent.databinding.FragmentPhotoDetailBinding
import com.bignerdranch.android.criminalintent.getScaledBitmap

class PhotoDetailDialogFragment : DialogFragment() {

    private var _binding: FragmentPhotoDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val args: PhotoDetailDialogFragmentArgs by navArgs()

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        // Optional: Set a specific background color (like black) for a photo viewer
        // This is often needed to hide the underlying view completely.
        //dialog?.window?.setBackgroundDrawableResource(android.R.color.black)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoFilePath = args.photoFilePath

        binding.photoDetailImageView.doOnLayout { measuredView ->

            val bitmap = getScaledBitmap(
                photoFilePath,
                measuredView.width,
                measuredView.height
            )

            binding.photoDetailImageView.setImageBitmap(bitmap)
        }

        binding.photoDetailImageView.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}