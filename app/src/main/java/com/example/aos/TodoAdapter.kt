package com.example.aos

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.aos.databinding.ItemTodoBinding
import androidx.core.content.ContextCompat

class TodoAdapter(
    private val todos: MutableList<TodoItem>,
    private val onCheckToggle: (TodoItem) -> Unit = {}
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    inner class TodoViewHolder(val binding: ItemTodoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val item = todos[position]

        holder.binding.tvTodoTitle.text = item.title
        holder.binding.tvTodoMemo.text = item.memo

        updateStyle(holder, item)

        holder.binding.btnCheck.setOnClickListener {
            item.isDone = !item.isDone
            updateStyle(holder, item)
            onCheckToggle(item)
        }
    }

    private fun updateStyle(holder: TodoViewHolder, item: TodoItem) {

        val context = holder.itemView.context

        holder.binding.tvTodoTitle.paintFlags =
            holder.binding.tvTodoTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

        holder.binding.tvTodoTitle.setTextColor(
            ContextCompat.getColor(context, R.color.black)
        )

        holder.binding.tvTodoMemo.setTextColor(
            ContextCompat.getColor(context, R.color.black)
        )

        if (item.isDone) {

            // 완료 → 회색
            holder.binding.llTodoItem.setBackgroundColor(
                ContextCompat.getColor(context, R.color.gray)
            )

            holder.binding.btnCheck.setImageResource(
                R.drawable.ic_check
            )

        } else {

            // 진행중 → 연두
            holder.binding.llTodoItem.setBackgroundColor(
                ContextCompat.getColor(context, R.color.primary_light)
            )

            holder.binding.btnCheck.setImageResource(
                R.drawable.ic_check_outline
            )
        }
    }

    override fun getItemCount(): Int = todos.size
}