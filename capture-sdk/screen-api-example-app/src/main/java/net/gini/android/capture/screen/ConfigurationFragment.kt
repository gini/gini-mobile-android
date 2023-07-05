package net.gini.android.capture.screen

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import net.gini.android.capture.screen.databinding.FragmentConfigurationBinding

class ConfigurationFragment : Fragment(R.layout.fragment_configuration) {

    private lateinit var binding: FragmentConfigurationBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentConfigurationBinding.inflate(layoutInflater)


    }

}