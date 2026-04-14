package com.example.aos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.*

class ResultActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    // Intent로 받은 데이터 전체 보관
    private var imageUri: String? = null
    private var imageUrl: String = ""
    private var cropName: String = ""
    private var diagType: String = ""
    private var label: String = ""
    private var confidence: Int = 0
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var addressDo: String = ""
    private var addressSi: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        viewPager = findViewById(R.id.viewPager)

        // ── Intent 데이터 추출 ────────────────────────────────────────────────
        imageUri   = intent.getStringExtra("imageUri")
        imageUrl   = intent.getStringExtra("imageUrl") ?: ""
        cropName   = intent.getStringExtra("cropName") ?: ""
        diagType   = intent.getStringExtra("diagType") ?: "UNKNOWN"
        label      = intent.getStringExtra("label") ?: ""
        confidence = intent.getIntExtra("confidence", 0)
        latitude   = intent.getDoubleExtra("latitude", 0.0)
        longitude  = intent.getDoubleExtra("longitude", 0.0)
        addressDo  = intent.getStringExtra("addressDo") ?: ""
        addressSi  = intent.getStringExtra("addressSi") ?: ""

        if (diagType == "DISEASE") {
            // API 호출 → Firestore 저장 → ViewPager 구성
            lifecycleScope.launch {
                val diseaseInfo = DiseaseRepository.getDiseaseInfo(cropName, label)
                saveDiagnosis()
                setupViewPager(diseaseInfo)
            }
        } else {
            // NORMAL / UNKNOWN은 API 불필요
            saveDiagnosis()
            setupViewPager(null)
        }
    }

    // ── ViewPager 구성 ────────────────────────────────────────────────────────
    private fun setupViewPager(diseaseInfo: DiseaseInfo?) {
        val adapter = ResultPagerAdapter(this, diseaseInfo)
        viewPager.adapter = adapter
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        viewPager.isUserInputEnabled = (diagType == "DISEASE")
    }

    // ── Firestore: Diagnoses 컬렉션 저장 ─────────────────────────────────────
    private fun saveDiagnosis() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = Firebase.firestore

        val savedLabel = when (diagType) {
            "NORMAL"  -> "정상"
            "UNKNOWN" -> "인식 불가"
            else      -> label
        }

        val data = hashMapOf(
            "userId"      to uid,
            "cropType"    to cropName,
            "diseaseName" to savedLabel,
            "confidence"  to confidence / 100.0,   // DB 스키마: 0.95 형식
            "imageUrl"    to imageUrl,
            "location"    to GeoPoint(latitude, longitude),
            "addressDo"   to addressDo,
            "addressSi"   to addressSi,
            "timestamp"   to FieldValue.serverTimestamp(),
            "isCounted"   to false,
            "isHandled"   to false,
            "memoTitle"   to "",
            "memoContent" to ""
        )

        db.collection("Diagnoses").add(data)
            .addOnSuccessListener { docRef ->
                // 병해이고 위치 정보가 있을 때만 isCounted 판단
                if (diagType == "DISEASE" && latitude != 0.0) {
                    checkIsCountedAndUpdateStats(docRef.id, savedLabel)
                }
            }
    }

    // ── isCounted 판단: 1km 반경 / 30일 이내 동일 병해 발생 여부 확인 ──────────
    // Firestore 복합 인덱스 필요:
    //   Diagnoses | diseaseName ASC, isCounted ASC, timestamp ASC
    //   → Firebase Console에서 오류 메시지의 링크로 자동 생성 가능
    private fun checkIsCountedAndUpdateStats(docId: String, diseaseName: String) {
        val db = Firebase.firestore
        val cutoff = Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)

        db.collection("Diagnoses")
            .whereEqualTo("diseaseName", diseaseName)
            .whereEqualTo("isCounted", true)
            .whereGreaterThan("timestamp", cutoff)
            .get()
            .addOnSuccessListener { snapshots ->
                val nearbyExists = snapshots.documents.any { doc ->
                    val geo = doc.getGeoPoint("location") ?: return@any false
                    distanceKm(latitude, longitude, geo.latitude, geo.longitude) < 1.0
                }

                if (!nearbyExists) {
                    // 1km 반경 내 동일 병해 없음 → 새 발생으로 인정
                    db.collection("Diagnoses").document(docId)
                        .update("isCounted", true)
                    updateStatistics()
                }
            }
    }

    // ── Statistics 컬렉션 업데이트 (트랜잭션으로 동시성 보장) ─────────────────
    private fun updateStatistics() {
        if (addressDo.isEmpty()) return
        val db = Firebase.firestore

        // 문서 ID: "전라남도-광주시" 형식
        val regionId = "${addressDo}-${addressSi}".replace(" ", "-")
        val statsRef = db.collection("Statistics").document(regionId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(statsRef)
            val total = snapshot.getLong("totalCount") ?: 0

            @Suppress("UNCHECKED_CAST")
            val cropStats = (snapshot.get("cropStats") as? Map<String, Long>)
                ?.toMutableMap() ?: mutableMapOf()
            cropStats[cropName] = (cropStats[cropName] ?: 0) + 1

            transaction.set(
                statsRef,
                hashMapOf(
                    "totalCount" to total + 1,
                    "cropStats"  to cropStats,
                    "lastUpdate" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
        }
    }

    // ── Haversine 공식으로 두 좌표 간 거리(km) 계산 ──────────────────────────
    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // ── ViewPager Adapter ─────────────────────────────────────────────────────
    private inner class ResultPagerAdapter(
        fa: FragmentActivity,
        private val diseaseInfo: DiseaseInfo?
    ) : FragmentStateAdapter(fa) {

        override fun getItemCount() = if (diagType == "DISEASE") 2 else 1

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> ResultFragment.newInstance(
                imageUri, cropName, diagType, label, confidence,
                briefDesc = diseaseInfo?.briefDesc ?: ""
            )
            1 -> DetailFragment.newInstance(cropName, diseaseInfo)
            else -> ResultFragment.newInstance(imageUri, cropName, diagType, label, confidence)
        }
    }
}
