package com.tenmillionapps.qrscanner.views.fragments

import android.R
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.tenmillionapps.qrscanner.databinding.FragmentSettingsBinding
import com.tenmillionapps.qrscanner.models.SettingsModel
import com.tenmillionapps.qrscanner.views.adapters.SettingsAdapter


class FragmentSettings : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val list = ArrayList<SettingsModel>()
    private val adapter by lazy {
        SettingsAdapter(list, this.requireContext())
    }
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingsRecyclerView.adapter = adapter
        sharedPreferences = requireContext().getSharedPreferences("scanner_preferences", Context.MODE_PRIVATE)
        populateSettingsList()

    }

    private fun populateSettingsList() {
        list.add(SettingsModel(title = "Vibrate", desc = "Make vibration when something is scanned"))
        list.add(SettingsModel(title = "Sound", desc = "Make sound when something is scanned"))
        list.add(SettingsModel(title = "Privacy Policy", desc = "View our privacy policy"))
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        Log.d("FragmentSettings", "onPause")
        checkSwitchStates()
    }
    private fun checkSwitchStates() {
        val editor = sharedPreferences.edit()
        val vibrateSwitch = list.find { it.title == "Vibrate" }?.isChecked ?: false
        val soundSwitch = list.find { it.title == "Sound" }?.isChecked ?: false
        editor.putBoolean("vibrate", vibrateSwitch)
        editor.putBoolean("sound", soundSwitch)
        editor.apply()

        if (vibrateSwitch) {
            // Perform action when Vibrate switch is on
            Log.d("FragmentSettings", "Vibrate switch is on")
        }

        if (soundSwitch) {
            // Perform action when Sound switch is on
            Log.d("FragmentSettings", "Sound switch is on")
        }
    }
}