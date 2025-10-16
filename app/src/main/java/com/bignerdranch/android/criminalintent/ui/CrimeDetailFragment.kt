package com.bignerdranch.android.criminalintent.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.criminalintent.Crime
import com.bignerdranch.android.criminalintent.R
import com.bignerdranch.android.criminalintent.viewmodel.CrimeDetailViewModel
import com.bignerdranch.android.criminalintent.viewmodel.CrimeDetailViewModelFactory
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeDetailBinding
import com.bignerdranch.android.criminalintent.getScaledBitmap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

private const val DATE_FORMAT = "EEE, MMM, dd"
private const val TIME_FORMAT = "hh:mm a"
private const val TAG = "CrimeDetailFragment"

class CrimeDetailFragment : Fragment() {
    private var _binding: FragmentCrimeDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val args: CrimeDetailFragmentArgs by navArgs()
    private val crimeDetailViewModel: CrimeDetailViewModel by viewModels {
        CrimeDetailViewModelFactory(args.crimeID)
    }

    //GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    @SuppressLint("MissingPermission")
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getUpdatedGps()
        } else {
            Snackbar.make(
                binding.root, "Cannot update GPS.",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private val selectSuspect = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { parseContactSelection(it) }
    }

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto: Boolean ->
        if (didTakePhoto && photoName != null) {
            crimeDetailViewModel.updateCrime { oldCrime ->
                oldCrime.copy(photoFileName = photoName)
            }
        }
    }

    private var photoName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)
        //GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentCrimeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val callback = object  : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentCrime = crimeDetailViewModel.crime.value

                if (currentCrime?.title.isNullOrBlank()) {
                    Snackbar.make(
                        binding.root,
                        "Please enter a title.",
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    isEnabled = false
                    findNavController().popBackStack()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        binding.apply {
            crimeTitle.doOnTextChanged { text, _, _, _ ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(title = text.toString())
                }
            }

            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(isSolved = isChecked)
                }
            }

            crimeSuspect.setOnClickListener {
                selectSuspect.launch(null)
            }

            val selectSuspectIntent = selectSuspect.contract.createIntent(
                requireContext(),
                null
            )
            crimeSuspect.isEnabled = canResolveIntent(selectSuspectIntent)

            crimeCamera.setOnClickListener {
                photoName = "IMG_${Date()}.JPG"

                val nonNullPhotoName = photoName!!

                val photoFile = File(
                    requireContext().applicationContext.filesDir,
                    nonNullPhotoName
                )

                val photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.bignerdranch.android.criminalintent.fileprovider",
                    photoFile
                )
                takePhoto.launch(photoUri)
            }

            val captureImageIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            crimeCamera.isEnabled = canResolveIntent(captureImageIntent)

            //GPS Button
            updateGps.setOnClickListener {
                checkLocationPermissionAndGetGps()
            }
            updateGps.isEnabled = true
