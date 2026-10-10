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
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.ActivityAppBinding
import ru.netology.nework.db.AppDb
import ru.netology.nework.extensions.load
import ru.netology.nework.extensions.shareIntent
import ru.netology.nework.extensions.showConfirmationDialog
import ru.netology.nework.fragment.DetailedPostFragmentArgs
import javax.inject.Inject

@AndroidEntryPoint
class AppActivity : AppCompatActivity(R.layout.activity_app) {
    @Inject
    lateinit var auth: AppAuth

    @Inject
    lateinit var appDb: AppDb
    private val binding by viewBinding(ActivityAppBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        setupMenu()
        observes()
    }

    private fun setupMenu() {
        with(binding) {

//          findNavController(R.id.nav_host_fragment).navigate() TODO

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
                            R.id.signin -> {
                                navController.navigate(R.id.authenticationFragment)
                                true
                            }

                            R.id.signup -> {
                                navController.navigate(R.id.registrationFragment)
                                true
                            }

                            R.id.signout -> {
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

            navController.addOnDestinationChangedListener { controller, destination, arguments ->
                toolbar.menu.clear()
                toolbar.setOnMenuItemClickListener(null)
                when (destination.id) {
                    R.id.postsFragment, R.id.eventsFragment, R.id.usersFragment ->
                        showBottomNavAndAvatar(bottomNavigation)

                    R.id.detailedPostFragment, R.id.detailedEventFragment -> {
                        hideBottomNavAndAvatar(bottomNavigation)
                        toolbar.inflateMenu(R.menu.menu_share)
                        toolbar.setOnMenuItemClickListener { item ->
                            if (item.itemId == R.id.action_share) {
                                val args = arguments?.let { DetailedPostFragmentArgs.fromBundle(it) }
                                val content = args?.postContent ?: ""
                                if (content.isNotBlank()) shareIntent(content)
                                true
                            } else false
                        }
                    }

                    R.id.newPostFragment, R.id.newEventFragment -> {
                        hideBottomNavAndAvatar(bottomNavigation)
                        toolbar.inflateMenu(R.menu.menu_save)
                        toolbar.setOnMenuItemClickListener { item ->
                            if (item.itemId == R.id.action_save) {
                                TODO()
                                true
                            } else false
                        }
                    }

                    else -> hideBottomNavAndAvatar(bottomNavigation)
                }
            }
        }
    }

    private fun observes() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                auth.authStateFlow.collectLatest { authState ->
                    val isLoggedIn = authState.token != null
                    if (isLoggedIn && binding.toolbarAvatar.isVisible) {
                        binding.toolbarAvatar.load(authState.avatar, true)
                    } else {
                        binding.toolbarAvatar.setImageResource(R.drawable.ic_avatar_placeholder_48)
                    }
                }
            }
        }
    }

    private fun hideBottomNavAndAvatar(bottomNav: BottomNavigationView) {
        binding.toolbarAvatar.isVisible = false
//        if (bottomNav.isVisible) {
//            bottomNav.animate()
//                .translationY(bottomNav.height.toFloat())
//                .alpha(0f)
//                .setDuration(300)
//                .withEndAction { bottomNav.isVisible = false }
//                .start()
//        }
        bottomNav.isVisible = false
    }

    private fun showBottomNavAndAvatar(bottomNav: BottomNavigationView) {
        binding.toolbarAvatar.isVisible = true
        if (!bottomNav.isVisible) {
            bottomNav.isVisible = true
            bottomNav.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .start()

        }
    }
}