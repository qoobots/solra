package com.solra.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/// Tab 导航框架（发现/创作/虚拟人/我的）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolraNavHost() {
    val navController = rememberNavController()
    val tabs = listOf(
        TabItem("discover", "发现", Icons.Filled.Search),
        TabItem("create", "创作", Icons.Filled.Add),
        TabItem("avatar", "虚拟人", Icons.Filled.Person),
        TabItem("profile", "我的", Icons.Filled.Settings),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "discover",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("discover") { SpaceFeedScreen(navController) }
            composable("create") { CreateSpaceScreen() }
            composable("avatar") { AvatarScreen() }
            composable("profile") { ProfileScreen() }
        }
    }
}

// MARK: - 空间发现页

@Composable
fun SpaceFeedScreen(navController: androidx.navigation.NavController) {
    var isLoading by remember { mutableStateOf(true) }
    val spaces = remember {
        listOf(
            SpaceItem("1", "赛博茶馆", "聊天交友的虚拟茶室", listOf("社交", "聊天"), 42),
            SpaceItem("2", "星空画廊", "AI艺术展览空间", listOf("艺术", "AI"), 18),
            SpaceItem("3", "代码峡谷", "程序员聚集地", listOf("技术", "学习"), 67),
            SpaceItem("4", "音乐森林", "虚拟演唱会现场", listOf("音乐", "演出"), 103),
            SpaceItem("5", "禅意庭院", "冥想放松空间", listOf("冥想", "自然"), 9),
            SpaceItem("6", "赛博竞技场", "对抗竞技空间", listOf("游戏", "竞技"), 55),
        )
    }

    LaunchedEffect(Unit) {
        // TODO: P1 — 调用后端 API 获取空间推荐列表
        kotlinx.coroutines.delay(500)
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("发现空间", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(spaces) { space ->
                    SpaceCardItem(space)
                }
            }
        }
    }
}

@Composable
fun SpaceCardItem(space: SpaceItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 缩略图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6C5CE7), Color(0xFF4A90D9))
                        )
                    ),
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    modifier = Modifier.padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        "${space.onlineCount} 在线",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(space.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(space.description, color = Color.Gray, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    space.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF6C5CE7).copy(alpha = 0.15f)
                        ) {
                            Text(
                                tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = Color(0xFF6C5CE7),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// MARK: - 空间创作页

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSpaceScreen() {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("PUBLIC") }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("创建空间", fontWeight = FontWeight.Bold) })

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("空间名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("空间描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            Text("可见性", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = visibility == "PUBLIC",
                    onClick = { visibility = "PUBLIC" },
                    label = { Text("公开") }
                )
                FilterChip(
                    selected = visibility == "FRIENDS_ONLY",
                    onClick = { visibility = "FRIENDS_ONLY" },
                    label = { Text("仅好友") }
                )
                FilterChip(
                    selected = visibility == "PRIVATE",
                    onClick = { visibility = "PRIVATE" },
                    label = { Text("私密") }
                )
            }

            Button(
                onClick = {
                    isSubmitting = true
                    // TODO: P1 — 调用后端 API 创建空间
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && !isSubmitting
            ) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text("创建空间")
            }
        }
    }
}

// MARK: - 虚拟人对话页

@Composable
fun AvatarScreen() {
    val conversations = remember {
        listOf(
            AvatarConversation("1", "茶馆小助手", "欢迎来到赛博茶馆！"),
            AvatarConversation("2", "艺术导览员", "这幅作品由AI生成..."),
            AvatarConversation("3", "代码导师", "你的代码写得不错！"),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("虚拟人对话", fontWeight = FontWeight.Bold) })

        LazyColumn(contentPadding = PaddingValues(8.dp)) {
            items(conversations) { conv ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = Color(0xFF6C5CE7).copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🤖", fontSize = 20.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(conv.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                conv.lastMessage,
                                color = Color.Gray,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
        }
    }
}

// MARK: - 个人中心页

@Composable
fun ProfileScreen() {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("我的", fontWeight = FontWeight.Bold) })

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 头像
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF6C5CE7), Color(0xFF4A90D9))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("探索者", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("信仰等级 Lv.12", color = Color.Gray, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(20.dp))

            // 统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn("12", "信仰等级")
                StatColumn("0", "创建空间")
                StatColumn("3", "虚拟人")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 功能列表
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column {
                    ProfileMenuItem("成就", Icons.Filled.Star)
                    HorizontalDivider()
                    ProfileMenuItem("我的空间", Icons.Filled.Folder)
                    HorizontalDivider()
                    ProfileMenuItem("设置", Icons.Filled.Settings)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { /* TODO: 退出登录 */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("退出登录")
            }
        }
    }
}

@Composable
fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C5CE7))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ProfileMenuItem(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF6C5CE7))
        Text(title, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}

// MARK: - 数据模型

data class SpaceItem(
    val id: String,
    val title: String,
    val description: String,
    val tags: List<String>,
    val onlineCount: Int
)

data class AvatarConversation(
    val id: String,
    val name: String,
    val lastMessage: String
)

data class TabItem(val route: String, val label: String, val icon: ImageVector)
