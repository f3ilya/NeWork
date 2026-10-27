package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nework.databinding.CardUserBinding
import ru.netology.nework.extensions.load
import ru.netology.nework.model.UserModel

class UsersAdapter(
    private val isSelectedMode: Boolean,
    private val onUserClicked: (Long) -> Unit
) : ListAdapter<UserModel, UsersAdapter.UserViewHolder>(UserDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = CardUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: CardUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(userModel: UserModel) {
            with(binding) {
                avatar.load(userModel.user.avatar, true)
                name.text = userModel.user.name
                login.text = userModel.user.login
                checkbox.isVisible = isSelectedMode
                checkbox.isChecked = userModel.isSelected
                itemView.setOnClickListener { onUserClicked(userModel.user.id) }
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.user.id == newItem.user.id
        }

        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem == newItem
        }

    }
}