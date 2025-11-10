package com.miladb.ui.screen.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miladb.data.model.ConnectionConfig
import com.miladb.data.model.ConnectionUiState
import com.miladb.data.model.SshConfig
import com.miladb.data.source.connection.JdbcConnectionManager
import com.miladb.data.source.connection.SshTunnelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Bağlantı ekranının state ve iş mantığını yöneten ViewModel.
 * 
 * Özellikler:
 * - JDBC bağlantı yönetimi
 * - SSH tünel yönetimi
 * - SSL bağlantı desteği
 * - Asenkron işlemler (Coroutines)
 * - State management (StateFlow)
 */
class ConnectionViewModel(
    private val connectionManager: JdbcConnectionManager,
    private val sshTunnelManager: SshTunnelManager,
    val connectionStorage: com.miladb.data.source.connection.ConnectionStorage
) : ViewModel() {
    
    // UI State
    private val _connectionState = MutableStateFlow<ConnectionUiState>(ConnectionUiState.Idle)
    val connectionState: StateFlow<ConnectionUiState> = _connectionState.asStateFlow()
    
    // Saved Connections
    private val _savedConnections = MutableStateFlow<List<com.miladb.data.model.SavedConnection>>(emptyList())
    val savedConnections: StateFlow<List<com.miladb.data.model.SavedConnection>> = _savedConnections.asStateFlow()
    
    // Form state
    private val _sshEnabled = MutableStateFlow(false)
    val sshEnabled: StateFlow<Boolean> = _sshEnabled.asStateFlow()
    
    private val _sslEnabled = MutableStateFlow(false)
    val sslEnabled: StateFlow<Boolean> = _sslEnabled.asStateFlow()
    
    init {
        loadSavedConnections()
    }
    
    /**
     * Veritabanına bağlantı kurar.
     * SSH aktifse önce tünel oluşturur, sonra JDBC bağlantısı kurar.
     * 
     * @param config Bağlantı yapılandırması
     */
    fun connect(config: ConnectionConfig) {
        viewModelScope.launch {
            _connectionState.value = ConnectionUiState.Loading
            
            try {
                // SSH tüneli varsa oluştur
                val finalConfig = if (config.sshConfig != null) {
                    val tunnelResult = sshTunnelManager.createTunnel(config.sshConfig)
                    
                    if (tunnelResult.isFailure) {
                        _connectionState.value = ConnectionUiState.Error(
                            tunnelResult.exceptionOrNull()?.message ?: "SSH tüneli oluşturulamadı"
                        )
                        return@launch
                    }
                    
                    // SSH tüneli başarılı, local port'u kullan
                    val localPort = tunnelResult.getOrNull() ?: run {
                        _connectionState.value = ConnectionUiState.Error("SSH local port alınamadı")
                        return@launch
                    }
                    
                    // Bağlantı config'ini SSH tüneli için güncelle
                    config.copy(
                        host = "127.0.0.1",
                        port = localPort
                    )
                } else {
                    config
                }
                
                // JDBC bağlantısı kur
                val connectionResult = connectionManager.connect(finalConfig)
                
                if (connectionResult.isSuccess) {
                    // Eğer veritabanı adı verilmişse, onu state'e ekle
                    _connectionState.value = if (!finalConfig.database.isNullOrBlank()) {
                        ConnectionUiState.SuccessWithDatabase(finalConfig.database)
                    } else {
                        ConnectionUiState.Success
                    }
                } else {
                    // Bağlantı başarısız, SSH tünelini kapat
                    if (config.sshConfig != null) {
                        sshTunnelManager.closeTunnel()
                    }
                    
                    _connectionState.value = ConnectionUiState.Error(
                        connectionResult.exceptionOrNull()?.message ?: "Bağlantı başarısız"
                    )
                }
                
            } catch (e: Exception) {
                // Hata durumunda SSH tünelini kapat
                if (config.sshConfig != null) {
                    sshTunnelManager.closeTunnel()
                }
                
                // Detaylı hata mesajı
                val errorMessage = buildString {
                    append("Bağlantı hatası: ")
                    append(e.message ?: "Bilinmeyen hata")
                    append("\nTip: ${e.javaClass.simpleName}")
                    e.cause?.let { cause ->
                        append("\nNeden: ${cause.message}")
                    }
                }
                
                android.util.Log.e("ConnectionViewModel", "Connection error", e)
                
                _connectionState.value = ConnectionUiState.Error(errorMessage)
            }
        }
    }
    
    /**
     * SSH toggle durumunu günceller.
     * 
     * @param enabled SSH aktif mi
     */
    fun setSshEnabled(enabled: Boolean) {
        _sshEnabled.value = enabled
    }
    
    /**
     * SSL toggle durumunu günceller.
     * 
     * @param enabled SSL aktif mi
     */
    fun setSslEnabled(enabled: Boolean) {
        _sslEnabled.value = enabled
    }
    
    /**
     * Bağlantı durumunu sıfırlar.
     */
    fun resetConnectionState() {
        _connectionState.value = ConnectionUiState.Idle
    }
    
    /**
     * Kaydedilmiş bağlantıları yükler.
     */
    fun loadSavedConnections() {
        viewModelScope.launch {
            val connections = connectionStorage.getSavedConnections()
            _savedConnections.value = connections
        }
    }
    
    /**
     * Bağlantıyı kaydeder.
     */
    fun saveConnection(connection: com.miladb.data.model.SavedConnection) {
        viewModelScope.launch {
            connectionStorage.saveConnection(connection)
            loadSavedConnections()
        }
    }
    
    /**
     * Bağlantıyı siler.
     */
    fun deleteConnection(id: String) {
        viewModelScope.launch {
            connectionStorage.deleteConnection(id)
            loadSavedConnections()
        }
    }
    
    /**
     * ViewModel temizlendiğinde bağlantıları kapat.
     */
    override fun onCleared() {
        super.onCleared()
        sshTunnelManager.closeTunnel()
        connectionManager.disconnect()
    }
}
