package ru.netology.nework.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.NavGraphDirections
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.ActivityAppBinding
import ru.netology.nework.extensions.load
import ru.netology.nework.extensions.shareIntent
import ru.netology.nework.extensions.showConfirmationDialog
import ru.netology.nework.fragment.event.DetailedEventFragmentArgs
import ru.netology.nework.fragment.post.DetailedPostFragmentArgs
import ru.netology.nework.fragment.user.DetailedUserFragmentArgs
import ru.netology.nework.fragment.user.UsersFragmentArgs
import ru.netology.nework.util.FragmentMode
import ru.netology.nework.util.OnSaveActionListener
import javax.inject.Inject

@AndroidEntryPoint
class AppActivity : AppCompatActivity(R.layout.activity_app) {
    @Inject
    lateinit var auth: AppAuth

    private val binding by viewBinding(ActivityAppBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)
            binding.bottomNavigation.post {
                val menuHeight = if (binding.bottomNavigation.isVisible) {
                    binding.bottomNavigation.height
                } else {
                    0
                }
                binding.navHostFragment.setPadding(0, 0, 0, menuHeight)
            }
            insets
        }

        setupMenu()
        observes()
    }

    private fun setupMenu() {
        with(binding) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            val appBarConfig = AppBarConfiguration(
                setOf(
                    R.id.postsFragment,
                    R.id.eventsFragment,
                    R.id.usersFragment,
                )
            )

            toolbar.setupWithNavController(navController, appBarConfig)
            bottomNavigation.setupWithNavController(navController)

            toolbarAvatar.setOnClickListener { view ->
                PopupMenu(view.context, view).apply {
                    inflate(R.menu.menu_main)
                    val isAuthenticated = auth.authStateFlow.value.token != null
                    menu.let {
                        it.setGroupVisible(R.id.unauthenticated, !isAuthenticated)
                        it.setGroupVisible(R.id.authenticated, isAuthenticated)
                    }
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.sign_in -> {
                                navController.navigate(R.id.authenticationFragment)
                                true
                            }

                            R.id.sign_up -> {
                                navController.navigate(R.id.registrationFragment)
                                true
                            }

                            R.id.profile -> {
                                val userId = auth.authStateFlow.value.id
                                val action = NavGraphDirections.actionToDetailedUserFragment(userId)
                                navController.navigate(action)
                                true
                            }

                            R.id.sign_out -> {
                                showConfirmationDialog(
                                    title = getString(R.string.exiting_the_app),
                                    message = getString(R.string.are_you_sure_you_want_to_get_out),
                                    onConfirm = {
                                        auth.removeAuth()
                                    },
                                    positive = getString(R.string.sign_out)
                                )
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            navController.addOnDestinationChangedListener { _, destination, arguments ->
                toolbar.menu.clear()
                toolbar.setOnMenuItemClickListener(null)
                when (destination.id) {
                    R.id.postsFragment, R.id.eventsFragment, R.id.usersFragment ->
                        showBottomNavAndAvatar(bottomNavigation)

                    R.id.detailedPostFragment -> {
                        hideBottomNavAndAvatar(bottomNavigation)
                        toolbar.inflateMenu(R.menu.menu_share)
                        toolbar.setOnMenuItemClickListener { item ->
                            if (item.itemId == R.id.action_share) {
                                val args =
                                    arguments?.let { DetailedPostFragmentArgs.fromBundle(it) }
                                val content = args?.postContent ?: ""
                                if (content.isNotBlank()) shareIntent(content)
                                true
                            } else false
                        }
                    }

                    R.id.detailedEventFragment -> {
                        hideBottomNavAndAvatar(bottomNavigation)
                        toolbar.inflateMenu(R.menu.menu_share)
                        toolbar.setOnMenuItemClickListener { item ->
                            if (item.itemId == R.id.action_share) {
                                val args =
                                    arguments?.let { DetailedEventFragmentArgs.fromBundle(it) }
                                val content = args?.eventContent ?: ""
                                if (content.isNotBlank()) shareIntent(content)
                                true
                            } else false
                        }
                    }

                    R.id.newPostFragment, R.id.newEventFragment, R.id.mapFragment, R.id.newJobFragment -> {
                        hideBottomNavAndAvatar(bottomNavigation)
                        toolbar.inflateMenu(R.menu.menu_save)
                        toolbar.setOnMenuItemClickListener { item ->
                            if (item.itemId == R.id.action_save) {
                                val currentFragment =
                                    navHostFragment.childFragmentManager.primaryNavigationFragment
                                (currentFragment as? OnSaveActionListener)?.onSaveAction()
                                true
                            } else false
                        }
                    }

                    R.id.detailedUsersListFragment -> {
                        hideBottomNavAndAvatar(bottomNavigation)
                        val args = arguments?.let { UsersFragmentArgs.fromBundle(it) }
                        val mode = args?.fragmentMode ?: FragmentMode.USERS
                        toolbar.setTitle(mode.titleRes)
                        if (mode == FragmentMode.SELECTED) {
                            toolbar.inflateMenu(R.menu.menu_save)
                            toolbar.setOnMenuItemClickListener { item ->
                                if (item.itemId == R.id.action_save) {
                                    val currentFragment =
                                        navHostFragment.childFragmentManager.primaryNavigationFragment
                                    (currentFragment as? OnSaveActionListener)?.onSaveAction()
                                    true
                                } else false
                            }
                        }
                    }

                    R.id.detailedUserFragment -> {
                        hideBottomNavAndAvatar(bottomNavigation)
                        val args = arguments?.let { DetailedUserFragmentArgs.fromBundle(it) }
                        val isMyProfile = args?.userId == auth.authStateFlow.value.id
                        toolbar.title = when {
                            isMyProfile -> getString(R.string.you)
                            else -> ""
                        }
                    }

                    else -> hideBottomNavAndAvatar(bottomNavigation)
                }
            }
        }
    }

    fun setToolbarUserTitle(title: String, isMyProfile: Boolean) {
        binding.toolbar.title = if (isMyProfile) getString(R.string.you) else title
    }

    private fun observes() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                auth.authStateFlow.collectLatest { authState ->
                    binding.toolbarAvatar.load(authState.avatar, true)
                }
            }
        }
    }

    private fun hideBottomNavAndAvatar(bottomNav: BottomNavigationView) {
        binding.toolbarAvatar.isVisible = false
        bottomNav.isVisible = false
    }

    private fun showBottomNavAndAvatar(bottomNav: BottomNavigationView) {
        binding.toolbarAvatar.isVisible = true
        if (!bottomNav.isVisible) {
            bottomNav.animate().cancel()
            bottomNav.isVisible = true
            bottomNav.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    override fun onDestroy() {
        binding.bottomNavigation.animate().cancel()
        super.onDestroy()
    }
}