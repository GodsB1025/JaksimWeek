package com.team.jaksimweek.ui.challenge

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.team.jaksimweek.data.model.Challenge
import com.team.jaksimweek.databinding.ActivityEditChallengeBinding

class EditChallengeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditChallengeBinding
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var challengeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditChallengeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        challengeId = intent.getStringExtra("CHALLENGE_ID")
        if (challengeId == null) {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadChallengeData()

        binding.saveButton.setOnClickListener {
            updateChallenge()
        }
    }

    private fun loadChallengeData() {
        firestore.collection("challenges").document(challengeId!!)
            .get()
            .addOnSuccessListener { document ->
                val challenge = document.toObject(Challenge::class.java)
                if (challenge != null) {
                    binding.titleEditText.setText(challenge.title)
                    binding.contentEditText.setText(challenge.description)
                    Glide.with(this).load(challenge.imageUrl).into(binding.postImageView)
                    binding.postImageView.visibility = android.view.View.VISIBLE
                    challenge.location?.let {
                        binding.locationTextView.text = "📍 ${it.addressName}"
                    }
                }
            }
    }

    private fun updateChallenge() {
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.contentEditText.text.toString().trim()

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf(
            "title" to title,
            "description" to description
        )

        firestore.collection("challenges").document(challengeId!!)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "챌린지 정보가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "수정에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }
}
