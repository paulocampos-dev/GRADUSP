package com.prototype.gradusp.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "AdViewModel"

@HiltViewModel
class AdViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var rewardedAd: RewardedAd? = null
    private val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // Test Ad Unit ID

    private val _customColorClickCount = MutableStateFlow(0)
    val customColorClickCount: StateFlow<Int> = _customColorClickCount.asStateFlow()

    private val _adsEnabled = MutableStateFlow(true)
    val adsEnabled: StateFlow<Boolean> = _adsEnabled.asStateFlow()

    init {
        loadRewardedAd(context)
    }

    fun incrementCustomColorClick() {
        _customColorClickCount.value++
    }

    fun toggleAdsEnabled(enabled: Boolean) {
        _adsEnabled.value = enabled
    }

    fun loadRewardedAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Rewarded ad failed to load: ${adError.message}")
                    rewardedAd = null
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad was loaded.")
                    rewardedAd = ad
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onAdDismissed: () -> Unit, onUserEarnedReward: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "Ad dismissed fullscreen content.")
                    rewardedAd = null
                    onAdDismissed()
                    loadRewardedAd(context) // Load a new ad after dismissal
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "Ad failed to show fullscreen content: ${adError.message}")
                    rewardedAd = null
                    onAdDismissed()
                    loadRewardedAd(context) // Load a new ad even if it failed to show
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "Ad showed fullscreen content.")
                }
            }
            rewardedAd?.show(activity) { rewardItem ->
                // Handle the reward.
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                Log.d(TAG, "User earned the reward: $rewardAmount $rewardType")
                onUserEarnedReward()
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            onAdDismissed() // Dismiss immediately if ad not ready
            loadRewardedAd(context) // Try to load again
        }
    }
}
