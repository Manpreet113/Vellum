package com.reader.vellum.util

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.reader.vellum.data.repository.BookRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.call
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.html.*
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanTransferManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val bookParser: BookParser
) {
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning = _isServerRunning.asStateFlow()

    private val _serverAddress = MutableStateFlow<String?>(null)
    val serverAddress = _serverAddress.asStateFlow()

    private val _serverPin = MutableStateFlow<String?>(null)
    val serverPin = _serverPin.asStateFlow()

    private var server: EmbeddedServer<*, *>? = null

    fun startServer() {
        if (_isServerRunning.value) return

        val port = 8080
        val ip = getLocalIpAddress() ?: "127.0.0.1"
        val pin = (1000..9999).random().toString()
        _serverPin.value = pin
        _serverAddress.value = "http://$ip:$port"

        server = embeddedServer(Netty, port = port) {
            routing {
                get("/") {
                    call.respondHtml {
                        head {
                            title { text("Vellum Transfer") }
                            style {
                                unsafe {
                                    raw("""
                                        body {
                                            background-color: #0A0A0A;
                                            color: #FFFFFF;
                                            font-family: 'Inter', sans-serif;
                                            display: flex;
                                            flex-direction: column;
                                            align-items: center;
                                            justify-content: center;
                                            height: 100vh;
                                            margin: 0;
                                        }
                                        .container {
                                            border: 1px solid rgba(255, 255, 255, 0.1);
                                            border-radius: 32px;
                                            padding: 48px;
                                            background: rgba(255, 255, 255, 0.02);
                                            backdrop-filter: blur(40px);
                                            text-align: center;
                                            max-width: 500px;
                                            width: 90%;
                                        }
                                        h1 { letter-spacing: 4px; font-weight: 800; color: #6366F1; }
                                        .drop-zone {
                                            border: 2px dashed rgba(99, 102, 241, 0.3);
                                            border-radius: 24px;
                                            padding: 40px;
                                            margin-top: 32px;
                                            transition: all 0.3s ease;
                                            cursor: pointer;
                                        }
                                        .drop-zone:hover {
                                            background: rgba(99, 102, 241, 0.05);
                                            border-color: #6366F1;
                                        }
                                        input[type="file"] { display: none; }
                                        .status { margin-top: 24px; font-size: 14px; opacity: 0.6; }
                                        .pin-input {
                                            background: rgba(255, 255, 255, 0.05);
                                            border: 1px solid rgba(255, 255, 255, 0.1);
                                            border-radius: 12px;
                                            color: white;
                                            padding: 12px;
                                            margin-top: 16px;
                                            text-align: center;
                                            font-size: 18px;
                                            letter-spacing: 4px;
                                            width: 120px;
                                        }
                                    """)
                                }
                            }
                        }
                        body {
                            div("container") {
                                h1 { text("VELLUM") }
                                p { text("Wireless Archive Integration") }
                                input(type = InputType.text, classes = "pin-input") {
                                    id = "pinInput"
                                    placeholder = "PIN"
                                    maxLength = "4"
                                }
                                label("drop-zone") {
                                    id = "dropZone"
                                    text("Click or Drag Archives Here")
                                    input(type = InputType.file) {
                                        id = "fileInput"
                                        multiple = true
                                    }
                                }
                                div("status") {
                                    id = "status"
                                    text("Enter PIN to integrate...")
                                }
                            }
                            script {
                                unsafe {
                                    raw("""
                                        const dropZone = document.getElementById('dropZone');
                                        const fileInput = document.getElementById('fileInput');
                                        const status = document.getElementById('status');
                                        const pinInput = document.getElementById('pinInput');

                                        dropZone.onclick = () => fileInput.click();

                                        fileInput.onchange = () => {
                                            uploadFiles(fileInput.files);
                                        };

                                        async function uploadFiles(files) {
                                            const pin = pinInput.value;
                                            if (!pin || pin.length !== 4) {
                                                status.innerText = 'Please enter a valid 4-digit PIN.';
                                                return;
                                            }

                                            for (const file of files) {
                                                status.innerText = 'Integrating: ' + file.name;
                                                const formData = new FormData();
                                                formData.append('archive', file);
                                                try {
                                                    const response = await fetch('/upload', {
                                                        method: 'POST',
                                                        headers: { 'X-Vellum-PIN': pin },
                                                        body: formData
                                                    });
                                                    if (response.status === 401) {
                                                        status.innerText = 'Authentication failed. Incorrect PIN.';
                                                        return;
                                                    }
                                                    if (response.ok) {
                                                        status.innerText = 'Successfully integrated: ' + file.name;
                                                    } else {
                                                        status.innerText = 'Integration failed: ' + file.name;
                                                    }
                                                } catch (e) {
                                                    status.innerText = 'Error: ' + e.message;
                                                }
                                            }
                                            status.innerText = 'All content integrated.';
                                        }
                                    """)
                                }
                            }
                        }
                    }
                }

                post("/upload") {
                    val clientPin = call.request.headers["X-Vellum-PIN"]
                    if (clientPin != _serverPin.value) {
                        call.respondText("Unauthorized", status = io.ktor.http.HttpStatusCode.Unauthorized)
                        return@post
                    }

                    val multipart = call.receiveMultipart()
                    multipart.forEachPart { part ->
	                        if (part is PartData.FileItem) {
	                            val fileName = part.originalFileName ?: "unknown"
	                            val file = File(context.filesDir, fileName)
	                            part.provider().copyTo(file.outputStream())

	                            withContext(Dispatchers.IO) {
	                                val document = DocumentFile.fromFile(file)
	                                val book = bookParser.parseDocumentFile(document, "LAN Transfer")
                                if (book != null) {
                                    bookRepository.upsertBook(book)
                                }
                            }
                        }
                        part.dispose()
                    }
                    call.respondText("Integrated")
                }
            }
        }.start(wait = false)
        
        _isServerRunning.value = true
    }

    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
        _isServerRunning.value = false
        _serverAddress.value = null
        _serverPin.value = null
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is InetAddress && address.address.size == 4) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
