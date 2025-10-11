package com.orderfast

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.*
import com.easygoband.commons.unsigned.Uint
import com.easygoband.nfc.tags.reader.FeedbackType
import com.easygoband.toolkit.commons.type.Reference
import com.easygoband.toolkit.sdk.android.ToolkitBuilder
import com.easygoband.toolkit.sdk.bundle.components.Toolkit
import com.easygoband.toolkit.sdk.bundle.handlers.AddOrderToTagHandler
import com.easygoband.toolkit.sdk.bundle.handlers.AddRechargeToTagHandler
import com.easygoband.toolkit.sdk.bundle.handlers.GetUserByTagHandler
import com.easygoband.toolkit.sdk.bundle.handlers.TagHandler
import com.easygoband.toolkit.sdk.core.transaction.recharge.usecase.AddRecharge
import com.easygoband.toolkit.sdk.core.transaction.transaction.data.SyncTransactionsMode
import com.easygoband.toolkit.sdk.core.utils.Binary
import com.orderfast.ToolkitProvider
import com.orderfast.workers.ToolkitSyncWorker
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * GoFunBand Flutter Plugin
 *
 * Plugin para integración del SDK de GoFunBand (EasyGoBand) con Flutter.
 * Permite la gestión de pulseras NFC, lectura de tags, recargas y sincronización.
 *
 * Features:
 * - Inicialización y configuración del toolkit
 * - Lectura de tags NFC
 * - Recargas de saldo
 * - Sincronización automática en segundo plano (cada 15 minutos)
 *
 * @author OrderFast
 * @version 1.1.0
 */
class GoFunBandPlugin : FlutterPlugin, MethodCallHandler {

    companion object {
        private const val TAG = "GoFunBandPlugin"
        private const val CHANNEL_NAME = "gofunband"

        // Method names
        private const val METHOD_INITIALIZE = "initializeToolkit"
        private const val METHOD_CONFIGURE = "configureDevice"
        private const val METHOD_CHECK_READER = "checkAvailableReader"
        private const val METHOD_SYNC = "syncToolkitData"
        private const val METHOD_REMOVE_HANDLERS = "removeHandlers"
        private const val METHOD_START_READER = "startReader"
        private const val METHOD_STOP_READER = "stopReader"
        private const val METHOD_ADD_RECHARGE = "addRecharge"
        private const val METHOD_GET_TAG_INFO = "getTagInfo"
        private const val METHOD_IS_DEVICE_CONFIGURED = "isDeviceConfigured"
        private const val METHOD_START_AUTO_SYNC = "startAutoSync"
        private const val METHOD_STOP_AUTO_SYNC = "stopAutoSync"
        private const val METHOD_SHUTDOWN = "shutdownToolkit"
        private const val METHOD_IS_INITIALIZED = "isToolkitInitialized"
        private const val METHOD_ADD_RECHARGE_TO_USER_ID = "addRechargeToUserId"

        // Callback method names
        private const val CALLBACK_TAG_READ = "onTagRead"
        private const val CALLBACK_TAG_ERROR = "onTagReadError"
        private const val CALLBACK_RECHARGE_SUCCESS = "onRechargeSuccess"
        private const val CALLBACK_RECHARGE_ERROR = "onRechargeError"

        // Error codes
        private const val ERROR_INIT = "INIT_ERROR"
        private const val ERROR_CONFIGURE = "CONFIGURE_ERROR"
        private const val ERROR_READER_NOT_AVAILABLE = "READER_NOT_AVAILABLE"
        private const val ERROR_NOT_INITIALIZED = "NOT_INITIALIZED"
        private const val ERROR_RECHARGE = "RECHARGE_ERROR"
        private const val ERROR_INVALID_PARAMS = "INVALID_PARAMS"
    }

    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var getUserByTagHandler: GetUserByTagHandler? = null
    private var addRechargeToTagHandler: AddRechargeToTagHandler? = null
    private var addOrderToTagHandler: AddOrderToTagHandler? = null


    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
        context = binding.applicationContext
        Log.d(TAG, "Plugin attached to engine")
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        Log.d(TAG, "Method called: ${call.method}")

