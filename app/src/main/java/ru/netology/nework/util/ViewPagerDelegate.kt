package ru.netology.nework.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ViewPagerMediatorDelegate(
    fragment: Fragment,
    private val viewPagerProvider: () -> ViewPager2,
    private val tabLayoutProvider: () -> TabLayout,
    private val adapterFactory: () -> RecyclerView.Adapter<*>,
    private val tabConfigurationStrategy: TabLayoutMediator.TabConfigurationStrategy
) {
    private var mediator: TabLayoutMediator? = null

    init {
        fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
            viewLifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    val viewPager = viewPagerProvider()
                    val tabLayout = tabLayoutProvider()

                    viewPager.adapter = adapterFactory()
                    mediator =
                        TabLayoutMediator(tabLayout, viewPager, tabConfigurationStrategy).apply {
                            attach()
                        }
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    mediator?.detach()
                    mediator = null
                    viewPagerProvider().adapter = null
                }
            })
        }
    }
}

fun Fragment.bindViewPager(
    viewPager: () -> ViewPager2,
    tabLayout: () -> TabLayout,
    adapter: () -> RecyclerView.Adapter<*>,
    tabConfig: TabLayoutMediator.TabConfigurationStrategy
) {
    ViewPagerMediatorDelegate(this, viewPager, tabLayout, adapter, tabConfig)
}