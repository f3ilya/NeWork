package ru.netology.nework.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.netology.nework.error.UnknownError
import ru.netology.nework.fragment.user.JobsFragment
import ru.netology.nework.fragment.user.WallFragment

class DetailedUserAdapter(
    fragment: Fragment,
    private val userId: Long,
    private val isMyProfile: Boolean
) :
    FragmentStateAdapter(fragment) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WallFragment.newInstance(userId, isMyProfile)
            1 -> JobsFragment.newInstance(userId, isMyProfile)

            else -> throw UnknownError()
        }
    }

    override fun getItemCount(): Int = 2
}