        when (call.method) {
            METHOD_INITIALIZE -> handleInitialize(call, result)
            METHOD_CONFIGURE -> handleConfigure(call, result)
            METHOD_CHECK_READER -> handleCheckReader(result)
            METHOD_SYNC -> handleSync(result)
            METHOD_REMOVE_HANDLERS -> handleRemoveHandlers(result)
            METHOD_START_READER -> handleStartReader(result)
            METHOD_STOP_READER -> handleStopReader(result)
            METHOD_ADD_RECHARGE -> handleAddRecharge(call, result)
            METHOD_GET_TAG_INFO -> handleGetTagInfo(result)
            METHOD_IS_DEVICE_CONFIGURED -> handleIsDeviceConfigured(result)
            METHOD_START_AUTO_SYNC -> handleStartAutoSync(result)
            METHOD_STOP_AUTO_SYNC -> handleStopAutoSync(result)
            METHOD_ADD_RECHARGE_TO_USER_ID -> handleAddRechargeToUserId(call, result)
            METHOD_SHUTDOWN -> {
                shutdownToolkit()
                result.success(true)
            }

            METHOD_IS_INITIALIZED -> result.success(isToolkitInitialized())

            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        stopAutoSync()
        shutdownToolkit()
        Log.d(TAG, "Plugin detached from engine")
    }

    // ============================================================
    // Method Handlers
    // ============================================================

    /**
     * Inicializa el toolkit con el entorno especificado
     */
    private fun handleInitialize(call: MethodCall, result: Result) {
        val env = call.argument<String>("environment") ?: "SANDBOX"
        val readerType = call.argument<String>("readerType") ?: "ACS"
        val autoSync = call.argument<Boolean>("autoSync") ?: true

        try {
            val toolkit = initializeToolkit(env, readerType)

            // Guarda la referencia en el provider para el Worker
            ToolkitProvider.setToolkit(toolkit)

            // Inicia sincronización automática si está habilitada
            if (autoSync) {
                startAutoSync()
            }

            Log.d(TAG, "Toolkit initialized successfully (autoSync: $autoSync)")
            result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing toolkit: ${e.message}", e)
            result.error(ERROR_INIT, "Failed to initialize toolkit: ${e.message}", null)
        }
    }

    /**
     * Configura el dispositivo con la API key
     */
    private fun handleConfigure(call: MethodCall, result: Result) {
        if (!isToolkitInitialized()) {
            result.error(ERROR_NOT_INITIALIZED, "Toolkit not initialized", null)
            return
        }

        val apiKey = call.argument<String>("apiKey")
        if (apiKey.isNullOrEmpty()) {
            result.error(ERROR_INVALID_PARAMS, "API key is required", null)
            return
        }

        val success = configureDevice(apiKey)
        if (success) {
            result.success(true)
        } else {
            result.error(ERROR_CONFIGURE, "Failed to configure device", null)
        }
    }

    /**
     * Verifica si hay un lector disponible
     */
    private fun handleCheckReader(result: Result) {
        if (!isToolkitInitialized()) {
            result.error(ERROR_NOT_INITIALIZED, "Toolkit not initialized", null)
            return
        }

        val isAvailable = checkAvailableReader()
        result.success(isAvailable)
    }

    /**
     * Sincroniza los datos con el servidor (manual)
     */
    private fun handleSync(result: Result) {
        if (!isToolkitInitialized()) {
            result.error(ERROR_NOT_INITIALIZED, "Toolkit not initialized", null)
            return
        }

        syncToolkit()
        result.success(true)
    }

    /**
     * Remueve todos los handlers activos
     */
    private fun handleRemoveHandlers(result: Result) {
        if (!isToolkitInitialized()) {
            result.error(ERROR_NOT_INITIALIZED, "Toolkit not initialized", null)
            return
        }

        removeHandlers()
        Log.d(TAG, "Handlers removed")
        result.success(true)
    }

