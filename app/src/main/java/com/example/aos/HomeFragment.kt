package com.example.aos

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import java.time.LocalDate

class HomeFragment : Fragment() {

    private lateinit var mAuth: FirebaseAuth

    @RequiresApi(Build.VERSION_CODES.O)
    private val today = LocalDate.now()

    @RequiresApi(Build.VERSION_CODES.O)
    private var selectedDate = today

    @RequiresApi(Build.VERSION_CODES.O)
    private var weekStartDates: List<LocalDate> = emptyList()

    @RequiresApi(Build.VERSION_CODES.O)
    private var dayStat: Map<LocalDate, CalendarDayStat> = emptyMap()

    @RequiresApi(Build.VERSION_CODES.O)
    private lateinit var homeCalendarAdapter: WeekCalendarAdapter

    private lateinit var homeCalendarLayoutManager: LinearLayoutManager
    private var currentWeekPageIndex: Int = RecyclerView.NO_POSITION

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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        loadUserName(view)
        loadWeather(view)
        setupHomeCalendar(view)

        view.findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        loadUserName(requireView())
        refreshHomeCalendar()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupHomeCalendar(view: View) {
        dayStat = CalendarHistoryStats.load(requireContext())

        val baseWeekStart = startOfWeek(today)
        weekStartDates = (-104..104).map { offset ->
            baseWeekStart.plusWeeks(offset.toLong())
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvHomeCalendar)

        homeCalendarAdapter = WeekCalendarAdapter(
            weekStartDates = weekStartDates,
            selectedDate = selectedDate,
            stats = dayStat
        ) { date ->
            selectHomeDate(date)
        }

        homeCalendarLayoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        recyclerView.apply {
            layoutManager = homeCalendarLayoutManager
            adapter = homeCalendarAdapter
            itemAnimator = null
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = false
            clipToPadding = false

            if (onFlingListener == null) {
                SingleWeekSnapHelper().attachToRecyclerView(this)
            }

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(rv, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        updateSelectedHomeDateFromSnappedWeek()
                    }
                }
            })
        }

        moveHomeCalendarToSelectedWeek(recyclerView)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun refreshHomeCalendar() {
        if (!::homeCalendarAdapter.isInitialized) return

        dayStat = CalendarHistoryStats.load(requireContext())
        homeCalendarAdapter.updateStats(dayStat)
        homeCalendarAdapter.setSelectedDate(selectedDate)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun moveHomeCalendarToSelectedWeek(recyclerView: RecyclerView) {
        val selectedWeekIndex = homeCalendarAdapter.weekIndexOf(selectedDate)
        if (selectedWeekIndex == -1) return

        currentWeekPageIndex = selectedWeekIndex
        recyclerView.post {
            homeCalendarLayoutManager.scrollToPositionWithOffset(selectedWeekIndex, 0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateSelectedHomeDateFromSnappedWeek() {
        val firstVisible = homeCalendarLayoutManager.findFirstCompletelyVisibleItemPosition()
        val pageIndex = if (firstVisible != RecyclerView.NO_POSITION) {
            firstVisible
        } else {
            homeCalendarLayoutManager.findFirstVisibleItemPosition()
        }

        if (pageIndex == RecyclerView.NO_POSITION || pageIndex == currentWeekPageIndex) return

        currentWeekPageIndex = pageIndex
        val dateInNewWeek = homeCalendarAdapter.dateAtSameWeekday(pageIndex, selectedDate)
        selectHomeDate(dateInNewWeek)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectHomeDate(date: LocalDate) {
        selectedDate = date
        homeCalendarAdapter.setSelectedDate(date)
        currentWeekPageIndex = homeCalendarAdapter.weekIndexOf(date)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startOfWeek(date: LocalDate): LocalDate {
        return date.minusDays(date.dayOfWeek.value.toLong() - 1L)
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
