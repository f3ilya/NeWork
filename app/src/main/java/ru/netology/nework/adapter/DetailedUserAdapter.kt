package ru.netology.nework.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.netology.nework.error.UnknownError
import ru.netology.nework.fragment.user.JobsFragment
import ru.netology.nework.fragment.user.WallFragment

class DetailedUserAdapter(fragment: Fragment, private val userId: Long) :
    FragmentStateAdapter(fragment) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WallFragment.newInstance(userId)
            1 -> JobsFragment.newInstance(userId)

            else -> throw UnknownError()
        }
    }

    override fun getItemCount(): Int = 2
}