package ru.netology.nework.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.netology.nework.error.AppError
import ru.netology.nework.fragment.user.JobsFragment
import ru.netology.nework.fragment.user.WallFragment

class DetailedUserAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val userId: Long,
    private val isMyProfile: Boolean
) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WallFragment.newInstance(userId, isMyProfile)
            1 -> JobsFragment.newInstance(userId, isMyProfile)

            else -> throw AppError.UnknownError()
        }
    }

    override fun getItemCount(): Int = 2
}