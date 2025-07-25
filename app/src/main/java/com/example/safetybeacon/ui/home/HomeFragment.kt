package com.example.safetybeacon.ui.home

import android.content.Intent
import android.os.Bundle
import android.provider.Telephony.Sms
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.safetybeacon.databinding.FragmentHomeBinding
import com.example.safetybeacon.ui.MagnetometerActivity
import com.example.safetybeacon.ui.SmsActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.title
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        //This helps in updating the data whenever it changes in the view model.
        binding.spyClickableLayout.setOnClickListener {
            val intent = Intent(activity, MagnetometerActivity::class.java)
            startActivity(intent)
        }
        binding.sosClickableLayout.setOnClickListener {
            val intent = Intent(activity, SmsActivity::class.java)
            startActivity(intent)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