    /**
     * Inicia el lector de tags
     */
    private fun handleStartReader(result: Result) {
        if (!isToolkitInitialized()) {
            result.error(ERROR_NOT_INITIALIZED, "Toolkit not initialized", null)
            return
        }

        if (!checkAvailableReader()) {
            result.error(ERROR_READER_NOT_AVAILABLE, "No reader attached", null)
            return
        }

        setTagReadHandler()
        Log.d(TAG, "Reader started")
        result.success(true)
    }

    /**
     * Detiene el lector de tags
     */
    private fun handleStopReader(result: Result) {
        if (!isToolkitInitialized()) {
            result.error(ERROR_NOT_INITIALIZED, "Toolkit not initialized", null)
            return
        }

        ToolkitProvider.getToolkit()?.instance()?.removeHandler()
        Log.d(TAG, "Reader stopped")
        result.success(true)
    }

    /**
     * Añade una recarga a un tag
     */
    private fun handleAddRecharge(call: MethodCall, result: Result) {
        if (!isToolkitInitialized()) {
            result.error(ERROR_NOT_INITIALIZED, "Toolkit not initialized", null)
            return
        }

        val amount = call.argument<Int>("amount")
        val concept = call.argument<String>("concept") ?: "CASH"
        val origin = call.argument<String>("origin") ?: "OrderFast"
        val reference = call.argument<String>("reference")

        if (amount == null || amount <= 0) {
            result.error(ERROR_INVALID_PARAMS, "Invalid amount", null)
            return
        }

        try {
            addRechargeToTagHandler(
                amount = Uint(amount),
                concept = concept,
                origin = origin,
                reference = reference
            )
            result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding recharge: ${e.message}", e)
            result.error(ERROR_RECHARGE, "Failed to add recharge: ${e.message}", null)
        }
    }

    /**
     * Obtiene información del último tag leído
     */
    private fun handleGetTagInfo(result: Result) {
        if (!isToolkitInitialized()) {
            result.error(ERROR_NOT_INITIALIZED, "Toolkit not initialized", null)
            return
        }

        result.error("NOT_IMPLEMENTED", "Feature not yet implemented", null)
    }

    /**
     * Verifica si el dispositivo está configurado
     */
    private fun handleIsDeviceConfigured(result: Result) {
        if (!isToolkitInitialized()) {
            result.success(false)
            return
        }

        val isConfigured = ToolkitProvider.getToolkit()?.instance()?.isDeviceConfigured() ?: false
        result.success(isConfigured)
    }

    /**
     * Inicia la sincronización automática en segundo plano
     */
    private fun handleStartAutoSync(result: Result) {
        try {
            startAutoSync()
            result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting auto sync: ${e.message}", e)
            result.error("AUTO_SYNC_ERROR", "Failed to start auto sync: ${e.message}", null)
        }
    }

    /**
     * Detiene la sincronización automática
     */
    private fun handleStopAutoSync(result: Result) {
        try {
            stopAutoSync()
            result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping auto sync: ${e.message}", e)
            result.error("AUTO_SYNC_ERROR", "Failed to stop auto sync: ${e.message}", null)
        }
    }

    private fun handleAddRechargeToUserId(call: MethodCall, result: Result) {
        if (!isToolkitInitialized()) {
            result.error(ERROR_NOT_INITIALIZED, "Toolkit not initialized", null)
            return
        }

        val amount = call.argument<Int>("amount")
        val concept = call.argument<String>("concept") ?: "CASH"
        val origin = call.argument<String>("origin") ?: "OrderFast"
        val reference = call.argument<String>("reference")
        val userIdStr = call.argument<String>("userId")

        if (amount == null || amount <= 0 || userIdStr.isNullOrEmpty()) {
            result.error(ERROR_INVALID_PARAMS, "Invalid amount or userId", null)
            return
        }

        try {
            val userId = UUID.fromString(userIdStr)
            val response = addRechargeToUserId(
                amount = Uint(amount),
                concept = concept,
                origin = origin,
                reference = reference,
                userId = userId
            )

            Log.d(TAG, "Recharge added to user ID successfully: $response")

            result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding recharge to user ID: ${e.message}", e)
            result.error(ERROR_RECHARGE, "Failed to add recharge: ${e.message}", null)
        }
    }

    // ============================================================
    // Core Functions
    // ============================================================

