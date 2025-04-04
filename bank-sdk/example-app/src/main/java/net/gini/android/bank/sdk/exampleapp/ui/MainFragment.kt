package net.gini.android.bank.sdk.exampleapp.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_CANCELED
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.databinding.FragmentMainBinding
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity.Companion.CAMERA_PERMISSION_BUNDLE
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity.Companion.CONFIGURATION_BUNDLE
import net.gini.android.bank.sdk.exampleapp.ui.data.Configuration
import net.gini.android.capture.EntryPoint

@AndroidEntryPoint
class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private var configurationActivityLauncher: ActivityResultLauncher<Intent>? = null
    private val configurationViewModel: ConfigurationViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showVersions()
        setupActivityResultLauncher()
        addInputHandlers()

    }


    private fun setupActivityResultLauncher() {
        configurationActivityLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                when (result.resultCode) {
                    RESULT_CANCELED -> {}
                    RESULT_OK -> {
                        val configurationResult: Configuration? = result.data?.getParcelableExtra(
                            CONFIGURATION_BUNDLE
                        )
                        if (configurationResult != null) {
                            configurationViewModel.setConfiguration(configurationResult)
                        }

                        configurationViewModel.disableCameraPermission(
                            result.data?.getBooleanExtra(
                                CAMERA_PERMISSION_BUNDLE, false
                            ) ?: false
                        )
                    }
                }
            }
    }

    private fun addInputHandlers() {
        binding.tilFieldEntryPoint.setEndIconOnClickListener {
            findNavController().navigate(
                MainFragmentDirections.actionMainFragmentToClientBankSDKFragment(EntryPoint.FIELD)
            )
        }

        binding.buttonStartScanner.setOnClickListener {
            findNavController().navigate(
                MainFragmentDirections.actionMainFragmentToClientBankSDKFragment(EntryPoint.BUTTON)
            )
        }

        binding.textGiniBankVersion.setOnClickListener {
            configurationActivityLauncher?.launch(
                Intent(requireActivity(), ConfigurationActivity::class.java)
                    .putExtra(
                        CONFIGURATION_BUNDLE,
                        configurationViewModel.configurationFlow.value
                    )
                    .putExtra(
                        CAMERA_PERMISSION_BUNDLE,
                        configurationViewModel.disableCameraPermissionFlow.value
                    )
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showVersions() {
        binding.textGiniBankVersion.text =
            getString(R.string.gini_bank_sdk_version) + net.gini.android.bank.sdk.BuildConfig.VERSION_NAME +
                    getString(R.string.gini_capture_sdk_version) + net.gini.android.capture.BuildConfig.VERSION_NAME +
                    getString(R.string.gini_client_id) + getString(R.string.gini_api_client_id)
    }


}