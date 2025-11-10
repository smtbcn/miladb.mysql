package com.miladb.data.source.connection

import com.miladb.data.model.ConnectionConfig
import com.miladb.util.MilaDbError
import com.miladb.util.toMilaDbError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

/**
 * JDBC bağlantılarını yöneten singleton sınıf.
 * MariaDB JDBC driver kullanarak MySQL/MariaDB sunucularına bağlantı sağlar.
 * 
 * Özellikler:
 * - Singleton pattern ile tek instance
 * - SSL bağlantı desteği
 * - Bağlantı havuzu yok (mobil uygulama için tek bağlantı yeterli)
 * - Ana thread dışında çalışır (Dispatchers.IO)
 */
object JdbcConnectionManager {
    
    private var connection: Connection? = null
    private var currentConfig: ConnectionConfig? = null
    
    init {
        // MySQL JDBC driver'ı yükle (5.1.x için eski driver class)
        try {
            Class.forName("com.mysql.jdbc.Driver")
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("MySQL JDBC driver bulunamadı", e)
        }
    }
    
    /**
     * Veritabanı bağlantısı oluşturur.
     * Mevcut bağlantı varsa önce kapatır.
     * 
     * @param config Bağlantı yapılandırma bilgileri
     * @return Result<Connection> Başarılı ise Connection, hata durumunda MilaDbError
     */
    suspend fun connect(config: ConnectionConfig): Result<Connection> = withContext(Dispatchers.IO) {
        try {
            // İnternet bağlantısını test et
            try {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress(config.host, config.port), 5000)
                socket.close()
                android.util.Log.d("JdbcConnectionManager", "Socket bağlantısı başarılı: ${config.host}:${config.port}")
            } catch (e: Exception) {
                android.util.Log.e("JdbcConnectionManager", "Socket bağlantısı başarısız: ${config.host}")
                return@withContext Result.failure(
                    Exception("Sunucuya bağlanılamıyor!\n\n" +
                            "Olası nedenler:\n" +
                            "• MySQL sunucusu uzaktan erişime kapalı\n" +
                            "• Firewall/güvenlik duvarı engelliyor\n" +
                            "• Sunucu adresi veya port yanlış\n\n" +
                            "Çözüm: Hosting panelinizden (cPanel) 'Remote MySQL' ayarlarını kontrol edin ve cihazınızın IP adresini ekleyin.", e)
                )
            }
            
            // Mevcut bağlantıyı kapat
            disconnect()
            
            // Bağlantı URL'i oluştur
            val jdbcUrl = buildJdbcUrl(config)
            
            // Bağlantı özellikleri
            val properties = Properties().apply {
                setProperty("user", config.username)
                setProperty("password", config.password)
                
                // SSL ayarları
                if (config.useSsl) {
                    setProperty("useSSL", "true")
                    setProperty("requireSSL", "true")
                    setProperty("verifyServerCertificate", "true")
                } else {
                    setProperty("useSSL", "false")
                }
                
                // Timeout ayarları
                setProperty("connectTimeout", "10000") // 10 saniye
                setProperty("socketTimeout", "30000")  // 30 saniye
                
                // Karakter seti
                setProperty("characterEncoding", "UTF-8")
                setProperty("useUnicode", "true")
            }
            
            // Bağlantı kur
            connection = DriverManager.getConnection(jdbcUrl, properties)
            currentConfig = config
            
            // Eğer veritabanı adı verilmişse, USE komutu ile seç
            if (!config.database.isNullOrBlank()) {
                try {
                    connection?.createStatement()?.use { statement ->
                        statement.execute("USE `${config.database}`")
                    }
                    android.util.Log.d("JdbcConnectionManager", "Veritabanı seçildi: ${config.database}")
                } catch (e: Exception) {
                    // Veritabanı seçilemedi ama bağlantı başarılı
                    android.util.Log.w("JdbcConnectionManager", "Veritabanı seçilemedi: ${e.message}")
                    // Hata vermeden devam et, kullanıcı veritabanlarını görebilir
                }
            }
            
            Result.success(connection!!)
            
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * JDBC URL'i oluşturur.
     * 
     * @param config Bağlantı yapılandırması
     * @return JDBC URL string
     */
    private fun buildJdbcUrl(config: ConnectionConfig): String {
        val host = config.host
        val port = config.port
        
        // Veritabanı adını URL'de belirtme, sonra USE komutu ile seçeceğiz
        return "jdbc:mysql://$host:$port/"
    }
    
    /**
     * Aktif bağlantıyı döndürür.
     * 
     * @return Connection? Aktif bağlantı veya null
     */
    fun getConnection(): Connection? {
        return if (connection?.isClosed == false) {
            connection
        } else {
            null
        }
    }
    
    /**
     * Bağlantıyı kapatır ve kaynakları temizler.
     * Güvenlik için bağlantı bilgilerini de temizler.
     */
    fun disconnect() {
        try {
            connection?.close()
        } catch (e: Exception) {
            // Bağlantı kapatma hatası önemli değil
        } finally {
            connection = null
            currentConfig = null
        }
    }
    
    /**
     * Bağlantının aktif olup olmadığını kontrol eder.
     * 
     * @return Boolean Bağlantı durumu
     */
    fun isConnected(): Boolean {
        return try {
            connection?.isClosed == false && connection?.isValid(5) == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Mevcut bağlantı yapılandırmasını döndürür.
     * 
     * @return ConnectionConfig? Mevcut yapılandırma veya null
     */
    fun getCurrentConfig(): ConnectionConfig? {
        return currentConfig
    }
    
    /**
     * Bağlantı belirli bir veritabanı ile mi yapıldı kontrol eder.
     * 
     * @return Boolean Veritabanı adı ile bağlanıldıysa true
     */
    fun isConnectedWithDatabase(): Boolean {
        return !currentConfig?.database.isNullOrBlank()
    }
    
    /**
     * Bağlanılan veritabanı adını döndürür.
     * 
     * @return String? Veritabanı adı veya null
     */
    fun getConnectedDatabase(): String? {
        return currentConfig?.database
    }
    
    /**
     * Bağlantıyı test eder.
     * 
     * @return Result<Boolean> Başarılı ise true, hata durumunda MilaDbError
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection() ?: return@withContext Result.failure(
                Exception("Bağlantı yok")
            )
            
            // Basit bir sorgu ile test et
            conn.createStatement().use { statement ->
                statement.executeQuery("SELECT 1").use { resultSet ->
                    Result.success(resultSet.next())
                }
            }
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
}
