package com.team.jaksimweek.viewmodel // 실제 프로젝트 구조에 맞게 수정

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.team.jaksimweek.data.model.User // 제공해주신 User 데이터 클래스
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val _searchResults = MutableLiveData<List<User>>()
    val searchResults: LiveData<List<User>> = _searchResults

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun searchUser(query: String) {
        viewModelScope.launch {
            try {
                db.collection("users")
                    .whereEqualTo("nickname", query)
                    .get()
                    .addOnSuccessListener { nicknameResults ->
                        val userList = nicknameResults.toObjects(User::class.java)

                        if (userList.isNotEmpty()) {
                            _searchResults.value = userList
                        } else {
                            searchByEmail(query)
                        }
                    }
                    .addOnFailureListener { exception ->
                        _errorMessage.value = "닉네임 검색 중 오류가 발생했습니다: ${exception.message}"
                    }
            } catch (e: Exception) {
                _errorMessage.value = "알 수 없는 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    private fun searchByEmail(email: String) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { emailResults ->
                _searchResults.value = emailResults.toObjects(User::class.java)
            }
            .addOnFailureListener { exception ->
                _errorMessage.value = "이메일 검색 중 오류가 발생했습니다: ${exception.message}"
            }
    }
}