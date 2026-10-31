package ir.smartcallcrm.admin

import android.app.admin.DeviceAdminReceiver

/**
 * Lets the app be provisioned as **device owner** via ADB:
 *
 *   adb shell dpm set-device-owner ir.smartcallcrm/.admin.DeviceOwnerReceiver
 *
 * Once device owner, the app can install updates completely silently (no
 * prompt) and cannot be uninstalled until device-owner is removed. See
 * docs/AUTO_UPDATE.md.
 */
class DeviceOwnerReceiver : DeviceAdminReceiver()
