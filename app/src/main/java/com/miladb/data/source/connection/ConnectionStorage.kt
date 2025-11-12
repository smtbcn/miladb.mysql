package com.miladb.data.source.connection

import android.content.Context
import android.content.SharedPreferences
import com.miladb.data.model.SavedConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Bağlantı bilgilerini SharedPreferences'ta saklayan sınıf.
 * 
 * NOT: Şifreler GÜVENLİK nedeniyle saklanmaz!
 */
class ConnectionStorage(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "miladb_connections",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_CONNECTIONS = "saved_connections"
    }
    
    /**
     * Tüm kaydedilmiş bağlantıları getirir.
     * İlk kullanımda demo bağlantısını otomatik olarak ekler.
     */
    suspend fun getSavedConnections(): List<SavedConnection> = withContext(Dispatchers.IO) {
        try {
            val json = prefs.getString(KEY_CONNECTIONS, "[]") ?: "[]"
            val jsonArray = JSONArray(json)
            
            val connections = (0 until jsonArray.length()).map { index ->
                val obj = jsonArray.getJSONObject(index)
                SavedConnection(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    host = obj.getString("host"),
                    port = obj.getInt("port"),
                    username = obj.getString("username"),
                    password = obj.optString("password").ifEmpty { null },
                    database = obj.optString("database").ifEmpty { null },
                    useSsl = obj.optBoolean("useSsl", false),
                    useSsh = obj.optBoolean("useSsh", false),
                    sshHost = obj.optString("sshHost").ifEmpty { null },
                    sshPort = if (obj.has("sshPort")) obj.getInt("sshPort") else null,
                    sshUsername = obj.optString("sshUsername").ifEmpty { null },
                    sshPassword = obj.optString("sshPassword").ifEmpty { null }
                )
            }.toMutableList()
            
            // İlk kullanımda demo bağlantısını ekle
            if (connections.isEmpty()) {
                val demoConnection = SavedConnection(
                    id = "demo_connection_001",
                    name = "Demo Sunucu (sql7.freesqldatabase.com)",
                    host = "sql7.freesqldatabase.com",
                    port = 3306,
                    username = "sql7807105",
                    password = "hnKJ5TrMwM",
                    database = "sql7807105",
                    useSsl = false,
                    useSsh = false
                )
                connections.add(demoConnection)
                saveConnectionsList(connections)
            }
            
            connections
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Yeni bağlantı kaydeder.
     */
    suspend fun saveConnection(connection: SavedConnection): Boolean = withContext(Dispatchers.IO) {
        try {
            val connections = getSavedConnections().toMutableList()
            
            // Aynı ID varsa güncelle, yoksa ekle
            val existingIndex = connections.indexOfFirst { it.id == connection.id }
            if (existingIndex >= 0) {
                connections[existingIndex] = connection
            } else {
                connections.add(connection)
            }
            
            saveConnectionsList(connections)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Bağlantıyı siler.
     */
    suspend fun deleteConnection(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val connections = getSavedConnections().toMutableList()
            connections.removeAll { it.id == id }
            saveConnectionsList(connections)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Kayıtlı tüm bağlantıları ham JSON metni olarak dışa aktarır.
     * Şifre de dahil mevcut veriler olduğu gibi döndürülür.
     */
    fun exportConnectionsJson(): String {
        return prefs.getString(KEY_CONNECTIONS, "[]") ?: "[]"
    }
    
    /**
     * Bağlantıları JSON dizisi formatında içe aktarır. Var olan listeyi verilen
     * içerikle değiştirir. Eksik alanlar için varsayılanlar uygulanır.
     *
     * Beklenen format: [
     *   {"id":"...","name":"...","host":"...","port":3306,
     *    "username":"...","password":"...","database":"...",
     *    "useSsl":false,"useSsh":false,
     *    "sshHost":"...","sshPort":22,"sshUsername":"...","sshPassword":"..."}
     * ]
     */
    suspend fun importConnectionsJson(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // JSON'u temizle ve olası BOM/zero-width karakterleri çıkar
            val sanitized = json.trim()
                .replace("\uFEFF", "") // BOM
                .replace("\u200B", "") // zero-width space
                .replace("\u2060", "") // word joiner

            // Dizi veya { connections: [] } formatını destekle
            val jsonArray = if (sanitized.startsWith("{")) {
                val obj = JSONObject(sanitized)
                if (obj.has("connections")) obj.getJSONArray("connections") else JSONArray()
            } else {
                JSONArray(sanitized)
            }
            val imported = mutableListOf<SavedConnection>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id").ifBlank { generateId() }
                val name = obj.getString("name")
                val host = obj.getString("host")
                val port = when {
                    obj.has("port") && obj.opt("port") is Number -> obj.getInt("port")
                    else -> obj.optString("port").toIntOrNull() ?: 3306
                }
                val username = obj.getString("username")
                val password = obj.optString("password").ifEmpty { null }
                val database = obj.optString("database").ifEmpty { null }
                val useSsl = obj.optBoolean("useSsl", false)
                val useSsh = obj.optBoolean("useSsh", false)
                val sshHost = obj.optString("sshHost").ifEmpty { null }
                val sshPort = when {
                    obj.has("sshPort") && obj.opt("sshPort") is Number -> obj.getInt("sshPort")
                    else -> obj.optString("sshPort").toIntOrNull()
                }
                val sshUsername = obj.optString("sshUsername").ifEmpty { null }
                val sshPassword = obj.optString("sshPassword").ifEmpty { null }

                imported.add(
                    SavedConnection(
                        id = id,
                        name = name,
                        host = host,
                        port = port,
                        username = username,
                        password = password,
                        database = database,
                        useSsl = useSsl,
                        useSsh = useSsh,
                        sshHost = sshHost,
                        sshPort = sshPort,
                        sshUsername = sshUsername,
                        sshPassword = sshPassword
                    )
                )
            }
            // Mevcut listeyi tamamen değiştir
            saveConnectionsList(imported)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Bağlantı listesini kaydeder.
     */
    private fun saveConnectionsList(connections: List<SavedConnection>) {
        val jsonArray = JSONArray()
        connections.forEach { conn ->
            val obj = JSONObject().apply {
                put("id", conn.id)
                put("name", conn.name)
                put("host", conn.host)
                put("port", conn.port)
                put("username", conn.username)
                conn.password?.let { put("password", it) }
                conn.database?.let { put("database", it) }
                put("useSsl", conn.useSsl)
                put("useSsh", conn.useSsh)
                conn.sshHost?.let { put("sshHost", it) }
                conn.sshPort?.let { put("sshPort", it) }
                conn.sshUsername?.let { put("sshUsername", it) }
                conn.sshPassword?.let { put("sshPassword", it) }
            }
            jsonArray.put(obj)
        }
        
        prefs.edit().putString(KEY_CONNECTIONS, jsonArray.toString()).apply()
    }
    
    /**
     * Yeni benzersiz ID oluşturur.
     */
    fun generateId(): String = UUID.randomUUID().toString()
}
