package net.gini.android.bank.sdk.exampleapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.databinding.FragmentComposeBinding
import net.gini.android.capture.ui.theme.GiniTheme

abstract class ComposeFragment : Fragment(R.layout.fragment_compose) {

    private lateinit var binding: FragmentComposeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentComposeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.composeView.setContent {
            GiniTheme {
                ScreenContent()
            }
        }
    }

    @Composable
    abstract fun ScreenContent()
}
