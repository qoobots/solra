import SwiftUI

/// 主内容视图 — Tab 导航框架
/// Tab: 空间消费 → 创作 → 虚拟人 → 个人中心
struct ContentView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        TabView {
            NavigationStack {
                SpaceFeedView()
                    .navigationTitle("发现")
            }
            .tabItem {
                Label("发现", systemImage: "compass.drawing")
            }

            NavigationStack {
                CreationView()
                    .navigationTitle("创作")
            }
            .tabItem {
                Label("创作", systemImage: "cube.transparent")
            }

            NavigationStack {
                AvatarView()
                    .navigationTitle("虚拟人")
            }
            .tabItem {
                Label("虚拟人", systemImage: "person.crop.circle")
            }

            NavigationStack {
                ProfileView()
                    .navigationTitle("我的")
            }
            .tabItem {
                Label("我的", systemImage: "person.circle")
            }
        }
    }
}

// MARK: - Placeholder Views

struct SpaceFeedView: View {
    var body: some View {
        // TODO: P1 — 空间推荐流
        Text("空间发现")
    }
}

struct CreationView: View {
    var body: some View {
        // TODO: P1 — 创作入口
        Text("空间创作")
    }
}

struct AvatarView: View {
    var body: some View {
        // TODO: P1 — 虚拟人对话
        Text("虚拟人对话")
    }
}

struct ProfileView: View {
    var body: some View {
        // TODO: P1 — 个人中心
        Text("个人中心")
    }
}

#Preview {
    ContentView()
        .environmentObject(AppState())
}
