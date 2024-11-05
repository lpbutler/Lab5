package com.cs407.lab5_milestone

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cs407.lab5_milestone.data.NoteDatabase
import com.cs407.lab5_milestone.data.User
import com.cs407.lab5_milestone.data.UserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class LoginFragment(
    private val injectedUserViewModel: UserViewModel? = null // For testing only
) : Fragment() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var errorTextView: TextView

    private lateinit var userViewModel: UserViewModel

    private lateinit var userPasswdKV: SharedPreferences
    private lateinit var noteDB: NoteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        usernameEditText = view.findViewById(R.id.usernameEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        loginButton = view.findViewById(R.id.loginButton)
        errorTextView = view.findViewById(R.id.errorTextView)

        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            // TOD - Use ViewModelProvider to init UserViewModel
            //UserViewModel()
            ViewModelProvider(requireActivity())[UserViewModel::class.java]
        }

        // TOD - Get shared preferences from using R.string.userPasswdKV as the name
        // maybe change activity or variable name
        //userPasswdKV = activity?.getSharedPreferences(getString(R.string.userPasswdKV), Context.MODE_PRIVATE)!!
        val context = requireContext()
        userPasswdKV = context.getSharedPreferences(getString(R.string.userPasswdKV), Context.MODE_PRIVATE)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        usernameEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }

        passwordEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }

        // Set the login button click action
        loginButton.setOnClickListener {
            // TOD: Get the entered username and password from EditText fields
            val inputUser = usernameEditText.text.toString()
            val inputPass = passwordEditText.text.toString()
            if (inputUser == null || inputPass == null || inputUser == "" || inputPass == "") {
                    errorTextView.visibility = View.VISIBLE
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    var attempt =
                        withContext(Dispatchers.IO) {
                            getUserPasswd(inputUser, inputPass)
                        }
                    // TOD: Show an error message if either username or password is empty
                    if(!attempt) {
                    errorTextView.visibility = View.VISIBLE
                } else{
                        // TOD: Set the logged-in user in the ViewModel (store user info) (placeholder)
                        //instead of 0: noteDB.userDao().getByName(inputUser).userId
                        noteDB = NoteDatabase.getDatabase(requireContext())
                        userViewModel.setUser(UserState(noteDB.userDao().getByName(inputUser).userId, inputUser, inputPass)) // You will implement this in UserViewModel

                        // TOD: Navigate to another fragment after successful login
                        findNavController().navigate(R.id.action_loginFragment_to_noteListFragment) // Example navigation action
                    }
                }
            }


        }
    }

    private suspend fun getUserPasswd(
        name: String,
        passwdPlain: String
    ): Boolean {
        // TOD: Hash the plain password using a secure hashing function
        val passwdHash = hash(passwdPlain)

        // TOD: Check if the user exists in SharedPreferences (using the username as the key)
        var exists = userPasswdKV.contains(name)

        // TOD: Retrieve the stored password from SharedPreferences
        //if the username exists, retrieve its 'key'/password
        var storedPass : String?
        if(exists){
            storedPass = userPasswdKV.getString(name, null)
        } else{
            storedPass = null
        }
        // TOD: Compare the hashed password with the stored one and return false if they don't match
        if(storedPass != null && (passwdHash == storedPass)){
            //user exists and password is correct
            return true
        } else if(storedPass != null){
            //user exists, incorrect password
            return false
        }
        // TOD: If the user doesn't exist in userPasswdKV, create a new user
        if(!exists){
            val editPref = userPasswdKV.edit()
            // TOD: Store the hashed password in SharedPreferences for future logins
            editPref.putString(name, passwdHash)
            editPref.apply()
            // TOD: Insert the new user into the Room database (implement this in your User DAO)
            //change userID back to 0?
            //Log.i("INFO", "userID: "+userViewModel.userState.value.id)
            var u = User(userName = name, userId = 0)
            NoteDatabase.getDatabase(requireContext()).userDao().insert(u)
            // TOD: Return true if the user login is successful or the user was newly created
            return true
        }
        return false
    }

    //Hash code provided from Sha256
    class Hasher {
        fun hash(): String {
            val bytes = this.toString().toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.fold("", { str, it -> str + "%02x".format(it) })
        }
    }

    private fun hash(input: String): String {
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}