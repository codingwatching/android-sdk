package com.qonversion.android.sdk.ad

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.LinkedBlockingQueue

class AdvertisingProvider {

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw IllegalStateException("Cannot be called from the main thread")
        }

        val connection = AdvertisingConnection()
        val intent = Intent("com.google.android.gms.ads.identifier.service.START").apply {
            setPackage("com.google.android.gms")
        }
        if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            throw IllegalStateException("Binding to advertising id service failed")
        }

        try {
            AdvertisingInterface(connection.binder).id ?: ""
        } finally {
            context.unbindService(connection)
        }
    }

    class AdvertisingConnection : ServiceConnection {
        private var retrieved = false
        private val queue = LinkedBlockingQueue<IBinder>(1)

        internal val binder: IBinder
            @Throws(InterruptedException::class)
            get() {
                if (this.retrieved) throw IllegalStateException()
                this.retrieved = true
                return this.queue.take() as IBinder
            }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            try {
                this.queue.put(service)
            } catch (localInterruptedException: InterruptedException) {
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    class AdvertisingInterface(private val binder: IBinder) : IInterface {

        val id: String?
            @Throws(RemoteException::class)
            get() {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                val id: String?
                try {
                    data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService")
                    binder.transact(1, data, reply, 0)
                    reply.readException()
                    id = reply.readString()
                } finally {
                    reply.recycle()
                    data.recycle()
                }
                return id
            }

        override fun asBinder(): IBinder {
            return binder
        }
    }
}
