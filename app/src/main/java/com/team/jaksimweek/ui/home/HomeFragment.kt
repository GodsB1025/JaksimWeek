package com.team.jaksimweek.ui.home

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.team.jaksimweek.adapter.ChallengeAdapter
import com.team.jaksimweek.data.model.Challenge
import com.team.jaksimweek.databinding.FragmentHomeBinding
import com.team.jaksimweek.ui.challenge.ChallengeDetailActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var challengeAdapter: ChallengeAdapter
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        // 초기에는 전체 목록을 보여줍니다.
        listenForChallengeUpdates("")
    }

    private fun setupRecyclerView() {
        challengeAdapter = ChallengeAdapter(emptyList())
        binding.rvChallenges.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = challengeAdapter
        }
        challengeAdapter.setOnItemClickListener(object : ChallengeAdapter.OnItemClickListener {
            override fun onItemClick(challenge: Challenge) {
                val intent = Intent(requireContext(), ChallengeDetailActivity::class.java)
                intent.putExtra("CHALLENGE_ID", challenge.id)
                startActivity(intent)
            }
        })
    }

    /**
     * 검색창에 TextWatcher를 설정하여 텍스트 변경을 감지합니다.
     */
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 텍스트가 변경될 때마다 검색 쿼리를 실행합니다.
                val searchText = s.toString().trim()
                listenForChallengeUpdates(searchText)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * 검색어에 따라 'challenges' 컬렉션의 변경사항을 실시간으로 감지합니다.
     * @param searchText 사용자가 입력한 검색어
     */
    private fun listenForChallengeUpdates(searchText: String) {
        // 기존 리스너가 있다면 제거하여 중복 실행을 방지합니다.
        firestoreListener?.remove()

        var query: Query = firestore.collection("challenges")

        // 검색어가 있는 경우, 쿼리를 수정합니다.
        if (searchText.isNotEmpty()) {
            query = query.orderBy("title")
                .whereGreaterThanOrEqualTo("title", searchText)
                .whereLessThanOrEqualTo("title", searchText + '\uf8ff')
        } else {
            // 검색어가 없는 경우, 최신순으로 정렬합니다.
            query = query.orderBy("createdAt", Query.Direction.DESCENDING)
        }

        firestoreListener = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e("HomeFragment", "Listen failed.", error)
                Toast.makeText(context, "데이터를 실시간으로 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val challenges = snapshots.toObjects(Challenge::class.java)
                challengeAdapter.updateData(challenges)
                Log.d("HomeFragment", "검색 결과 업데이트: ${challenges.size}개")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
        _binding = null
    }
}
