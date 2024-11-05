package com.cs407.lab5_milestone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cs407.lab5_milestone.data.NoteSummary
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val onClick: (Int) -> Unit,
    private val onLongClick: (NoteSummary) -> Unit
) : PagingDataAdapter<NoteSummary, NoteAdapter.NoteViewHolder>(NOTE_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(itemView, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val noteSummary = getItem(position)
        if (noteSummary != null) {
            holder.bind(noteSummary)
        }
    }

    class NoteViewHolder(
        itemView: View,
        private val onClick: (Int) -> Unit,
        private val onLongClick: (NoteSummary) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val noteTitle: TextView = itemView.findViewById(R.id.titleTextView)
        private val noteAbstract: TextView = itemView.findViewById(R.id.abstractTextView)
        private val noteDate: TextView = itemView.findViewById(R.id.dateTextView)

        fun bind(noteSummary: NoteSummary) {
            noteTitle.text = noteSummary.noteTitle
            noteAbstract.text = noteSummary.noteAbstract
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            noteDate.text = dateFormatter.format(noteSummary.lastEdited)

            itemView.setOnClickListener {
                onClick(noteSummary.noteId)
            }
            itemView.setOnLongClickListener {
                onLongClick(noteSummary)
                true
            }
        }
    }

    companion object {
        private val NOTE_COMPARATOR = object : DiffUtil.ItemCallback<NoteSummary>() {
            override fun areItemsTheSame(oldItem: NoteSummary, newItem: NoteSummary): Boolean =
                oldItem.noteId == newItem.noteId

            override fun areContentsTheSame(oldItem: NoteSummary, newItem: NoteSummary): Boolean =
                oldItem == newItem
        }
    }
}
