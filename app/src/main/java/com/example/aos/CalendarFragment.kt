package com.example.aos

import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aos.databinding.FragmentCalendarBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val gson = Gson()

    @RequiresApi(Build.VERSION_CODES.O)
    private val today = LocalDate.now()

    @RequiresApi(Build.VERSION_CODES.O)
    private var selectedDate = today

    @RequiresApi(Build.VERSION_CODES.O)
    private var weekStartDates: List<LocalDate> = emptyList()

    @RequiresApi(Build.VERSION_CODES.O)
    private var dayStat: Map<LocalDate, CalendarDayStat> = emptyMap()

    @RequiresApi(Build.VERSION_CODES.O)
    private lateinit var calendarAdapter: WeekCalendarAdapter

    private lateinit var calendarLayoutManager: LinearLayoutManager
    private var currentWeekPageIndex: Int = RecyclerView.NO_POSITION

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadStats()
        setupCalendar()
        updateMonthTitle()
        updateDiseaseCard(selectedDate)
        setupTodoList()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()

        if (_binding != null) {
            loadStats()
            if (::calendarAdapter.isInitialized) {
                calendarAdapter.updateStats(dayStat)
                calendarAdapter.setSelectedDate(selectedDate)
            }
            updateMonthTitle()
            updateDiseaseCard(selectedDate)
            loadTodos()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadStats() {
        dayStat = CalendarHistoryStats.load(requireContext(), gson)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupCalendar() {
        val baseWeekStart = startOfWeek(today)

        // 한 페이지가 월~일 7일이다. 앞뒤 2년치 주간 페이지를 준비한다.
        weekStartDates = (-104..104).map { offset ->
            baseWeekStart.plusWeeks(offset.toLong())
        }

        calendarAdapter = WeekCalendarAdapter(
            weekStartDates = weekStartDates,
            selectedDate = selectedDate,
            stats = dayStat
        ) { date ->
            selectDate(date)
        }

        calendarLayoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        binding.rvCalendar.apply {
            layoutManager = calendarLayoutManager
            adapter = calendarAdapter
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
                        updateSelectedDateFromSnappedWeek()
                    }
                }
            })
        }

        moveToSelectedWeekWithoutAnimation()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun moveToSelectedWeekWithoutAnimation() {
        val selectedWeekIndex = calendarAdapter.weekIndexOf(selectedDate)
        if (selectedWeekIndex == -1) return

        currentWeekPageIndex = selectedWeekIndex
        binding.rvCalendar.post {
            calendarLayoutManager.scrollToPositionWithOffset(selectedWeekIndex, 0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateSelectedDateFromSnappedWeek() {
        val firstVisible = calendarLayoutManager.findFirstCompletelyVisibleItemPosition()
        val pageIndex = if (firstVisible != RecyclerView.NO_POSITION) {
            firstVisible
        } else {
            calendarLayoutManager.findFirstVisibleItemPosition()
        }

        if (pageIndex == RecyclerView.NO_POSITION || pageIndex == currentWeekPageIndex) return

        currentWeekPageIndex = pageIndex
        val dateInNewWeek = calendarAdapter.dateAtSameWeekday(pageIndex, selectedDate)
        selectDate(dateInNewWeek)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectDate(date: LocalDate) {
        selectedDate = date
        calendarAdapter.setSelectedDate(date)
        currentWeekPageIndex = calendarAdapter.weekIndexOf(date)
        updateMonthTitle()
        updateDiseaseCard(date)
        loadTodos()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateMonthTitle() {
        binding.tvMonth.text =
            selectedDate.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateDiseaseCard(date: LocalDate) {
        val stat = dayStat[date]

        val total = stat?.total ?: 0
        val treated = stat?.treated ?: 0
        val untreated = (total - treated).coerceAtLeast(0)

        // 병해 건수는 전체 건수 고정. 처치 완료를 눌러도 줄어들지 않는다.
        binding.tvDiseaseCount.text = "병해 ${total}건"

        binding.tvDiseaseType.text = if (total == 0) {
            "진단 질병: x"
        } else {
            val diseases = stat?.diseaseNames
                ?.filter { it.isNotBlank() }
                ?.joinToString(", ")
                ?.ifBlank { "-" }
                ?: "-"
            "진단 질병: $diseases"
        }

        val colorRes = when {
            total == 0 -> R.color.primary_light
            total < 5 -> R.color.yellow
            else -> R.color.orange
        }

        // 처리 필요 바만 미처치/전체 비율로 왼쪽으로 줄어든다.
        val barRatio = if (total == 0) 0f else untreated.toFloat() / total.toFloat()
        val color = ContextCompat.getColorStateList(requireContext(), colorRes)

        binding.viewDiseaseOrb.backgroundTintList = color
        binding.viewBarFill.backgroundTintList = color

        binding.viewBarFill.post {
            val parentWidth = (binding.viewBarFill.parent as View).width
            val targetWidth = (parentWidth * barRatio).toInt()
            val currentWidth = binding.viewBarFill.width

            ValueAnimator.ofInt(currentWidth, targetWidth).apply {
                duration = 500
                interpolator = DecelerateInterpolator()
                addUpdateListener { anim: ValueAnimator ->
                    val lp = binding.viewBarFill.layoutParams
                    lp.width = anim.animatedValue as Int
                    binding.viewBarFill.layoutParams = lp
                }
                start()
            }
        }
    }

    private lateinit var todoAdapter: TodoAdapter
    private val todos = mutableListOf<TodoItem>()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupTodoList() {
        todoAdapter = TodoAdapter(todos) { item ->
            updateTodoDone(item)
        }

        binding.rvTodoList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTodoList.adapter = todoAdapter

        binding.btnAddTodo.setOnClickListener {
            showAddTodoBottomSheet()
        }

        loadTodos()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTodos() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dateStr = selectedDate.toString()

        FirebaseFirestore.getInstance()
            .collection("Todos")
            .whereEqualTo("userId", uid)
            .whereEqualTo("date", dateStr)
            .get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener

                todos.clear()

                snapshot.documents
                    .mapNotNull { doc -> doc.toTodoItemCompat() }
                    .sortedByDescending { it.id }
                    .forEach { todos.add(it) }

                todoAdapter.notifyDataSetChanged()
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

    private fun updateTodoDone(item: TodoItem) {
        if (item.id.isEmpty()) return

        FirebaseFirestore.getInstance()
            .collection("Todos")
            .document(item.id)
            .set(
                mapOf(
                    "isDone" to item.isDone,
                    "done" to item.isDone
                ),
                SetOptions.merge()
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddTodoBottomSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())

        val view = layoutInflater.inflate(
            R.layout.bottom_sheet_add_todo,
            null
        )

        val etTitle = view.findViewById<android.widget.EditText>(R.id.etTodoTitle)
        val etMemo = view.findViewById<android.widget.EditText>(R.id.etTodoMemo)
        val btnSave = view.findViewById<android.widget.ImageButton>(R.id.btnSaveTodo)

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val memo = etMemo.text.toString().trim()

            if (title.isEmpty()) return@setOnClickListener

            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val dateStr = selectedDate.toString()

            val newItem = TodoItem(
                userId = uid,
                date = dateStr,
                title = title,
                memo = memo,
                isDone = false
            )

            btnSave.isEnabled = false

            FirebaseFirestore.getInstance()
                .collection("Todos")
                .add(
                    mapOf(
                        "userId" to uid,
                        "date" to dateStr,
                        "title" to title,
                        "memo" to memo,
                        "isDone" to false,
                        "done" to false
                    )
                )
                .addOnSuccessListener { docRef ->
                    if (_binding == null) return@addOnSuccessListener

                    newItem.id = docRef.id
                    todos.add(0, newItem)
                    todoAdapter.notifyItemInserted(0)
                    binding.rvTodoList.scrollToPosition(0)
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    btnSave.isEnabled = true
                }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startOfWeek(date: LocalDate): LocalDate {
        return date.minusDays(date.dayOfWeek.value.toLong() - 1L)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
