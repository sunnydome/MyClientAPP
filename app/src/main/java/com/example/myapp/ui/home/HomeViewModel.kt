package com.example.myapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private val feedRepository = FeedRepository()

    fun getFeeds(): LiveData<List<FeedModel>> {
        return feedRepository.fetchFeeds()
    }
}