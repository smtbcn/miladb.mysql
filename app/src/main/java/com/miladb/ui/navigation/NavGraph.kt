package com.miladb.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.miladb.data.repository.DatabaseRepository
import com.miladb.data.repository.ExportRepository
import com.miladb.data.source.connection.JdbcConnectionManager
import com.miladb.data.source.connection.SshTunnelManager
import com.miladb.data.source.export.CsvExporter
import com.miladb.data.source.export.ExcelExporter
import com.miladb.ui.screen.connection.ConnectionScreen
import com.miladb.ui.screen.connection.ConnectionViewModel
import com.miladb.ui.screen.database.DatabaseListScreen
import com.miladb.ui.screen.database.DatabaseViewModel
import com.miladb.ui.screen.export.ExportViewModel
import com.miladb.ui.screen.query.QueryEditorScreen
import com.miladb.ui.screen.query.QueryViewModel
import com.miladb.ui.screen.table.*

/**
 * Uygulama navigasyon rotaları.
 */
sealed class Screen(val route: String) {
    object Connection : Screen("connection")
    object DatabaseList : Screen("database_list")
    object TableList : Screen("table_list/{database}") {
        fun createRoute(database: String) = "table_list/$database"
    }
    object TableData : Screen("table_data/{database}/{table}") {
        fun createRoute(database: String, table: String) = "table_data/$database/$table"
    }
    object RowEditor : Screen("row_editor/{database}/{table}/{isNew}") {
        fun createRoute(database: String, table: String, isNew: Boolean) = 
            "row_editor/$database/$table/$isNew"
    }
    object CreateTable : Screen("create_table/{database}") {
        fun createRoute(database: String) = "create_table/$database"
    }
    object QueryEditor : Screen("query_editor/{database}") {
        fun createRoute(database: String) = "query_editor/$database"
    }
    object EditTable : Screen("edit_table/{database}/{table}") {
        fun createRoute(database: String, table: String) = "edit_table/$database/$table"
    }
}

/**
 * Navigasyon grafiği.
 * Tüm ekranları ve aralarındaki geçişleri tanımlar.
 */
