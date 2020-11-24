/*
 * Copyright 2020 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.punchthrough.blestarterappandroid.ble.ConnectionEventListener
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import com.punchthrough.blestarterappandroid.ble.isIndicatable
import com.punchthrough.blestarterappandroid.ble.isNotifiable
import com.punchthrough.blestarterappandroid.ble.isReadable
import com.punchthrough.blestarterappandroid.ble.isWritable
import com.punchthrough.blestarterappandroid.ble.isWritableWithoutResponse
import com.punchthrough.blestarterappandroid.ble.toHexString
import kotlinx.android.synthetic.main.activity_ble_operations.characteristics_recycler_view
import kotlinx.android.synthetic.main.activity_ble_operations.log_scroll_view
import kotlinx.android.synthetic.main.activity_ble_operations.log_text_view
import kotlinx.android.synthetic.main.activity_ble_operations_2.liquidLevelText
import kotlinx.android.synthetic.main.activity_ble_operations_2.liquidLevelImg
import kotlinx.android.synthetic.main.row_scan_result.view.mac_address
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.selector
import org.jetbrains.anko.yesButton
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BleOperationsActivity2 : AppCompatActivity() {

    private lateinit var device: BluetoothDevice

    private var notifyingCharacteristics = mutableListOf<UUID>()



    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        ConnectionManager.registerListener(connectionEventListener)

        super.onCreate(savedInstanceState)

        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")

        setContentView(R.layout.activity_ble_operations_2)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            //title = getString(R.string.ble_playground)
            title = device.name
        }
        for (bleChar in characteristics) {
            val bleCharUUID: UUID = bleChar.uuid
            if (bleCharUUID.toString() == "966499db-e864-4b73-bbda-95e6bac02da1") {
                Timber.i("Found Characteristic UUID: $bleCharUUID")
                ConnectionManager.enableNotifications(device, bleChar)
            }
        }
    }

    override fun onDestroy() {
        ConnectionManager.teardownConnection(device)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {

            onCharacteristicChanged = { _, characteristic ->
                Timber.i("Value changed on ${characteristic.uuid}: ${characteristic.value.toHexString()}")
                runOnUiThread {
                    val liquidLevelInt = Integer.decode(characteristic.value.toHexString())
                    liquidLevelText.text = "$liquidLevelInt"

                    val waterLevelDrawable = when (liquidLevelInt) {
                        in 0..9 -> R.drawable.water_0
                        in 10..19 -> R.drawable.water_1
                        in 20..29 -> R.drawable.water_2
                        in 30..39 -> R.drawable.water_3
                        in 40..49 -> R.drawable.water_4
                        in 50..59 -> R.drawable.water_5
                        in 60..69 -> R.drawable.water_6
                        in 70..79 -> R.drawable.water_7
                        in 80..89 -> R.drawable.water_8
                        in 90..99 -> R.drawable.water_9
                        100 -> R.drawable.water_10
                        else -> R.drawable.water_0
                    }

                    liquidLevelImg.setImageResource(waterLevelDrawable)
                }
            }

            onNotificationsEnabled = { _, characteristic ->
                Timber.i("Enabled notifications on ${characteristic.uuid}")
                notifyingCharacteristics.add(characteristic.uuid)
            }

            onNotificationsDisabled = { _, characteristic ->
                Timber.i("Disabled notifications on ${characteristic.uuid}")
                notifyingCharacteristics.remove(characteristic.uuid)
            }
        }
    }
}