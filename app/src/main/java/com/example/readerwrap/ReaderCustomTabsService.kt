package com.example.readerwrap

import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsService
import androidx.browser.customtabs.CustomTabsSessionToken

class ReaderCustomTabsService : CustomTabsService() {

    companion object {
        // Nothing else on-device binds here (News now targets this package
        // explicitly via the patched getBrowserPackage()), so there's no need
        // to verify the caller — accepting any bind is fine.
        var lastSession: CustomTabsSessionToken? = null
    }

    override fun warmup(flags: Long): Boolean = true

    override fun newSession(sessionToken: CustomTabsSessionToken): Boolean {
        lastSession = sessionToken
        return true
    }

    override fun mayLaunchUrl(
        sessionToken: CustomTabsSessionToken,
        url: Uri?,
        extras: Bundle?,
        otherLikelyBundles: MutableList<Bundle>?
    ): Boolean = true

    override fun extraCommand(commandName: String, args: Bundle?): Bundle? = null

    override fun updateVisuals(sessionToken: CustomTabsSessionToken, bundle: Bundle?): Boolean = true

    override fun requestPostMessageChannel(
        sessionToken: CustomTabsSessionToken,
        postMessageOrigin: Uri
    ): Boolean = true

    override fun postMessage(
        sessionToken: CustomTabsSessionToken,
        message: String,
        extras: Bundle?
    ): Int = 0

    override fun validateRelationship(
        sessionToken: CustomTabsSessionToken,
        relation: Int,
        origin: Uri,
        extras: Bundle?
    ): Boolean = true

    override fun receiveFile(
        sessionToken: CustomTabsSessionToken,
        uri: Uri,
        purpose: Int,
        extras: Bundle?
    ): Boolean = false

    override fun cleanUpSession(sessionToken: CustomTabsSessionToken): Boolean {
        if (lastSession == sessionToken) lastSession = null
        return super.cleanUpSession(sessionToken)
    }
}
