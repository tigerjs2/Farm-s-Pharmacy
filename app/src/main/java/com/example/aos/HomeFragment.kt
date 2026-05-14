package com.example.aos

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.SetOptions
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
    private var currentHomeTodo: TodoItem? = null
    private var currentSafetyGuide: ToolGuide? = null

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
        setupHomeTodoCard(view)
        loadSelectedDateTodo(view)
        setupTodaySafety(view)

        view.findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        loadUserName(requireView())
        refreshHomeCalendar()
        loadSelectedDateTodo(requireView())
        loadTodaySafety(requireView())
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

        val root = view ?: return
        loadSelectedDateTodo(root)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startOfWeek(date: LocalDate): LocalDate {
        return date.minusDays(date.dayOfWeek.value.toLong() - 1L)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupHomeTodoCard(view: View) {
        view.findViewById<ImageView>(R.id.btnHomeTodoCheck).setOnClickListener {
            val todo = currentHomeTodo ?: return@setOnClickListener
            if (todo.id.isEmpty()) return@setOnClickListener

            val checkButton = view.findViewById<ImageView>(R.id.btnHomeTodoCheck)
            checkButton.isEnabled = false

            Firebase.firestore
                .collection("Todos")
                .document(todo.id)
                .set(
                    mapOf(
                        "isDone" to true,
                        "done" to true
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener {
                    todo.isDone = true
                    currentHomeTodo = null
                    loadSelectedDateTodo(view)
                }
                .addOnFailureListener {
                    checkButton.isEnabled = true
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadSelectedDateTodo(view: View) {
        updateHomeFarmTitle(view)
        renderHomeTodoLoading(view)

        val uid = mAuth.currentUser?.uid
        if (uid == null) {
            renderHomeTodoEmpty(
                view,
                title = "로그인이 필요해요",
                memo = "할 일을 불러올 수 없습니다."
            )
            return
        }

        val requestDate = selectedDate
        val dateStr = requestDate.toString()

        Firebase.firestore
            .collection("Todos")
            .whereEqualTo("userId", uid)
            .whereEqualTo("date", dateStr)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded || selectedDate != requestDate) return@addOnSuccessListener

                val firstUndoneTodo = snapshot.documents
                    .mapNotNull { doc -> doc.toTodoItemCompat() }
                    .filter { !it.isDone }
                    .sortedByDescending { it.id }
                    .firstOrNull()

                renderHomeTodo(view, firstUndoneTodo)
            }
            .addOnFailureListener {
                if (!isAdded || selectedDate != requestDate) return@addOnFailureListener

                renderHomeTodoEmpty(
                    view,
                    title = "할 일을 불러오지 못했어요",
                    memo = "잠시 후 다시 시도해주세요."
                )
            }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toTodoItemCompat(): TodoItem? {
        val title = getString("title") ?: return null

        return TodoItem(
            id = id,
            userId = getString("userId") ?: getString("uid") ?: "",
            date = getString("date") ?: "",
            title = title,
            memo = getString("memo") ?: "",
            isDone = getBoolean("isDone") ?: getBoolean("done") ?: false
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateHomeFarmTitle(view: View) {
        val titleView = view.findViewById<TextView>(R.id.tvHomeFarmTitle)

        titleView.text = if (selectedDate == today) {
            "오늘의 농사"
        } else {
            "${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일 농사"
        }
    }

    private fun renderHomeTodoLoading(view: View) {
        currentHomeTodo = null

        view.findViewById<TextView>(R.id.tvHomeTodoTitle).text = "할 일을 불러오는 중..."
        view.findViewById<TextView>(R.id.tvHomeTodoMemo).text = ""

        view.findViewById<ImageView>(R.id.btnHomeTodoCheck).apply {
            setImageResource(R.drawable.ic_check_outline)
            alpha = 0.35f
            isEnabled = false
        }
    }

    private fun renderHomeTodo(view: View, todo: TodoItem?) {
        if (todo == null) {
            renderHomeTodoEmpty(
                view,
                title = "처리할 일이 없어요",
                memo = "선택한 날짜의 To do를 모두 완료했어요."
            )
            return
        }

        currentHomeTodo = todo

        view.findViewById<TextView>(R.id.tvHomeTodoTitle).text = todo.title
        view.findViewById<TextView>(R.id.tvHomeTodoMemo).text =
            todo.memo.ifBlank { "메모 없음" }

        view.findViewById<ImageView>(R.id.btnHomeTodoCheck).apply {
            setImageResource(R.drawable.ic_check_outline)
            alpha = 1f
            isEnabled = true
        }
    }

    private fun renderHomeTodoEmpty(view: View, title: String, memo: String) {
        currentHomeTodo = null

        view.findViewById<TextView>(R.id.tvHomeTodoTitle).text = title
        view.findViewById<TextView>(R.id.tvHomeTodoMemo).text = memo

        view.findViewById<ImageView>(R.id.btnHomeTodoCheck).apply {
            setImageResource(R.drawable.ic_check)
            alpha = 0.35f
            isEnabled = false
        }
    }


    private fun setupTodaySafety(view: View) {
        view.findViewById<View>(R.id.homeSafetyCard).setOnClickListener {
            openCurrentSafetyGuide()
        }

        loadTodaySafety(view)
    }

    private fun loadTodaySafety(view: View) {
        renderTodaySafetyLoading(view)

        val appContext = requireContext().applicationContext
        val requestedRoot = view

        lifecycleScope.launch {
            val guide = withContext(Dispatchers.IO) {
                TodaySafetyRepository.getTodaySafety(appContext)
            }

            if (!isAdded || this@HomeFragment.view !== requestedRoot) return@launch

            if (guide == null) {
                renderTodaySafetyEmpty(requestedRoot)
            } else {
                renderTodaySafety(requestedRoot, guide)
            }
        }
    }

    private fun renderTodaySafetyLoading(view: View) {
        currentSafetyGuide = null

        view.findViewById<TextView>(R.id.tvHomeSafetyTitle).text = "안전 지침을 불러오는 중..."
        view.findViewById<ImageView>(R.id.ivHomeSafetyImage).apply {
            visibility = View.GONE
            setImageDrawable(null)
        }
        view.findViewById<LinearLayout>(R.id.homeSafetyBullets).removeAllViews()
    }

    private fun renderTodaySafetyEmpty(view: View) {
        currentSafetyGuide = null

        view.findViewById<TextView>(R.id.tvHomeSafetyTitle).text = "안전 지침을 불러오지 못했어요"
        view.findViewById<ImageView>(R.id.ivHomeSafetyImage).apply {
            visibility = View.GONE
            setImageDrawable(null)
        }

        renderSafetyBullets(
            view.findViewById(R.id.homeSafetyBullets),
            listOf("잠시 후 다시 시도해주세요.")
        )

    }

    private fun renderTodaySafety(view: View, guide: ToolGuide) {
        currentSafetyGuide = guide

        view.findViewById<TextView>(R.id.tvHomeSafetyTitle).text =
            guide.title.ifBlank { guide.safeacdntSeNm.ifBlank { "오늘의 안전 지침" } }

        val image = view.findViewById<ImageView>(R.id.ivHomeSafetyImage)
        if (guide.imageUrl.isNotBlank()) {
            image.visibility = View.VISIBLE
            Glide.with(this).load(guide.imageUrl).fitCenter().into(image)
        } else {
            image.visibility = View.GONE
            image.setImageDrawable(null)
        }

        val bullets = TodaySafetyRepository
            .splitBullets(guide.content)
            .take(3)
            .ifEmpty { listOf("카드를 누르면 자세한 안전 지침을 확인할 수 있어요.") }

        renderSafetyBullets(view.findViewById(R.id.homeSafetyBullets), bullets)
    }

    private fun renderSafetyBullets(container: LinearLayout, bullets: List<String>) {
        container.removeAllViews()

        bullets.forEach { text ->
            val bullet = TextView(requireContext()).apply {
                this.text = "• $text"
                textSize = 10f
                setTextColor(0xFF000000.toInt())
                typeface = ResourcesCompat.getFont(requireContext(), R.font.paperlogy_4regular)
                setPadding(0, 0, 0, 6.dpToPx())
            }
            container.addView(bullet)
        }
    }

    private fun openCurrentSafetyGuide() {
        val guide = currentSafetyGuide ?: return

        val intent = Intent(requireContext(), ToolGuideDetailActivity::class.java).apply {
            putExtra(ToolGuideDetailActivity.EXTRA_TOOL_NAME, TodaySafetyRepository.toolNameOf(guide))
            putExtra(ToolGuideDetailActivity.EXTRA_FOCUS_CNTNTS_NO, guide.cntntsNo)
            putExtra(ToolGuideDetailActivity.EXTRA_FALLBACK_CNTNTS_NO, guide.cntntsNo)
            putExtra(ToolGuideDetailActivity.EXTRA_FALLBACK_TITLE, guide.title)
            putExtra(ToolGuideDetailActivity.EXTRA_FALLBACK_CONTENT, guide.content)
            putExtra(ToolGuideDetailActivity.EXTRA_FALLBACK_KNMC_NM, guide.knmcNm)
            putExtra(ToolGuideDetailActivity.EXTRA_FALLBACK_SAFEACDNT_SE_NM, guide.safeacdntSeNm)
            putExtra(ToolGuideDetailActivity.EXTRA_FALLBACK_IMAGE_URL, guide.imageUrl)
        }

        startActivity(intent)
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density + 0.5f).toInt()

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
