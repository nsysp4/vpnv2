/*******************************************************************************
 *                                                                             *
 *  Copyright (C) 2017 by Max Lv <max.c.lv@gmail.com>                          *
 *  Copyright (C) 2017 by Mygod Studio <contact-shadowsocks-android@mygod.be>  *
 *                                                                             *
 *  This program is free software: you can redistribute it and/or modify       *
 *  it under the terms of the GNU General Public License as published by       *
 *  the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                        *
 *                                                                             *
 *  This program is distributed in the hope that it will be useful,            *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *  GNU General Public License for more details.                               *
 *                                                                             *
 *  You should have received a copy of the GNU General Public License          *
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                             *
 *******************************************************************************/

package com.github.shadowsocks.bg

import android.app.Service
import android.content.Intent
import android.util.Log
import com.github.shadowsocks.preference.DataStore
import org.bannedbook.app.service.NativeCall
import java.io.*
/**
 * Shadowsocks service at its minimum.
 */
open class ProxyService : Service(), BaseService.Interface {
    companion object {
        var polipoThread : Thread? = null
    }
    override val data = BaseService.Data(this)
    override val tag: String get() = "ShadowsocksProxyService"
    override fun createNotification(profileName: String): ServiceNotification =
            ServiceNotification(this, profileName, "service-proxy", true)

    override fun onBind(intent: Intent) = super.onBind(intent)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
            super<BaseService.Interface>.onStartCommand(intent, flags, startId)
    override fun onDestroy() {
        super.onDestroy()
        data.binder.close()
        //??????app???so???????????????????????????app???????????????????????????????????????????????????
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("polipo")   //defaultConfig.ndk.moduleName
        Log.e("ProxyService", "onCreate...")
        val rootDataDir = filesDir
        val toPath = rootDataDir.toString()

        var confFilename = "config-v2.conf"
        if (DataStore.listenAddress=="0.0.0.0")confFilename = "configLanShare-v2.conf"

        var configFile = "$toPath/$confFilename"
        Log.e("configFile is: ", configFile)

        val file = File(configFile)
        if (!file.exists()) copyAssets(confFilename, toPath)
        if (polipoThread==null) {
            polipoThread = Thread(Runnable { NativeCall.execPolipo(configFile) })
            polipoThread?.start()
        }
    }

    private fun copyAssets(configFile: String, toPath: String) {
        val assetManager = assets
        Log.e("---", "Copy file:  $configFile")
        var `in`: InputStream? = null
        var out: OutputStream? = null
        try {
            `in` = assetManager.open(configFile)
            val outFile = File(toPath, configFile)
            out = FileOutputStream(outFile)
            copyFile(`in`, out)
            `in`.close()
            `in` = null
            out.flush()
            out.close()
            out = null
        } catch (e: IOException) {
            Log.e("tag", "Failed to copy asset file: $configFile", e)
        }
    }

    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while (`in`.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
    }
}
