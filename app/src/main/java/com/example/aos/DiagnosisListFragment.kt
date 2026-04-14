package com.example.aos

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale

class DiagnosisListFragment : Fragment() {

    private lateinit var listContainer: LinearLayout
    private val cropName: String get() = arguments?.getString("cropName") ?: ""

    companion object {
        fun newInstance(cropName: String) = DiagnosisListFragment().apply {
            arguments = Bundle().apply { putString("cropName", cropName) }
        }
    }

    // ── UI를 코드로 생성 (별도 XML 레이아웃 없음) ─────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val scrollView = ScrollView(requireContext()).apply {
            setBackgroundColor(Color.parseColor("#F0EFEB"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(72), dp(24), dp(32))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 뒤로가기 버튼
        root.addView(TextView(requireContext()).apply {
            text = "← 뒤로"
            textSize = 18f
            setTextColor(Color.parseColor("#5B8B5E"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(20) }
            setOnClickListener { parentFragmentManager.popBackStack() }
        })

        // 페이지 타이틀
        root.addView(TextView(requireContext()).apply {
            text = "$cropName 진단 기록"
            textSize = 30f
            setTextColor(Color.parseColor("#2E7D32"))
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(24) }
        })

        // 기록 목록 컨테이너
        listContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(listContainer)

        scrollView.addView(root)
        return scrollView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadDiagnoses()
    }

    // ── Firestore 쿼리: 현재 유저 + 선택 작물 기록 최신순 ──────────────────────
    // 복합 인덱스 필요: Diagnoses | userId ASC, cropType ASC, timestamp DESC
    // → 첫 실행 시 Logcat 오류 메시지의 링크로 Firebase Console에서 자동 생성 가능
    private fun loadDiagnoses() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        listContainer.removeAllViews()
        listContainer.addView(makeLoadingView())

        Firebase.firestore
            .collection("Diagnoses")
            .whereEqualTo("userId", uid)
            .whereEqualTo("cropType", cropName)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                listContainer.removeAllViews()
                if (documents.isEmpty) {
                    listContainer.addView(makeEmptyView())
                    return@addOnSuccessListener
                }
                documents.forEach { doc ->
                    listContainer.addView(makeDiagnosisCard(doc))
                }
            }
            .addOnFailureListener {
                listContainer.removeAllViews()
                listContainer.addView(makeErrorView())
            }
    }

    // ── 진단 기록 카드 뷰 ─────────────────────────────────────────────────────
    private fun makeDiagnosisCard(doc: QueryDocumentSnapshot): View {
        val diseaseName = doc.getString("diseaseName") ?: "-"
        val confidence  = ((doc.getDouble("confidence") ?: 0.0) * 100).toInt()
        val isHandled   = doc.getBoolean("isHandled") ?: false
        val addressDo   = doc.getString("addressDo") ?: ""
        val addressSi   = doc.getString("addressSi") ?: ""
        val timestamp   = doc.getTimestamp("timestamp")?.toDate()
        val memoTitle   = doc.getString("memoTitle") ?: ""
        val dateStr = if (timestamp != null)
            SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(timestamp)
        else "날짜 정보 없음"

        val isDisease = diseaseName != "정상" && diseaseName != "인식 불가"

        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(20), dp(18), dp(20), dp(18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        // 상단 행: 병해명 + 처리 여부 뱃지
        val topRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

        val diseaseColor = when (diseaseName) {
            "정상"    -> Color.parseColor("#005DE8")
            "인식 불가" -> Color.parseColor("#888888")
            else      -> Color.parseColor("#C62828")
        }

        topRow.addView(TextView(requireContext()).apply {
            text = diseaseName
            textSize = 20f
            setTextColor(diseaseColor)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        topRow.addView(TextView(requireContext()).apply {
            text = if (isHandled) "처리완료" else "미처리"
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundColor(if (isHandled) Color.parseColor("#2E7D32") else Color.parseColor("#FF6F00"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
        })
        card.addView(topRow)

        // 신뢰도
        if (isDisease) {
            card.addView(makeInfoText("신뢰도: $confidence%"))
        }

        // 위치
        if (addressDo.isNotEmpty()) {
            card.addView(makeInfoText("위치: $addressDo $addressSi"))
        }

        // 메모 제목 (있을 때만)
        if (memoTitle.isNotEmpty()) {
            card.addView(makeInfoText("메모: $memoTitle"))
        }

        // 날짜
        card.addView(TextView(requireContext()).apply {
            text = dateStr
            textSize = 13f
            setTextColor(Color.parseColor("#AAAAAA"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
        })

        // 처리 여부 토글 버튼 (병해일 때만)
        if (isDisease) {
            card.addView(Button(requireContext()).apply {
                text = if (isHandled) "처리 취소" else "처리 완료로 표시"
                textSize = 14f
                setBackgroundColor(
                    if (isHandled) Color.parseColor("#BDBDBD") else Color.parseColor("#2E7D32")
                )
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(12) }
                setOnClickListener {
                    Firebase.firestore
                        .collection("Diagnoses")
                        .document(doc.id)
                        .update("isHandled", !isHandled)
                        .addOnSuccessListener { loadDiagnoses() }
                }
            })
        }

        return card
    }

    // ── 헬퍼 뷰 ──────────────────────────────────────────────────────────────
    private fun makeInfoText(text: String) = TextView(requireContext()).apply {
        this.text = text
        textSize = 15f
        setTextColor(Color.parseColor("#555555"))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(2) }
    }

    private fun makeLoadingView() = TextView(requireContext()).apply {
        text = "기록을 불러오는 중..."
        textSize = 16f
        setTextColor(Color.GRAY)
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(48) }
    }

    private fun makeEmptyView() = TextView(requireContext()).apply {
        text = "$cropName 진단 기록이 없습니다.\n촬영 후 진단해보세요!"
        textSize = 16f
        setTextColor(Color.parseColor("#888888"))
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(48) }
    }

    private fun makeErrorView() = TextView(requireContext()).apply {
        text = "기록을 불러오지 못했습니다.\nFirebase Console에서 복합 인덱스를 생성해주세요."
        textSize = 14f
        setTextColor(Color.parseColor("#C62828"))
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(48) }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
