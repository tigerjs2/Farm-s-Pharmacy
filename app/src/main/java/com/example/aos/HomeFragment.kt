package com.example.aos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class HomeFragment : Fragment() {

    private lateinit var mAuth: FirebaseAuth

    // 전남대학교 고정 좌표 (추후 GPS 허용 시 대체)
    private val LAT = 35.1765
    private val LON = 126.9072

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        loadUserName(view)
        loadWeather(view)

        view.findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserName(requireView())
    }

    private fun loadUserName(view: View) {
        val uid = mAuth.currentUser?.uid ?: return
        val tvGreeting = view.findViewById<TextView>(R.id.tvGreeting)

        Firebase.firestore
            .collection("Users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: "농부"
                tvGreeting.text = "Hello, ${name}님!"
            }
            .addOnFailureListener {
                tvGreeting.text = "안녕하세요!"
            }
    }

    private fun loadWeather(view: View) {
        val tvTemp     = view.findViewById<TextView>(R.id.tvTemperature)
        val tvRain     = view.findViewById<TextView>(R.id.tvRain)
        val tvHumidity = view.findViewById<TextView>(R.id.tvHumidity)
        val tvWind     = view.findViewById<TextView>(R.id.tvWind)
        val ivIcon     = view.findViewById<ImageView>(R.id.ivWeatherIcon)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { fetchWeather() } ?: return@launch

            val temp     = result.getAsJsonObject("main")?.get("temp")?.asDouble?.toInt()
            val humidity = result.getAsJsonObject("main")?.get("humidity")?.asInt
            val wind     = result.getAsJsonObject("wind")?.get("speed")?.asDouble
            val rain     = result.getAsJsonObject("rain")?.get("1h")?.asDouble
                ?: result.getAsJsonObject("rain")?.get("3h")?.asDouble
                ?: 0.0
            val iconCode = result.getAsJsonArray("weather")
                ?.get(0)?.asJsonObject?.get("icon")?.asString

            tvTemp.text     = if (temp != null) "${temp}°" else "--°"
            tvHumidity.text = if (humidity != null) "${humidity}%" else "--%"
            tvWind.text     = if (wind != null) "${"%.1f".format(wind)}m/s" else "--m/s"
            tvRain.text     = "${"%.1f".format(rain)}mm"

            if (iconCode != null) {
                val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
                Glide.with(requireContext()).load(iconUrl).into(ivIcon)
            }
        }
    }

    private fun fetchWeather(): JsonObject? {
        return try {
            val apiKey = BuildConfig.OPENWEATHER_API_KEY
            val url = "https://api.openweathermap.org/data/2.5/weather" +
                    "?lat=$LAT&lon=$LON&appid=$apiKey&units=metric&lang=kr"
            val request = Request.Builder().url(url).build()
            val response = OkHttpClient().newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            com.google.gson.Gson().fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}