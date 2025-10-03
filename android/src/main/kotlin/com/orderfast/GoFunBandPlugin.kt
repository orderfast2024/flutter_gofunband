package com.orderfast

import android.content.Context
import android.util.Log
import com.easygoband.commons.unsigned.Uint
import com.easygoband.toolkit.sdk.bundle.components.Toolkit
import com.easygoband.toolkit.sdk.bundle.handlers.AddRechargeToTagHandler
import com.easygoband.toolkit.sdk.core.transaction.transaction.data.SyncTransactionsMode
import com.easygoband.toolkit.sdk.desktop.ToolkitBuilder

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.objectbox.BoxStore
import org.slf4j.Logger

/** GoFunBandPlugin */
class GoFunBandPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var toolkit: Toolkit
    private lateinit var context: Context
    private var logger: Logger = org.slf4j.LoggerFactory.getLogger("GoFunBandPlugin")

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "toolkit_flutter")
        channel.setMethodCallHandler(this)
        context = binding.applicationContext

    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "initializeToolkit" -> {
                val env = call.argument<String>("environment") ?: "SANDBOX"

                try {
                    toolkit = initializeToolkit(env)
                    result.success(null)
                } catch (e: Exception) {
                    logger.error("Error initializing toolkit: ${e.message}")
                    result.error("INIT_ERROR", "Failed to initialize toolkit", null)
                }
            }

            "configureDevice" -> {
                val apiKey = call.argument<String>("apiKey") ?: ""
                val success = configureDevice(apiKey)
                if (success) {
                    result.success(null)
                } else {
                    result.error("CONFIGURE_ERROR", "Failed to configure device", null)
                }
            }

            "checkAvailableReader" -> {
                val isAvailable = checkAvailableReader()
                if (isAvailable) {
                    result.success(true)
                } else {
                    result.error("READER_NOT_AVAILABLE", "No reader is attached", null)
                }
            }

            "syncToolkitData" -> {
                syncToolkit()
                result.success(null)
            }

            "removeHandlers" -> {
                toolkit.instance().removeHandler();
                result.success(null)
            }

            "startReader" -> {
                setTagReadHandler();
                result.success(null)
            }


            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)

        try {
            if (toolkit.instance().isReaderAttached()) {
                toolkit.instance().shutdown();
            }
        } catch (e: Exception) {
            Log.e("GoFunBandPlugin", "Error stopping reader: ${e.message}")
        }
    }

    private fun initializeToolkit(env: String): Toolkit {
        val environment =
            if (env == "PRODUCTION") ToolkitBuilder.Environment.PRODUCTION else ToolkitBuilder.Environment.SANDBOX

        return ToolkitBuilder()
            .withEnvironment(environment)
            .withTagType(Toolkit.TagType.MIFARE_EVENT_ULTRALIGHTC)
            .build()
    }

    private fun configureDevice(apiKey: String): Boolean {
        return try {
            if (!toolkit.instance().isDeviceConfigured()) {
                toolkit.instance().configure(apiKey)
            }

            logger.info("Toolkit has been initialized!")
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    private fun syncToolkit() {
        try {
            logger.info("Sync tookit..")
            toolkit.instance().sync(false, SyncTransactionsMode.DEVICE, true)
        } catch (e: Exception) {
            logger.info("Toolkit: Sync error -> " + e.message)
            e.printStackTrace()
        }
    }

    private fun checkAvailableReader(): Boolean {
        return if (!toolkit.instance().isReaderAttached()) {
            logger.info("Reader is not attached")
            false
        } else true
    }

    private fun setTagReadHandler() {
        logger.info("Reading tag...")
        val handler = toolkit.getUserByTagHandler()

        handler.onSucceed { user ->
            try {
                if (user != null) {
                    //Usuario encontrado
                    logger.info("Processing success read tag with userId: " + user.tagUser.id)
                    val userId = user.tagUser.id.toString()
                    val userBalance = user.tagUser.balance;

                    channel.invokeMethod(
                        "onTagRead",
                        mapOf(
                            "userId" to userId,
                            "userBalance" to userBalance
                        )
                    )

                }
            } catch (e: Exception) {
                //El tag no contiene un usuario
                logger.info("Processing success read tag error " + e.message)
                e.printStackTrace()
            }
        }
        handler.onError { exception ->
            //Error de lectura
            logger.info("Reading tag error " + exception.message)
            exception.printStackTrace()
            channel.invokeMethod("onTagReadError", exception.message)
        }

        toolkit.instance().setHandler(handler)
    }

    private fun addRechagerToTagHandler(amount: Uint) {
        logger.info("Reading tag...")
        val handler = toolkit.addRechargeToTagHandler()
        handler.requestFetcher {
            AddRechargeToTagHandler.Request(
                originalAmount = null,
                currencyId = null,
                concept = "CASH", //Admite CASH/CARD/CLAIM
                amount = amount, //Cantidad en céntimos
                origin = "OrderFast",
                attachment = null,
                operator = null,
                reference = null
            )
        }
    }

}
