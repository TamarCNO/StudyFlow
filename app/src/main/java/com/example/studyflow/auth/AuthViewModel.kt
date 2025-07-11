import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)  // אפשר להחליף ל-false לביטול קאש מקומי
            .build()
        db.firestoreSettings = settings
    }

    val loadingState = MutableLiveData<LoadingState>()
    val exceptionsState = MutableLiveData<Exception?>()

    fun registerStudent(student: Student, onComplete: () -> Unit) {
        loadingState.value = LoadingState.Loading
        exceptionsState.value = null

        viewModelScope.launch {
            try {
                // יצירת משתמש ב-Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(student.email, student.password).await()

                // שמירת פרטי משתמש ב-Firestore תחת אוסף "students" עם id = uid של FirebaseAuth
                val uid = authResult.user?.uid ?: throw Exception("User ID is null")

                val studentData = mapOf(
                    "id" to uid,
                    "first_name" to student.first_name,
                    "last_name" to student.last_name,
                    "email" to student.email,
                    "profileImageUrl" to student.profileImageUrl
                )

                firestore.collection("students")
                    .document(uid)
                    .set(studentData)
                    .await()

                loadingState.postValue(LoadingState.Success)
                onComplete()
            } catch (e: Exception) {
                exceptionsState.postValue(e)
                loadingState.postValue(LoadingState.Error)
                onComplete()
            }
        }
    }

    fun login(email: String, password: String, onComplete: () -> Unit) {
        loadingState.value = LoadingState.Loading
        exceptionsState.value = null

        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                loadingState.postValue(LoadingState.Success)
                onComplete()
            } catch (e: Exception) {
                exceptionsState.postValue(e)
                loadingState.postValue(LoadingState.Error)
                onComplete()
            }
        }
    }

    // אפשר להוסיף פונקציות נוספות כמו logout, getCurrentUser וכו'
}
