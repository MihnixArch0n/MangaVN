package com.example.mybookslibrary.data.repository

import android.content.Context
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.di.IoDispatcher
import com.example.mybookslibrary.domain.model.AuthStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for user authentication actions (Email, Google, Guest, Sign Out, Delete Account).
 */
@Singleton
class AuthRepository
    @Inject
    constructor(
        private val firebaseAuth: FirebaseAuth,
        private val preferencesDataStore: UserPreferencesDataStore,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val googleSignInClient: GoogleSignInClient,
    ) {
        /**
         * Observes the current authentication status.
         *
         * @return A Flow emitting the current AuthStatus.
         */
        fun observeAuthStatus(): Flow<AuthStatus> = preferencesDataStore.observeAuthStatus()

        /**
         * Gets the currently authenticated Firebase user, if any.
         *
         * @return The current FirebaseUser, or null if none.
         */
        fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

        /**
         * Signs out the user from Firebase and resets the local authentication state.
         */
        suspend fun signOut() {
            firebaseAuth.signOut()
            preferencesDataStore.updateAuthStatus(AuthStatus.LOGGED_OUT)
            preferencesDataStore.updateFirebaseUid(null)
        }

        /**
         * Transitions the user to guest mode.
         */
        suspend fun continueAsGuest() {
            preferencesDataStore.updateAuthStatus(AuthStatus.GUEST)
            preferencesDataStore.updateFirebaseUid(null)
        }

        /**
         * Registers a new account using email and password.
         *
         * @param email The registration email.
         * @param password The registration password.
         * @return A Result containing the registered FirebaseUser on success.
         */
        suspend fun registerWithEmail(
            email: String,
            password: String,
        ): Result<FirebaseUser> =
            withContext(ioDispatcher) {
                try {
                    val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                    val user = result.user ?: throw Exception("User is null after registration")
                    preferencesDataStore.updateAuthStatus(AuthStatus.LOGGED_IN)
                    preferencesDataStore.updateFirebaseUid(user.uid)
                    Result.success(user)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        /**
         * Signs in an existing account using email and password.
         *
         * @param email The sign-in email.
         * @param password The sign-in password.
         * @return A Result containing the authenticated FirebaseUser on success.
         */
        suspend fun signInWithEmail(
            email: String,
            password: String,
        ): Result<FirebaseUser> =
            withContext(ioDispatcher) {
                try {
                    val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                    val user = result.user ?: throw Exception("User is null after sign in")
                    preferencesDataStore.updateAuthStatus(AuthStatus.LOGGED_IN)
                    preferencesDataStore.updateFirebaseUid(user.uid)
                    Result.success(user)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        /**
         * Signs in the user using Google credentials retrieved via Credential Manager.
         *
         * @param context The Android Context to trigger Credential Manager.
         * @return A Result containing the authenticated FirebaseUser on success.
         */
        suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> =
            try {
                // Credential Manager presents UI, so keep this call on the caller's Main dispatcher.
                val account = googleSignInClient.getGoogleAccount(context).getOrThrow()

                withContext(ioDispatcher) {
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    val result = firebaseAuth.signInWithCredential(credential).await()
                    val user = result.user ?: throw Exception("User is null after Google sign in")

                    preferencesDataStore.updateAuthStatus(AuthStatus.LOGGED_IN)
                    preferencesDataStore.updateFirebaseUid(user.uid)
                    Result.success(user)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }

        /**
         * Permanently deletes the current user account from Firebase Auth and resets local storage.
         *
         * @return A Result representing success or failure.
         */
        suspend fun deleteAccount(): Result<Unit> =
            withContext(ioDispatcher) {
                try {
                    // Xóa Firestore data sẽ được thực hiện ở UseCase/ViewModel (Task 4)
                    // Xóa Firebase Auth user
                    firebaseAuth.currentUser?.delete()?.await()

                    preferencesDataStore.updateAuthStatus(AuthStatus.LOGGED_OUT)
                    preferencesDataStore.updateFirebaseUid(null)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
    }
