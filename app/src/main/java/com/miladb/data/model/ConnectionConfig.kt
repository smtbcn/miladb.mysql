package com.miladb.data.model

/**
 * Veritabanı bağlantı yapılandırması.
 * 
 * @property host Veritabanı sunucu adresi
 * @property port Veritabanı port numarası
 * @property username Kullanıcı adı
 * @property password Şifre
 * @property database Bağlanılacak veritabanı (opsiyonel)
 * @property useSsl SSL bağlantısı kullanılsın mı
 * @property sshConfig SSH tünel yapılandırması (opsiyonel)
 */
data class ConnectionConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val database: String? = null,
    val useSsl: Boolean = false,
    val sshConfig: SshConfig? = null
)
