package com.example.stt.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    val spokenText = MutableLiveData<String>()
}
