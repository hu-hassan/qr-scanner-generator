package com.tenmillionapps.qrscanner.views.adapters

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tenmillionapps.qrscanner.R
import com.tenmillionapps.qrscanner.databinding.ItemSettingsBinding
import com.tenmillionapps.qrscanner.models.SettingsModel


class SettingsAdapter(
    var list: ArrayList<SettingsModel>,
    var context: Context
) : RecyclerView.Adapter<SettingsAdapter.viewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        return viewHolder(ItemSettingsBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: viewHolder, position: Int) {
        holder.bind(model = list[position], position)
    }

    override fun getItemCount() = list.size



    inner class viewHolder(var binding: ItemSettingsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: SettingsModel, position: Int) {
            lateinit var sharedPreferences: SharedPreferences
            binding.apply {
                sharedPreferences = context.getSharedPreferences("scanner_preferences", Context.MODE_PRIVATE)

                // Handle binding
                settingsTitle.text = model.title
                settingsDesc.text = model.desc
                if (model.title == "Vibrate"||model.title == "Sound"){
                    customSwitch.visibility = View.VISIBLE
                }
                else{
                    customSwitch.visibility = View.GONE
                }
                binding.customSwitch.setOnCheckedChangeListener { _, isChecked ->
                    model.isChecked = isChecked
                }
                if (model.title == "Vibrate"){
                    customSwitch.isChecked = sharedPreferences.getBoolean("vibrate", false)
                }
                else if (model.title == "Sound"){
                    customSwitch.isChecked = sharedPreferences.getBoolean("sound", false)
                }
                root.setOnClickListener {
                    when(position){
                        0->{
                            // Handle click
                        }
                    }
                }

            }
        }
    }


}