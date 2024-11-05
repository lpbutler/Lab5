package com.cs407.lab5_milestone

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.room.RoomDatabase
import com.cs407.lab5_milestone.data.Converters
import com.cs407.lab5_milestone.data.Note
import com.cs407.lab5_milestone.data.NoteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Calendar

class NoteContentFragment(
    private val injectedUserViewModel: UserViewModel? = null
) : Fragment() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var saveButton: Button

    private var noteId: Int = 0
    private lateinit var noteDB: NoteDatabase
    private lateinit var userViewModel: UserViewModel
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteId = arguments?.getInt("noteId") ?: 0
        noteDB = NoteDatabase.getDatabase(requireContext())
        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            // TOD - Use ViewModelProvider to init UserViewModel
            //UserViewModel()
            ViewModelProvider(requireActivity())[UserViewModel::class.java]
        }
        userId = userViewModel.userState.value.id
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_note_content, container, false)
        titleEditText = view.findViewById(R.id.titleEditText)
        contentEditText = view.findViewById(R.id.contentEditText)
        saveButton = view.findViewById(R.id.saveButton)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupBackNavigation()

        if (noteId != 0) {
            // TOD: Launch a coroutine to fetch the note from the database in the background
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // TOD: Retrieve the note from the Room database using the noteId
                    val note = noteDB.noteDao().getById(noteId)
                    // TOD: Check if the note content is stored in the database or in a file
                    var check = (note.notePath != null)
                    // TOD: If the content is too large and stored as a file, read the file content
                    val filename = "${note.notePath}"
                    //Toast.makeText(requireContext(), filename, Toast.LENGTH_SHORT).show()
                    Toast.makeText(requireContext(), filename, Toast.LENGTH_SHORT).show()
                    val noteContent = note.noteDetail ?: note.notePath?. let { File(requireContext().filesDir, note.notePath).readText()}
                    /**
                    var noteContent = ""
                    if (check) {
                        noteContent = requireContext().filesDir.path
                        noteContent = requireContext().openFileInput(note.notePath).bufferedReader()
                            .use { reader ->
                                reader.readText()
                            }
                    } else {
                        noteContent = note.noteDetail.toString()
                    }
                    **/
                    // TOD: Switch back to the main thread to update the UI with the note content
                    withContext(Dispatchers.Main) {
                        // TOD: Set the retrieved note title to the title EditText field
                        titleEditText.setText(note.noteTitle)
                        // TOD: Set the note content (either from the file or the database) to the content EditText field
                        contentEditText.setText(noteContent)
                    }
                    // TOD: Optionally handle exceptions (e.g., file not found, database errors) if necessary
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        saveButton.setOnClickListener {
            saveContent() //error here
        }
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        findNavController().popBackStack()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        if (activity is AppCompatActivity) {
            (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupBackNavigation() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun saveContent() {
        // TOD: Retrieve the title and content from EditText fields
        var titleName = titleEditText.text.toString()
        var content = contentEditText.text.toString()
        var abstract = splitAbstractDetail(content) //noteAbstract is the first 20 characters and noteDetail is the entire message
        var lastEdited = Calendar.getInstance().time
        var path = "note-$userId-$noteId-$lastEdited"
        // TOD: Launch a coroutine to save the note in the background (non-UI thread)
        viewLifecycleOwner.lifecycleScope.launch { //error here
            /**
            // TOD: Insert or update the note in the Room database using the DAO method
            if (content.length > 10) {
                var file = File("$path")
                file.writeText(content)
            }
            //Log.i("INFO", userId.toString())
            noteDB.noteDao().upsertNote(
                Note(
                    // TOD: Ensure that noteId is assigned (could be auto-generated in Room)
                    noteId = noteId,
                    noteTitle = titleName,
                    // TOD: Implement logic to create an abstract from the content - already done above
                    noteAbstract = abstract,
                    // TOD: Check if the note content is too large for direct storage in the database
            // change noteDetail to null if too big
            // TOD: Save the content as a file if it's too large for the database
            // TOD: Store the note content directly in the database if it's small enough
            noteDetail = if (content.length > 10) null else content,
            // TOD: Store the note content directly in the database if it's small enough
            // name the file path following the convention: note-$userId-$noteId-$lastEdited
            notePath = if (content.length < 10) null else path,
            lastEdited = Calendar.getInstance().time
            ), userId
            // TOD: Ensure that userId is passed correctly (it should be associated with the note)
            )
            // TOD: Switch back to the main thread to navigate the UI after saving
            withContext(Dispatchers.Main) {
            // TOD: Navigate back to the previous screen (e.g., after saving the note)
            findNavController().popBackStack()
             **/
            if (content.toByteArray().size > 1024) {
                val fileName = "note-$userId-$noteId-$lastEdited"
                requireContext().openFileOutput(fileName, Context.MODE_PRIVATE).use {
                    it.write(content.toByteArray())
                }
                Toast.makeText(requireContext(), fileName, Toast.LENGTH_SHORT).show()
                noteDB.noteDao().upsertNote(
                    Note(
                        noteId,
                        titleName,
                        abstract,
                        null,
                        fileName,
                        lastEdited
                    ), userId
                )
            } else {
                noteDB.noteDao().upsertNote(
                    Note(
                        noteId,
                        titleName,
                        abstract,
                        content,
                        null,
                        lastEdited
                    ), userId
                )

            }
            //make sure userID is passed correctly
        }
        findNavController().popBackStack()
    }

    private fun splitAbstractDetail(content: String?): String {
        val stringList = content?.split('\n', limit = 2) ?: listOf("")
        var stringAbstract = stringList[0]
        if (stringAbstract.length > 20) {
            stringAbstract = stringAbstract.substring(0, 20) + "..."
        }
        return stringAbstract
    }
}
