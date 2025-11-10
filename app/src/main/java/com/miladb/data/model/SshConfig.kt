package com.miladb.data.model

/**
 * SSH tünel yapılandırması.
 * 
 * @property host SSH sunucu adresi
 * @property port SSH port numarası
 * @property username SSH kullanıcı adı
 * @property password SSH şifresi (opsiyonel)
 * @property privateKey SSH private key (opsiyonel)
 * @property remoteHost Uzak sunucu adresi (veritabanı sunucusu)
 * @property remotePort Uzak sunucu port numarası
 */
data class SshConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String? = null,
    val privateKey: String? = null,
    val remoteHost: String,
    val remotePort: Int
)