//            updateGps.isEnabled = false
//            if (isLocationPermissionGranted()) {
//                updateGps.isEnabled = true
//            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                crimeDetailViewModel.crime.collect { crime ->
                    crime?.let { updateUi(it) }
                }
            }
        }

        setFragmentResultListener(DatePickerFragment.REQUEST_KEY_DATE) { _, bundle ->
            val newDate
//            = bundle.getSerializable(DatePickerFragment.BUNDLE_KEY_DATE) as Date //is Deprecated
            : Date = bundle.getSerializable(
                DatePickerFragment.BUNDLE_KEY_DATE,
                Date::class.java
            ) ?: Date()
            crimeDetailViewModel.updateCrime { it.copy(date = newDate) }
        }

        setFragmentResultListener(TimePickerFragment.REQUEST_KEY_TIME) { _, bundle ->
            val newDate: Date = bundle.getSerializable(
                TimePickerFragment.BUNDLE_KEY_TIME,
                Date::class.java
            ) ?: Date()
            crimeDetailViewModel.updateCrime { it.copy(date = newDate) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        @Suppress("DEPRECATION")
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_detail, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        @Suppress("DEPRECATION")
        return when (item.itemId) {
            R.id.delete_crime -> {
                deleteCurrentCrime()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteCurrentCrime() {
        viewLifecycleOwner.lifecycleScope.launch {
            crimeDetailViewModel.crime.value?.let { crime ->
                crimeDetailViewModel.deleteCrime(crime)
            }
            findNavController().popBackStack()
        }
    }

    //GPS checks
    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun checkLocationPermissionAndGetGps() {
        if (isLocationPermissionGranted()) {
            getUpdatedGps()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @Suppress("MissingPermission")
    private fun getUpdatedGps() {
        Log.d(TAG, "Attempting to get updated GPS location...") // <--- LOG 1
        try {

            @Suppress("DEPRECATION")
            fusedLocationClient.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location: Location? ->
                Log.d(TAG, "Location success listener triggered.") // <--- LOG 2
                if (location != null) {
                    Log.d(TAG, "Location received: Lat=${location.latitude}, Lon=${location.longitude}") // <--- LOG 3
                    crimeDetailViewModel.updateCrime { oldCrime ->
                        oldCrime.copy(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    }
                } else {
                    Log.d(TAG, "Location received was NULL.") // <--- LOG 4
                    Snackbar.make(
                        binding.root,
                        "Ensure GPS is enabled.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            binding.updateGps.isEnabled = true



        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Location permission missing or disabled.", e) // <--- LOG 6
            Snackbar.make(
                binding.root,
                "Location permission Required. Check Settings.",
                Snackbar.LENGTH_LONG
            ).show()
            binding.updateGps.isEnabled = false
        }
    }

    private fun updateUi(crime: Crime) {
        binding.apply {
            if (crimeTitle.text.toString() != crime.title) {
                crimeTitle.setText(crime.title)
            }
            // Date button
            crimeDate.text = DateFormat.format(DATE_FORMAT, crime.date).toString()
            crimeDate.setOnClickListener {
                findNavController().navigate(
                    CrimeDetailFragmentDirections.selectDate(crime.date)
                )
            }
            // time button
            crimeTime.text = DateFormat.format(TIME_FORMAT, crime.date).toString()
            crimeTime.setOnClickListener{
                findNavController().navigate(
                    CrimeDetailFragmentDirections.selectTime(crime.date)
                )
            }
            crimeSolved.isChecked = crime.isSolved

            crimeReport.setOnClickListener {
                val reportIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getCrimeReport(crime))
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject)
                    )
                }

                val chooserIntent = Intent.createChooser(
                    reportIntent,
                    getString(R.string.send_report)
                )
                startActivity(chooserIntent)
            }

            crimeSuspect.text = crime.suspect.ifEmpty {
                getString(R.string.crime_suspect_text)
            }

            //GPS update display
            latitude.text = getString(R.string.latitude_label, String.format(java.util.Locale.US,"%.4f", crime.latitude))
            longitude.text = getString(R.string.longitude_label, String.format(java.util.Locale.US,"%.4f", crime.longitude))
            //updateGps.isEnabled = isLocationPermissionGranted()
            Log.d(TAG, "LOG7 updateUi: Displaying Lat=${String.format("%.4f", crime.latitude)}, Lon=${String.format("%.4f", crime.longitude)}")


            updatePhoto(crime.photoFileName)
        }
    }

    private fun getCrimeReport(crime: Crime): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)

        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        val suspectText = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(
            R.string.crime_report,
            crime.title, dateString, solvedString, suspectText
        )
    }

    private fun parseContactSelection(contactUri: Uri) {
        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)

        val queryCursor = requireActivity().contentResolver
            .query(contactUri, queryFields, null, null, null)
        queryCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val suspect = cursor.getString(0)
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(suspect = suspect)
                }
            }
        }
    }

    private fun canResolveIntent(intent: Intent): Boolean {
        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? =
            packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolvedActivity != null
    }

    private fun updatePhoto(photoFileName: String?) {
        if (binding.crimePhoto.tag != photoFileName) {
            val photoFile = photoFileName?.let {
                File(requireContext().applicationContext.filesDir, it)
            }

            if (photoFile?.exists() == true) {
                binding.crimePhoto.doOnLayout { measuredView ->
                    val scaledBitmap = getScaledBitmap(
                        photoFile.path,
                        measuredView.width,
                        measuredView.height
                    )
                    binding.crimePhoto.setImageBitmap(scaledBitmap)
                    binding.crimePhoto.tag = photoFileName
                }

                //Challenge: Detail Display
                binding.crimePhoto.isClickable = true
                binding.crimePhoto.setOnClickListener {
                    findNavController().navigate(
                        CrimeDetailFragmentDirections.showPhotoDetail(photoFile.path)
                    )
                }

            } else {
                binding.crimePhoto.setImageBitmap(null)
                binding.crimePhoto.tag = null

                //Challenge: Detail Display
                binding.crimePhoto.isClickable = false
                binding.crimePhoto.setOnClickListener(null)
            }
        }
    }

}