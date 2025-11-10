package com.miladb.data.source.connection

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.miladb.data.model.SshConfig
import com.miladb.util.MilaDbError
import com.miladb.util.toMilaDbError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

/**
 * SSH tünellerini yöneten sınıf.
 * JSch kütüphanesi kullanarak güvenli SSH bağlantıları oluşturur.
 * 
 * Özellikler:
 * - Şifre tabanlı kimlik doğrulama
 * - Private key tabanlı kimlik doğrulama
 * - Port forwarding (local port -> remote host:port)
 * - Ana thread dışında çalışır (Dispatchers.IO)
 */
class SshTunnelManager {
    
    private var session: Session? = null
    private var localPort: Int? = null
    private val jsch = JSch()
    
    /**
     * SSH tüneli oluşturur.
     * Başarılı olursa local port numarasını döndürür.
     * 
     * @param config SSH yapılandırma bilgileri
     * @return Result<Int> Başarılı ise local port, hata durumunda MilaDbError
     */
    suspend fun createTunnel(config: SshConfig): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Mevcut tüneli kapat
            closeTunnel()
            
            // Private key varsa ekle
            if (!config.privateKey.isNullOrEmpty()) {
                try {
                    jsch.addIdentity("ssh-key", config.privateKey.toByteArray(), null, null)
                } catch (e: Exception) {
                    return@withContext Result.failure(
                        Exception(MilaDbError.SshError(
                            "Private key yüklenemedi: ${e.message}",
                            e
                        ).toUserMessage())
                    )
                }
            }
            
            // SSH session oluştur
            session = jsch.getSession(config.username, config.host, config.port).apply {
                // Şifre varsa ayarla
                if (!config.password.isNullOrEmpty()) {
                    setPassword(config.password)
                }
                
                // SSH ayarları
                val sshConfig = Properties().apply {
                    // Host key kontrolünü devre dışı bırak (güvenlik riski ama mobil için gerekli)
                    put("StrictHostKeyChecking", "no")
                    // Timeout ayarları
                    put("ConnectTimeout", "10000") // 10 saniye
                }
                setConfig(sshConfig)
                
                // Timeout ayarları
                timeout = 10000 // 10 saniye
            }
            
            // Bağlan
            session?.connect()
            
            if (session?.isConnected != true) {
                return@withContext Result.failure(
                    Exception("SSH bağlantısı kurulamadı")
                )
            }
            
            // Port forwarding ayarla
            // Local port otomatik seçilir (0 = sistem boş port bulur)
            val assignedPort = session?.setPortForwardingL(
                0, // Local port (0 = otomatik)
                config.remoteHost,
                config.remotePort
            ) ?: return@withContext Result.failure(
                Exception("Port forwarding ayarlanamadı")
            )
            
            localPort = assignedPort
            
            Result.success(assignedPort)
            
        } catch (e: Exception) {
            closeTunnel()
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * Aktif tüneli kapatır ve kaynakları temizler.
     */
    fun closeTunnel() {
        try {
            // Port forwarding'i kaldır
            localPort?.let { port ->
                session?.delPortForwardingL(port)
            }
            
            // Session'ı kapat
            session?.disconnect()
        } catch (e: Exception) {
            // Kapatma hatası önemli değil
        } finally {
            session = null
            localPort = null
        }
    }
    
    /**
     * Tünelin aktif olup olmadığını kontrol eder.
     * 
     * @return Boolean Tünel durumu
     */
    fun isTunnelActive(): Boolean {
        return session?.isConnected == true && localPort != null
    }
    
    /**
     * Local port numarasını döndürür.
     * 
     * @return Int? Local port veya null
     */
    fun getLocalPort(): Int? {
        return if (isTunnelActive()) localPort else null
    }
    
    /**
     * SSH session bilgilerini döndürür (debug için).
     * 
     * @return String Session bilgileri
     */
    fun getSessionInfo(): String {
        return if (session?.isConnected == true) {
            "SSH Bağlantısı Aktif\n" +
            "Host: ${session?.host}\n" +
            "Port: ${session?.port}\n" +
            "User: ${session?.userName}\n" +
            "Local Port: $localPort"
        } else {
            "SSH Bağlantısı Yok"
        }
    }
}
