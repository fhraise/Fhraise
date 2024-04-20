/*
 * This file is part of Fhraise.
 * Copyright (c) 2024 HSAS Foodies. All Rights Reserved.
 *
 * Fhraise is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Fhraise is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Fhraise. If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.xfqlittlefan.fhraise.browser

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.xfqlittlefan.fhraise.R
import xyz.xfqlittlefan.fhraise.activity.FhraiseActivity

class BrowserActivity : FhraiseActivity() {
    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_INTERFACE = "interface"
    }

    private var shouldClose = false

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requiredId = intent.getStringExtra(EXTRA_ID) ?: run { finish(); return }

        lifecycleScope.launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
            browserFlow.collect(requiredId) { message ->
                withContext(Dispatchers.Main) {
                    when (message) {
                        is BrowserMessage.Launch -> message.run { launch() }

                        BrowserMessage.Close -> {
                            startActivity(Intent(this@BrowserActivity, this@BrowserActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            })
                        }

                        else -> Unit
                    }
                }
            }
        }

        browserFlow.tryEmit(requiredId to BrowserMessage.Ready)

        if (!intent?.extras?.getBoolean(EXTRA_INTERFACE, true)!!) return

        setContent {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = stringResource(R.string.browser_title)) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        },
                    )
                },
                contentWindowInsets = WindowInsets.safeDrawing,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(it),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = stringResource(R.string.browser_message))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (shouldClose) finish()
        shouldClose = true
    }
}
