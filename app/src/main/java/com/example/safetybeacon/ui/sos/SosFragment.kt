package com.example.safetybeacon.ui.sos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.safetybeacon.databinding.FragmentSosBinding
import com.example.safetybeacon.ui.MagnetometerActivity
import com.example.safetybeacon.ui.SmsActivity

class SosFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(SosViewModel::class.java)

        _binding = FragmentSosBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        startActivity(Intent(requireContext(), SmsActivity::class.java))
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}