@Composable
fun MilaDbNavGraph(
    navController: NavHostController,
    context: android.content.Context,
    startDestination: String = Screen.Connection.route
) {
    // ViewModels - remember ile oluştur (recomposition'da korunur)
    val connectionStorage = remember { com.miladb.data.source.connection.ConnectionStorage(context) }
    val connectionViewModel = remember {
        ConnectionViewModel(
            JdbcConnectionManager,
            SshTunnelManager(),
            connectionStorage
        )
    }
    
    val databaseRepository = remember { DatabaseRepository(JdbcConnectionManager) }
    val databaseViewModel = remember { DatabaseViewModel(databaseRepository) }
    val tableViewModel = remember { TableViewModel(databaseRepository) }
    val queryViewModel = remember { QueryViewModel(databaseRepository) }
    
    val exportRepository = remember {
        ExportRepository(
            ExcelExporter(context),
            CsvExporter(context),
            com.miladb.data.source.export.SqlExporter(context),
            databaseRepository
        )
    }
    val exportViewModel = remember { ExportViewModel(exportRepository) }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Connection Screen
        composable(Screen.Connection.route) {
            ConnectionScreen(
                viewModel = connectionViewModel,
                onConnectionSuccess = { database ->
                    if (database != null) {
                        // Veritabanı adı ile bağlanıldı, direkt tablo listesine git
                        navController.navigate(Screen.TableList.createRoute(database)) {
                            popUpTo(Screen.Connection.route) { inclusive = true }
                        }
                    } else {
                        // Veritabanı adı olmadan bağlanıldı, veritabanı listesine git
                        navController.navigate(Screen.DatabaseList.route) {
                            popUpTo(Screen.Connection.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        // Database List Screen
        composable(Screen.DatabaseList.route) {
            DatabaseListScreen(
                viewModel = databaseViewModel,
                onDatabaseSelected = { database ->
                    navController.navigate(Screen.TableList.createRoute(database))
                },
                onBackPressed = {
                    // Connection ekranına dön
                    navController.navigate(Screen.Connection.route) {
                        popUpTo(Screen.Connection.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Table List Screen
        composable(
            route = Screen.TableList.route,
            arguments = listOf(
                navArgument("database") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val database = backStackEntry.arguments?.getString("database") ?: return@composable
            
            TableListScreen(
                viewModel = databaseViewModel,
                tableViewModel = tableViewModel,
                database = database,
                onTableSelected = { table ->
                    navController.navigate(Screen.TableData.createRoute(database, table))
                },
                onCreateTable = {
                    navController.navigate(Screen.CreateTable.createRoute(database))
                },
                onSqlEditor = {
                    navController.navigate(Screen.QueryEditor.createRoute(database))
                },
                onEditTable = { table ->
                    navController.navigate(Screen.EditTable.createRoute(database, table))
                },
                onBackPressed = {
                    // Eğer belirli bir veritabanı ile bağlanıldıysa, direkt bağlantı ekranına dön
                    if (JdbcConnectionManager.isConnectedWithDatabase()) {
                        navController.navigate(Screen.Connection.route) {
                            popUpTo(Screen.Connection.route) { inclusive = true }
                        }
                    } else {
                        // Veritabanı listesine dön
                        navController.popBackStack()
                    }
                }
            )
        }
        
        // Table Data Screen
        composable(
            route = Screen.TableData.route,
            arguments = listOf(
                navArgument("database") { type = NavType.StringType },
                navArgument("table") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val database = backStackEntry.arguments?.getString("database") ?: return@composable
            val table = backStackEntry.arguments?.getString("table") ?: return@composable
            
            TableDataScreen(
                tableViewModel = tableViewModel,
                exportViewModel = exportViewModel,
                database = database,
                table = table,
                onEditRow = { columns, rowData ->
                    tableViewModel.setSelectedRowData(columns, rowData)
                    navController.navigate(Screen.RowEditor.createRoute(database, table, false))
                },
                onAddRow = {
                    tableViewModel.clearSelectedRowData()
                    navController.navigate(Screen.RowEditor.createRoute(database, table, true))
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        // Row Editor Screen
        composable(
            route = Screen.RowEditor.route,
            arguments = listOf(
                navArgument("database") { type = NavType.StringType },
                navArgument("table") { type = NavType.StringType },
                navArgument("isNew") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val database = backStackEntry.arguments?.getString("database") ?: return@composable
            val table = backStackEntry.arguments?.getString("table") ?: return@composable
            val isNew = backStackEntry.arguments?.getBoolean("isNew") ?: true
            
            RowEditorScreen(
                tableViewModel = tableViewModel,
                database = database,
                table = table,
                rowData = null, // selectedRowData ViewModel'den alınıyor
                isNewRow = isNew,
                onSaved = {
                    navController.popBackStack()
                },
                onCancelled = {
                    navController.popBackStack()
                }
            )
        }
        
        // Create Table Screen
        composable(
            route = Screen.CreateTable.route,
            arguments = listOf(
                navArgument("database") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val database = backStackEntry.arguments?.getString("database") ?: return@composable
            
            CreateTableScreen(
                tableViewModel = tableViewModel,
                database = database,
                onCreated = {
                    navController.popBackStack()
                },
                onCancelled = {
                    navController.popBackStack()
                }
            )
        }

        // Edit Table Screen
        composable(
            route = Screen.EditTable.route,
            arguments = listOf(
                navArgument("database") { type = NavType.StringType },
                navArgument("table") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val database = backStackEntry.arguments?.getString("database") ?: return@composable
            val table = backStackEntry.arguments?.getString("table") ?: return@composable

            EditTableScreen(
                tableViewModel = tableViewModel,
                database = database,
                table = table,
                onSaved = {
                    navController.popBackStack()
                },
                onCancelled = {
                    navController.popBackStack()
                }
            )
        }
        
        // Query Editor Screen
        composable(
            route = Screen.QueryEditor.route,
            arguments = listOf(
                navArgument("database") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val database = backStackEntry.arguments?.getString("database") ?: return@composable
            
            QueryEditorScreen(
                queryViewModel = queryViewModel,
                database = database,
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
    }
}
