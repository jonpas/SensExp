package com.jonpas.sensexp

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


class MQTT(context: Context) {
    companion object {
        private var TAG: String = MQTT::class.java.simpleName

        private const val SERVER_URI: String = BuildConfig.MQTT_SERVER_URI
        private const val USERNAME: String = BuildConfig.MQTT_USERNAME
        private const val PASSWORD: String = BuildConfig.MQTT_PASSWORD
        private const val PUBLISH_TOPIC: String = BuildConfig.MQTT_PUBLISH_TOPIC
    }

    private var client: MqttAndroidClient
    private var clientId = MqttClient.generateClientId()
    private var qos: Int = 0

    init {
        client = MqttAndroidClient(context, SERVER_URI, clientId)
        client.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(b: Boolean, s: String) {
                Log.i(TAG, s)
            }

            override fun connectionLost(throwable: Throwable) {}

            @Throws(Exception::class)
            override fun messageArrived(topic: String, mqttMessage: MqttMessage) {
                Log.i(TAG, mqttMessage.toString())
            }

            override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {}
        })
    }

    fun setCallback(callback: MqttCallbackExtended) {
        client.setCallback(callback)
    }

    fun connect() {
        val connectOptions = MqttConnectOptions()
        connectOptions.mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
        connectOptions.isAutomaticReconnect = true
        connectOptions.userName = USERNAME
        connectOptions.password = PASSWORD.toCharArray()

        try {
            client.connect(connectOptions, null, object: IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    val disconnectedBufferOpts = DisconnectedBufferOptions()
                    disconnectedBufferOpts.isBufferEnabled = true
                    disconnectedBufferOpts.bufferSize = 100
                    disconnectedBufferOpts.isPersistBuffer = false
                    disconnectedBufferOpts.isDeleteOldestMessages = false
                    client.setBufferOpts(disconnectedBufferOpts)
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.w(TAG, "Failed to connect to: $SERVER_URI\n$exception")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace();
        }
    }

    fun disconnect() {
        try {
            client.close()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun publish(message: String, topic: String = PUBLISH_TOPIC) {
        if (client.isConnected) {
            try {
                val mqttMessage = MqttMessage(message.toByteArray())
                mqttMessage.qos = qos
                client.publish(topic, mqttMessage)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    // Check if configuration is properly defined (not default empty)
    fun isValid(): Boolean {
        return SERVER_URI.isNotBlank()
    }
}
