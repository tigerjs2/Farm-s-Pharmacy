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
import com.example.aos.databinding.FragmentCalendarBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import com.google.android.material.bottomsheet.BottomSheetDialog

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val gson = Gson()

    // 오늘 날짜
    @RequiresApi(Build.VERSION_CODES.O)
    private val today = LocalDate.now()

    // 현재 선택 날짜
    @RequiresApi(Build.VERSION_CODES.O)
    private var selectedDate = today

    // 날짜별 병해 데이터
    private val dayStat = mutableMapOf<Int, Pair<Int, Int>>()

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

        // 현재 월 표시
        binding.tvMonth.text =
            today.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

        loadStats()
        setupCalendar()
        updateDiseaseCard(selectedDate.dayOfMonth)
        setupTodoList()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()

        if (_binding != null) {
            loadStats()
            setupCalendar()
            updateDiseaseCard(selectedDate.dayOfMonth)
            loadTodos()
        }
    }

    private fun loadStats() {

        dayStat.clear()

        val prefs = requireContext().getSharedPreferences(
            "HistoryPrefs",
            android.content.Context.MODE_PRIVATE
        )

        val json = prefs.getString("history_items", "[]") ?: "[]"

        val type = object : TypeToken<List<HistoryItem>>() {}.type

        val items: List<HistoryItem> = try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }

        items.forEach { item ->

            val day = item.date
                .substringAfterLast(".")
                .trimStart('0')
                .toIntOrNull()
                ?: return@forEach

            val current = dayStat.getOrDefault(day, Pair(0, 0))

            dayStat[day] = Pair(
                current.first + 1,
                current.second + if (item.isTreated) 1 else 0
            )
        }

        // 더미 데이터
        if (dayStat.isEmpty()) {
            dayStat[16] = Pair(1, 0)
            dayStat[18] = Pair(1, 1)
            dayStat[today.dayOfMonth] = Pair(6, 0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupCalendar() {

        // 오늘 기준 -3 ~ +3
        val dates = (-3..3).map {
            today.plusDays(it.toLong())
        }

        val dayViews: List<Triple<View, android.widget.ImageView, android.widget.TextView>> = listOf(
            Triple(binding.vDay16, binding.ivDisease16, binding.tvDay16),
            Triple(binding.vDay17, binding.ivDisease17, binding.tvDay17),
            Triple(binding.vDay18, binding.ivDisease18, binding.tvDay18),
            Triple(binding.vDay19, binding.ivDisease19, binding.tvDay19),
            Triple(binding.vDay20, binding.ivDisease20, binding.tvDay20),
            Triple(binding.vDay21, binding.ivDisease21, binding.tvDay21),
            Triple(binding.vDay22, binding.ivDisease22, binding.tvDay22)
        )

        dayViews.forEachIndexed { index, views ->

            val date = dates[index]

            val bgView = views.first
            val diseaseIcon = views.second
            val dayText = views.third

            // 날짜 표시
            dayText.text = date.dayOfMonth.toString()

            // 병해 표시
            val stat = dayStat[date.dayOfMonth]

            if (stat == null || stat.first == 0) {

                diseaseIcon.visibility = View.GONE

            } else {

                val total = stat.first

                diseaseIcon.setImageResource(
                    when {
                        total == 0 -> R.drawable.calendar_no_disease
                        total < 5 -> R.drawable.calendar_yellow_disease
                        else -> R.drawable.calendar_disease
                    }
                )

                diseaseIcon.visibility = View.VISIBLE
            }

            // 선택 색상
            updateDayTint(bgView, date == selectedDate)

            // 날짜 글자색
            dayText.setTextColor(
                if (date == selectedDate)
                    ContextCompat.getColor(requireContext(), android.R.color.white)
                else
                    ContextCompat.getColor(requireContext(), R.color.black)
            )

            // 클릭
            bgView.setOnClickListener {

                selectedDate = date

                setupCalendar()

                updateDiseaseCard(selectedDate.dayOfMonth)

                loadTodos()
            }
        }
    }

    private fun updateDayTint(view: View, isSelected: Boolean) {
        view.backgroundTintList =
            if (isSelected) {
                ContextCompat.getColorStateList(requireContext(), R.color.primary_green)
            } else {
                null
            }
    }

    private fun updateDiseaseCard(day: Int) {

        val stat = dayStat[day]

        val total = stat?.first ?: 0
        val treated = stat?.second ?: 0

        val untreated = total - treated

        binding.tvDiseaseCount.text = "병해 ${untreated}건"

        binding.tvDiseaseType.text =
            if (total == 0)
                "진단 질병: x"
            else
                "진단 질병: 노균병, 기타"

        val colorRes = when {

            total == 0 ->
                R.color.primary_light

            total < 5 ->
                R.color.yellow

            else ->
                R.color.orange
        }

        val barRatio =
            if (total == 0)
                0f
            else
                untreated.toFloat() / total.toFloat()

        val color =
            ContextCompat.getColorStateList(requireContext(), colorRes)

        binding.viewDiseaseOrb.backgroundTintList = color
        binding.viewBarFill.backgroundTintList = color

        binding.viewBarFill.post {

            val parentWidth =
                (binding.viewBarFill.parent as View).width

            val targetWidth =
                (parentWidth * barRatio).toInt()

            val currentWidth =
                binding.viewBarFill.width

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

        binding.rvTodoList.layoutManager =
            LinearLayoutManager(requireContext())

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
                    .mapNotNull { doc ->
                        doc.toObject(TodoItem::class.java)?.also {
                            it.id = doc.id
                        }
                    }
                    .sortedByDescending { it.id }
                    .forEach { todos.add(it) }

                todoAdapter.notifyDataSetChanged()
            }
    }

    private fun updateTodoDone(item: TodoItem) {

        if (item.id.isEmpty()) return

        FirebaseFirestore.getInstance()
            .collection("Todos")
            .document(item.id)
            .update("isDone", item.isDone)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddTodoBottomSheet() {

        val dialog =
            com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())

        val view = layoutInflater.inflate(
            R.layout.bottom_sheet_add_todo,
            null
        )

        val etTitle =
            view.findViewById<android.widget.EditText>(R.id.etTodoTitle)

        val etMemo =
            view.findViewById<android.widget.EditText>(R.id.etTodoMemo)

        val btnSave =
            view.findViewById<android.widget.ImageButton>(R.id.btnSaveTodo)

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
                .add(newItem)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}