    /**
     * Inicializa el toolkit con la configuración especificada
     */
    private fun initializeToolkit(env: String, readerType: String): Toolkit {
        val environment = when (env.uppercase()) {
            "PRODUCTION" -> ToolkitBuilder.Environment.PRODUCTION
            else -> ToolkitBuilder.Environment.SANDBOX
        }

        val reader = when (readerType.uppercase()) {
            "ACS" -> ToolkitBuilder.ReaderType.ACS
            else -> ToolkitBuilder.ReaderType.ACS
        }

        return ToolkitBuilder()
            .withApplication(context.applicationContext as Application)
            .withEnvironment(environment)
            .withTagType(Toolkit.TagType.MIFARE_EVENT_ULTRALIGHTC)
            .withReader(reader)
            .build()
    }

    /**
     * Configura el dispositivo con la API key proporcionada
     */
    private fun configureDevice(apiKey: String): Boolean {
        return try {
            val toolkitInstance = ToolkitProvider.getToolkit()?.instance() ?: return false

            if (!toolkitInstance.isDeviceConfigured()) {
                toolkitInstance.configure(apiKey)
                Log.d(TAG, "Device configured successfully")
            } else {
                Log.d(TAG, "Device already configured")
            }
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Error configuring device: ${e.message}", e)
            false
        }
    }

    /**
     * Sincroniza los datos del toolkit con el servidor
     */
    private fun syncToolkit() {
        try {
            Log.d(TAG, "Syncing toolkit data...")
            ToolkitProvider.getToolkit()?.instance()?.sync(false, SyncTransactionsMode.DEVICE, true)
            Log.d(TAG, "Sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing toolkit data: ${e.message}", e)
        }
    }

    /**
     * Verifica si hay un lector NFC disponible
     */
    private fun checkAvailableReader(): Boolean {
        val isAttached = ToolkitProvider.getToolkit()?.instance()?.isReaderAttached() ?: false
        if (!isAttached) {
            Log.d(TAG, "No reader is attached")
        }
        return isAttached
    }

    /**
     * Configura el handler para lectura de tags
     */
    private fun setTagReadHandler() {
        Log.d(TAG, "Setting tag read handler...")
        getUserByTagHandler = ToolkitProvider.getToolkit()?.getUserByTagHandler() ?: return

        getUserByTagHandler!!.onSucceed { user ->
            ToolkitProvider.getToolkit()?.reader()?.sound(FeedbackType.ON_SUCCESS)
            try {
                Log.d(TAG, "Tag read successfully - User ID: ${user.tagUser.id}")

                val userData = mapOf(
                    "userId" to user.tagUser.id.toString(),
                    "userBalance" to user.tagUser.balance,
                    "reference" to (user.tagUser.reference ?: ""),
                    "tagId" to Binary.bytesToString(user.tagUser.tagId.id),
                    "category" to (user.tagUser.category?.toInt() ?: 0),
                    "isPaid" to (user.tagUser.tagPaid ?: false),
                )

                channel.invokeMethod(CALLBACK_TAG_READ, userData)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing user data: ${e.message}", e)
                channel.invokeMethod(CALLBACK_TAG_ERROR, e.message ?: "Unknown error")
            }
        }

        getUserByTagHandler!!.onError { exception ->
            Log.e(TAG, "Error reading tag: ${exception.message}", exception)
            ToolkitProvider.getToolkit()?.reader()?.sound(FeedbackType.ON_ERROR)
            channel.invokeMethod(
                CALLBACK_TAG_ERROR,
                mapOf(
                    "error" to (exception.message ?: "Unknown error"),
                    "type" to exception.javaClass.simpleName
                )
            )
        }

        ToolkitProvider.getToolkit()?.instance()?.setHandler(getUserByTagHandler!!)
    }

