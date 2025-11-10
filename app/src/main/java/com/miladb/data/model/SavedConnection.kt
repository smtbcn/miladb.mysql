package com.miladb.data.model

/**
 * Kaydedilmiş bağlantı bilgileri.
 * 
 * @property id Benzersiz ID
 * @property name Bağlantı adı (kullanıcı tarafından verilen)
 * @property host Sunucu adresi
 * @property port Port numarası
 * @property username Kullanıcı adı
 * @property password Şifre (opsiyonel)
 * @property database Varsayılan veritabanı (opsiyonel)
 * @property useSsl SSL kullanılsın mı
 * @property useSsh SSH kullanılsın mı
 * @property sshHost SSH sunucu adresi (opsiyonel)
 * @property sshPort SSH port numarası (opsiyonel)
 * @property sshUsername SSH kullanıcı adı (opsiyonel)
 * @property sshPassword SSH şifresi (opsiyonel)
 */
data class SavedConnection(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String? = null,
    val database: String? = null,
    val useSsl: Boolean = false,
    val useSsh: Boolean = false,
    val sshHost: String? = null,
    val sshPort: Int? = null,
    val sshUsername: String? = null,
    val sshPassword: String? = null
)
