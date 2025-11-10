package com.miladb.util

/**
 * Tüm hataları merkezi olarak yöneten sealed class.
 * Her hata tipi için Türkçe kullanıcı dostu mesaj sağlar.
 */
sealed class MilaDbError {
    /**
     * Veritabanı bağlantı hatası.
     * 
     * @property message Hata mesajı
     * @property cause Hata nedeni (opsiyonel)
     */
    data class ConnectionError(
        val message: String,
        val cause: Throwable? = null
    ) : MilaDbError()
    
    /**
     * SSH tünel hatası.
     * 
     * @property message Hata mesajı
     * @property cause Hata nedeni (opsiyonel)
     */
    data class SshError(
        val message: String,
        val cause: Throwable? = null
    ) : MilaDbError()
    
    /**
     * Veritabanı işlem hatası (SQL hataları).
     * 
     * @property message Hata mesajı
     * @property cause Hata nedeni (opsiyonel)
     */
    data class DatabaseError(
        val message: String,
        val cause: Throwable? = null
    ) : MilaDbError()
    
    /**
     * Dışa aktarma hatası.
     * 
     * @property message Hata mesajı
     * @property cause Hata nedeni (opsiyonel)
     */
    data class ExportError(
        val message: String,
        val cause: Throwable? = null
    ) : MilaDbError()
    
    /**
     * Bilinmeyen hata.
     * 
     * @property message Hata mesajı
     * @property cause Hata nedeni (opsiyonel)
     */
    data class UnknownError(
        val message: String,
        val cause: Throwable? = null
    ) : MilaDbError()
    
    /**
     * Kullanıcı dostu Türkçe hata mesajı döndürür.
     * 
     * @return Türkçe hata mesajı
     */
    fun toUserMessage(): String {
        return when (this) {
            is ConnectionError -> {
                when {
                    message.contains("timeout", ignoreCase = true) -> 
                        "Bağlantı zaman aşımına uğradı. Sunucu adresini ve ağ bağlantınızı kontrol edin."
                    message.contains("refused", ignoreCase = true) -> 
                        "Bağlantı reddedildi. Sunucu çalışıyor mu ve port numarası doğru mu kontrol edin."
                    message.contains("unknown host", ignoreCase = true) -> 
                        "Sunucu bulunamadı. Sunucu adresini kontrol edin."
                    message.contains("access denied", ignoreCase = true) -> 
                        "Erişim reddedildi. Kullanıcı adı ve şifrenizi kontrol edin."
                    message.contains("authentication", ignoreCase = true) -> 
                        "Kimlik doğrulama başarısız. Kullanıcı adı ve şifrenizi kontrol edin."
                    else -> "Bağlantı hatası: $message"
                }
            }
            is SshError -> {
                when {
                    message.contains("auth", ignoreCase = true) -> 
                        "SSH kimlik doğrulama başarısız. SSH kullanıcı adı ve şifrenizi kontrol edin."
                    message.contains("timeout", ignoreCase = true) -> 
                        "SSH bağlantısı zaman aşımına uğradı. SSH sunucu adresini kontrol edin."
                    message.contains("refused", ignoreCase = true) -> 
                        "SSH bağlantısı reddedildi. SSH sunucusu çalışıyor mu kontrol edin."
                    message.contains("key", ignoreCase = true) -> 
                        "SSH private key hatası. Key dosyasını kontrol edin."
                    else -> "SSH tünel hatası: $message"
                }
            }
            is DatabaseError -> {
                when {
                    message.contains("syntax", ignoreCase = true) -> 
                        "SQL sözdizimi hatası. Sorgunuzu kontrol edin."
                    message.contains("doesn't exist", ignoreCase = true) -> 
                        "Veritabanı veya tablo bulunamadı."
                    message.contains("duplicate", ignoreCase = true) -> 
                        "Bu kayıt zaten mevcut. Primary key veya unique constraint ihlali."
                    message.contains("foreign key", ignoreCase = true) -> 
                        "Foreign key kısıtlaması ihlali. İlişkili kayıtları kontrol edin."
                    message.contains("permission", ignoreCase = true) || 
                    message.contains("denied", ignoreCase = true) -> 
                        "Yetki hatası. Bu işlem için yeterli izniniz yok."
                    message.contains("lock", ignoreCase = true) -> 
                        "Tablo kilitli. Lütfen daha sonra tekrar deneyin."
                    else -> "Veritabanı hatası: $message"
                }
            }
            is ExportError -> {
                when {
                    message.contains("permission", ignoreCase = true) -> 
                        "Dosya yazma izni yok. Uygulama izinlerini kontrol edin."
                    message.contains("space", ignoreCase = true) -> 
                        "Yetersiz depolama alanı. Cihazınızda yer açın."
                    message.contains("not found", ignoreCase = true) -> 
                        "Dosya yolu bulunamadı."
                    else -> "Dışa aktarma hatası: $message"
                }
            }
            is UnknownError -> {
                "Beklenmeyen bir hata oluştu: $message"
            }
        }
    }
    
    /**
     * Hata logunu döndürür (debug için).
     * 
     * @return Detaylı hata logu
     */
    fun toLogMessage(): String {
        return when (this) {
            is ConnectionError -> {
                val causeMessage = cause?.let { "\nNeden: ${it.message}\n${it.stackTraceToString()}" } ?: ""
                "ConnectionError: $message$causeMessage"
            }
            is SshError -> {
                val causeMessage = cause?.let { "\nNeden: ${it.message}\n${it.stackTraceToString()}" } ?: ""
                "SshError: $message$causeMessage"
            }
            is DatabaseError -> {
                val causeMessage = cause?.let { "\nNeden: ${it.message}\n${it.stackTraceToString()}" } ?: ""
                "DatabaseError: $message$causeMessage"
            }
            is ExportError -> {
                val causeMessage = cause?.let { "\nNeden: ${it.message}\n${it.stackTraceToString()}" } ?: ""
                "ExportError: $message$causeMessage"
            }
            is UnknownError -> {
                val causeMessage = cause?.let { "\nNeden: ${it.message}\n${it.stackTraceToString()}" } ?: ""
                "UnknownError: $message$causeMessage"
            }
        }
    }
}

/**
 * Exception'ı MilaDbError'a dönüştürür.
 * 
 * @return MilaDbError instance
 */
fun Throwable.toMilaDbError(): MilaDbError {
    val message = this.message ?: "Bilinmeyen hata"
    
    return when {
        // SSH hataları
        this::class.java.name.contains("jsch", ignoreCase = true) -> 
            MilaDbError.SshError(message, this)
        
        // JDBC/SQL hataları
        this::class.java.name.contains("sql", ignoreCase = true) -> 
            MilaDbError.DatabaseError(message, this)
        
        // Bağlantı hataları
        this is java.net.ConnectException ||
        this is java.net.SocketTimeoutException ||
        this is java.net.UnknownHostException -> 
            MilaDbError.ConnectionError(message, this)
        
        // Dosya/IO hataları
        this is java.io.IOException -> 
            MilaDbError.ExportError(message, this)
        
        // Diğer hatalar
        else -> MilaDbError.UnknownError(message, this)
    }
}
