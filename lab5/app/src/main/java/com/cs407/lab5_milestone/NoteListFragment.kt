package com.cs407.lab5_milestone

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs407.lab5_milestone.data.Note
import com.cs407.lab5_milestone.data.NoteDatabase
import com.cs407.lab5_milestone.data.NoteSummary
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class NoteListFragment(
    private val injectedUserViewModel: UserViewModel? = null
) : Fragment() {

    private lateinit var greetingTextView: TextView
    private lateinit var noteRecyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton

    private lateinit var userViewModel: UserViewModel

    private lateinit var noteDB: NoteDatabase
    private lateinit var userPasswdKV: SharedPreferences

    private var deleteIt: Boolean = false
    private lateinit var noteToDelete: NoteSummary

    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteDB = NoteDatabase.getDatabase(requireContext())
        userPasswdKV = requireContext().getSharedPreferences(
            getString(R.string.userPasswdKV), Context.MODE_PRIVATE)
        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            // TOD - Use ViewModelProvider to init UserViewModel
            //UserViewModel()
            ViewModelProvider(requireActivity())[UserViewModel::class.java]
        }

        // Manually create 1000 notes for "large" user
        val userState = userViewModel.userState.value
        lifecycleScope.launch {
            val countNote = noteDB.noteDao().userNoteCount(userState.id)
            if (countNote == 0 && userState.name == "large") {
                for (i in 1..1000) {
                    noteDB.noteDao().upsertNote(
                        Note(
                            noteTitle = "Note $i",
                            noteAbstract = "This is Note $i",
                            noteDetail = "Welcome to Note $i",
                            notePath = null,
                            lastEdited = Calendar.getInstance().time
                        ), userState.id
                    )
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_note_list, container, false)
        greetingTextView = view.findViewById(R.id.greetingTextView)
        noteRecyclerView = view.findViewById(R.id.noteRecyclerView)
        fab = view.findViewById(R.id.fab)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.note_list_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_logout -> {
                        userViewModel.setUser(UserState())
                        findNavController().navigate(R.id.action_noteListFragment_to_loginFragment)
                        true
                    }
                    R.id.action_delete_account -> {
                        deleteAccountAndLogout()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)

        val userState = userViewModel.userState.value
        greetingTextView.text = getString(R.string.greeting_text, userState.name)

        adapter = NoteAdapter(
            onClick = { noteId ->
                val action = NoteListFragmentDirections.actionNoteListFragmentToNoteContentFragment(noteId)
                findNavController().navigate(action)
            },
            onLongClick = { noteSummary ->
                deleteIt = true
                noteToDelete = noteSummary
                showDeleteBottomSheet()
            }
        )

        noteRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        noteRecyclerView.adapter = adapter

        loadNotes()

        fab.setOnClickListener {
            val action = NoteListFragmentDirections.actionNoteListFragmentToNoteContentFragment(0)
            findNavController().navigate(action)
        }
    }

    private fun loadNotes() {
        // TOD: Retrieve the current user state from the ViewModel (to get the user ID)
        val userState = userViewModel.userState.value

        // TODO: Set up paging configuration with a specified page size and prefetch distance
        //Then, create a Pager and pass the PagingSource to it, for instance (set pageSize to 20 and
        //prefetchDistance to 10):
        val pagingConfig = PagingConfig(pageSize = 20,prefetchDistance = 10)

        // TODO: Implement a query to retrieve the paged list of notes associated with the user
        val pager = Pager(config = pagingConfig, pagingSourceFactory = { noteDB.userDao().getUsersWithNoteListsByIdPaged(userState.id) })

        // TOD: Launch a coroutine to collect the paginated flow and submit it to the RecyclerView adapter
        viewLifecycleOwner.lifecycleScope.launch {
            pager.flow
                // TOD: Cache the paging flow in the lifecycle scope and collect the paginated data
                .cachedIn(lifecycleScope)
                .collect { pagingData ->
                    adapter.submitData(pagingData)
                }
            // TOD: Submit the paginated data to the adapter to display it in the RecyclerView - in line above
        }
    }


    private fun showDeleteBottomSheet() {
        if (deleteIt) {
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_delete, null)
            bottomSheetDialog.setContentView(bottomSheetView)

            val deleteButton = bottomSheetView.findViewById<Button>(R.id.deleteButton)
            val cancelButton = bottomSheetView.findViewById<Button>(R.id.cancelButton)
            val deletePrompt = bottomSheetView.findViewById<TextView>(R.id.deletePrompt)

            deletePrompt.text = "Delete Note: ${noteToDelete.noteTitle}"

            deleteButton.setOnClickListener {
                // TOD: Launch a coroutine to perform the note deletion in the background
                viewLifecycleOwner.lifecycleScope.launch {
                    // TOD: Implement the logic to delete the note from the Room database using the DAO
                    noteDB.deleteDao().deleteNote(noteToDelete.noteId)
                    // TOD: Reset any flags or variables that control the delete state
                    deleteIt = false // Example of resetting a flag after deletion

                    // TOD: Dismiss the bottom sheet dialog after the deletion is completed
                    bottomSheetDialog.dismiss()

                    // TOD: Reload the list of notes to reflect the deleted note (e.g., refresh UI)
                    loadNotes() // Implement the function to refresh or reload the notes
                }
            }

            cancelButton.setOnClickListener {
                deleteIt = false
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.setOnDismissListener {
                deleteIt = false
            }

            bottomSheetDialog.show()
        }
    }

    private fun deleteAccountAndLogout() {
        // TOD: Retrieve the current user state from the ViewModel (contains user details)
        val userState = userViewModel.userState.value
        // TOD: Launch a coroutine to perform account deletion in the background
        viewLifecycleOwner.lifecycleScope.launch() {
            // TOD: Implement the logic to delete the user's data from the Room database
            val allNotes = noteDB.deleteDao().getAllNoteIdsByUser(userState.id)

            Log.i("INFO", "number of notes for " + userState.name + " before: " + noteDB.deleteDao().getAllNoteIdsByUser(userState.id).size)
            noteDB.deleteDao().deleteNotes(allNotes)
            // TODO: Remove the user's credentials from SharedPreferences
            Log.i("INFO", "number of notes for " + userState.name + " after: " + noteDB.deleteDao().getAllNoteIdsByUser(userState.id).size)

            Log.i("INFO", "user ID: " + noteDB.userDao().getById(userState.id))
            noteDB.deleteDao().deleteUser(userState.id)
            noteDB.userDao().getById(userState.id)
            Log.i("INFO", "user ID after delete: " + noteDB.userDao().getById(userState.id))

            Log.i("INFO", "before remove of SharedPref: " + userPasswdKV.contains(userState.name))
            userPasswdKV.edit().remove(userState.name).apply()
            Log.i("INFO", "after remove of SharedPref: " + userPasswdKV.contains("carly"))
            Log.i("INFO", "after remove of SharedPref: " + userPasswdKV.contains("lucy"))
            Log.i("INFO", "after remove of SharedPref: " + userPasswdKV.contains(userState.name))
            // TOD: Reset the user state in the ViewModel to represent a logged-out state
            userViewModel.setUser(UserState())

            // TOD: Navigate back to the login screen after the account is deleted and user is logged out
            findNavController().navigate(R.id.action_noteListFragment_to_loginFragment)
        }
    }
}