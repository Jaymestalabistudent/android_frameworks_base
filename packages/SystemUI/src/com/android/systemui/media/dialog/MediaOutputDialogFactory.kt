/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.media.dialog

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.os.PowerExemptionManager
import android.view.View
import com.android.internal.jank.InteractionJankMonitor
import com.android.internal.logging.UiEventLogger
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.systemui.animation.DialogCuj
import com.android.systemui.animation.DialogTransitionAnimator
import com.android.systemui.broadcast.BroadcastSender
import com.android.systemui.flags.FeatureFlags
import com.android.systemui.media.nearby.NearbyMediaDevicesManager
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.notification.collection.notifcollection.CommonNotifCollection
import javax.inject.Inject

/** Factory to create [MediaOutputDialog] objects. */
open class MediaOutputDialogFactory
@Inject
constructor(
    private val context: Context,
    private val mediaSessionManager: MediaSessionManager,
    private val lbm: LocalBluetoothManager?,
    private val starter: ActivityStarter,
    private val broadcastSender: BroadcastSender,
    private val notifCollection: CommonNotifCollection,
    private val uiEventLogger: UiEventLogger,
    private val dialogTransitionAnimator: DialogTransitionAnimator,
    private val nearbyMediaDevicesManager: NearbyMediaDevicesManager,
    private val audioManager: AudioManager,
    private val powerExemptionManager: PowerExemptionManager,
    private val keyGuardManager: KeyguardManager,
    private val featureFlags: FeatureFlags,
    private val userTracker: UserTracker
) {
    companion object {
        const val INTERACTION_JANK_TAG = "media_output"
        var mediaOutputDialog: MediaOutputDialog? = null
    }

    /** Creates a [MediaOutputDialog] for the given package. */
    open fun create(packageName: String, aboveStatusBar: Boolean, view: View? = null) {
        createWithController(
            packageName,
            aboveStatusBar,
            controller =
                view?.let {
                    DialogTransitionAnimator.Controller.fromView(
                        it,
                        DialogCuj(
                            InteractionJankMonitor.CUJ_SHADE_DIALOG_OPEN,
                            INTERACTION_JANK_TAG
                        )
                    )
                },
        )
    }

    /** Creates a [MediaOutputDialog] for the given package. */
    open fun createWithController(
        packageName: String,
        aboveStatusBar: Boolean,
        controller: DialogTransitionAnimator.Controller?,
    ) {
        create(
            packageName,
            aboveStatusBar,
            dialogTransitionAnimatorController = controller,
            includePlaybackAndAppMetadata = true
        )
    }

    open fun createDialogForSystemRouting(controller: DialogTransitionAnimator.Controller? = null) {
        create(
            packageName = null,
            aboveStatusBar = false,
            dialogTransitionAnimatorController = null,
            includePlaybackAndAppMetadata = false
        )
    }

    private fun create(
        packageName: String?,
        aboveStatusBar: Boolean,
        dialogTransitionAnimatorController: DialogTransitionAnimator.Controller?,
        includePlaybackAndAppMetadata: Boolean = true
    ) {
        // Dismiss the previous dialog, if any.
        mediaOutputDialog?.dismiss()

        val controller =
            MediaOutputController(
                context,
                packageName,
                mediaSessionManager,
                lbm,
                starter,
                notifCollection,
                dialogTransitionAnimator,
                nearbyMediaDevicesManager,
                audioManager,
                powerExemptionManager,
                keyGuardManager,
                featureFlags,
                userTracker
            )
        val dialog =
            MediaOutputDialog(
                context,
                aboveStatusBar,
                broadcastSender,
                controller,
                dialogTransitionAnimator,
                uiEventLogger,
                includePlaybackAndAppMetadata
            )
        mediaOutputDialog = dialog

        // Show the dialog.
        if (dialogTransitionAnimatorController != null) {
            dialogTransitionAnimator.show(
                dialog,
                dialogTransitionAnimatorController,
            )
        } else {
            dialog.show()
        }
    }

    /** dismiss [MediaOutputDialog] if exist. */
    open fun dismiss() {
        mediaOutputDialog?.dismiss()
        mediaOutputDialog = null
    }
}