    /**
     * Configura el handler para añadir recargas a tags
     */
    private fun addRechargeToTagHandler(
        amount: Uint,
        concept: String = "CASH",
        origin: String = "OrderFast",
        reference: String? = null
    ) {
        Log.d(TAG, "Adding recharge handler - Amount: $amount, Concept: $concept")
        addRechargeToTagHandler = ToolkitProvider.getToolkit()?.addRechargeToTagHandler() ?: return


        addRechargeToTagHandler!!.requestFetcher {
            AddRechargeToTagHandler.Request(
                originalAmount = null,
                currencyId = null,
                concept = concept,
                amount = amount,
                origin = origin,
                attachment = null,
                operator = null,
                reference = Reference(reference ?: "")
            )
        }

        addRechargeToTagHandler!!.onSucceed { response ->
            ToolkitProvider.getToolkit()?.reader()?.sound(FeedbackType.ON_SUCCESS)
            Log.d(TAG, "Recharge added successfully")

            channel.invokeMethod(
                CALLBACK_RECHARGE_SUCCESS,
                mapOf(
                    "amount" to amount.v,
                    "concept" to concept,
                    "reference" to reference
                )
            )
        }

        addRechargeToTagHandler!!.onError { exception ->
            Log.e(TAG, "Error adding recharge: ${exception.message}", exception)
            ToolkitProvider.getToolkit()?.reader()?.sound(FeedbackType.ON_ERROR)

            channel.invokeMethod(
                CALLBACK_RECHARGE_ERROR,
                mapOf(
                    "error" to (exception.message ?: "Unknown error"),
                    "type" to exception.javaClass.simpleName
                )
            )
        }

        ToolkitProvider.getToolkit()?.instance()?.setHandler(addRechargeToTagHandler!!)
    }

    private fun addRechargeToUserId(
        amount: Uint,
        concept: String = "CASH",
        origin: String = "OrderFast",
        reference: String? = null,
        userId: UUID
    ): AddRecharge.Response {
        Log.d(
            TAG,
            "Adding recharge to user ID handler - Amount: $amount, Concept: $concept, UserID: $userId"
        )

        if (ToolkitProvider.getToolkit() == null) {
            throw Exception("Toolkit not initialized")
        }

        return ToolkitProvider.getToolkit()!!.addRechargeCommand().execute(
            AddRecharge.Request(
                userId = userId,
                originalAmount = null,
                currencyId = null,
                concept = concept,
                amount = amount,
                origin = origin,
                attachment = null,
                operator = null,
                reference = Reference(reference ?: "")
            )
        )
    }

    /**
     * Inicia la sincronización automática periódica en segundo plano
     * Se ejecuta cada 15 minutos usando WorkManager
     */
    private fun startAutoSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Solo con internet
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<ToolkitSyncWorker>(
            15, // Cada 15 minutos (mínimo permitido por Android)
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .addTag(ToolkitSyncWorker.TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                ToolkitSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // No duplicar si ya existe
                syncRequest
            )

        Log.d(TAG, "Auto-sync started (every 15 minutes)")
    }

    /**
     * Detiene la sincronización automática
     */
    private fun stopAutoSync() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(ToolkitSyncWorker.WORK_NAME)

        Log.d(TAG, "Auto-sync stopped")
    }

    /**
     * Apaga el toolkit de forma segura
     */
    private fun shutdownToolkit() {
        try {
            if (ToolkitProvider.getToolkit()?.instance()?.isReaderAttached() == true) {
                removeHandlers()

                Log.d(TAG, "Toolkit shutdown successfully")
            }


            ToolkitProvider.getToolkit()?.instance()?.shutdown()
            ToolkitProvider.clearToolkit()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down toolkit: ${e.message}", e)
        }
    }

    /**
     * Verifica si el toolkit está inicializado
     */
    private fun isToolkitInitialized(): Boolean {
        return ToolkitProvider.getToolkit() != null
    }

    private fun removeHandlers() {
        if (getUserByTagHandler != null)
            ToolkitProvider.getToolkit()?.instance()?.removeHandler(getUserByTagHandler)
        if (addOrderToTagHandler != null)
            ToolkitProvider.getToolkit()?.instance()?.removeHandler(addOrderToTagHandler)
        if (addRechargeToTagHandler != null)
            ToolkitProvider.getToolkit()?.instance()?.removeHandler(addRechargeToTagHandler)

        Log.d(TAG, "Handlers removed")
    }
}