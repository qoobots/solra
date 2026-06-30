import SwiftUI

/// 主内容视图 — Tab 导航框架
/// Tab: 空间发现 → 创作 → 虚拟人 → 个人中心
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
        .tint(.purple)
    }
}

// MARK: - 空间发现页

struct SpaceFeedView: View {
    @State private var spaces: [SpaceItem] = []
    @State private var isLoading = true
    @State private var errorMessage: String?

    var body: some View {
        Group {
            if isLoading {
                ProgressView("加载空间...")
            } else if let error = errorMessage {
                VStack(spacing: 16) {
                    Text(error).foregroundColor(.secondary)
                    Button("重试") { loadSpaces() }
                }
            } else {
                ScrollView {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                        ForEach(spaces) { space in
                            NavigationLink(destination: SpaceDetailView(space: space)) {
                                SpaceCard(space: space)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding()
                }
            }
        }
        .onAppear { loadSpaces() }
    }

    private func loadSpaces() {
        isLoading = true
        errorMessage = nil
        // TODO: P1 — 调用后端 API 获取空间推荐列表
        // SolraAPI.shared.fetchSpaces { result in ... }
        spaces = SpaceItem.samples
        isLoading = false
    }
}

struct SpaceCard: View {
    let space: SpaceItem

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // 缩略图占位
            RoundedRectangle(cornerRadius: 12)
                .fill(LinearGradient(
                    colors: [.purple, .blue],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ))
                .frame(height: 140)
                .overlay(alignment: .topTrailing) {
                    Text("\(space.onlineCount) 在线")
                        .font(.caption2)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(.ultraThinMaterial)
                        .clipShape(Capsule())
                        .padding(8)
                }

            VStack(alignment: .leading, spacing: 4) {
                Text(space.title)
                    .font(.headline)
                    .lineLimit(1)

                Text(space.description)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)

                HStack {
                    ForEach(space.tags.prefix(3), id: \.self) { tag in
                        Text(tag)
                            .font(.caption2)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(.purple.opacity(0.2))
                            .foregroundColor(.purple)
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(.horizontal, 8)
            .padding(.bottom, 8)
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
    }
}

// MARK: - 空间详情页

struct SpaceDetailView: View {
    let space: SpaceItem
    @State private var chatMessages: [ChatMessage] = []
    @State private var inputText = ""
    @State private var isStreaming = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Hero
                RoundedRectangle(cornerRadius: 16)
                    .fill(LinearGradient(colors: [.purple, .blue], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(height: 240)
                    .overlay(alignment: .bottomLeading) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(space.title).font(.title2).bold()
                            Text("\(space.onlineCount) 在线")
                                .font(.subheadline)
                        }
                        .foregroundColor(.white)
                        .padding()
                    }

                // 描述
                Text(space.description)
                    .font(.body)
                    .foregroundColor(.secondary)

                // AI 虚拟人对话
                VStack(alignment: .leading, spacing: 8) {
                    Label("AI 虚拟人", systemImage: "brain.head.profile")
                        .font(.headline)

                    ScrollView {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(chatMessages) { msg in
                                HStack {
                                    if msg.role == .user { Spacer() }
                                    Text(msg.content)
                                        .padding(10)
                                        .background(msg.role == .user ? Color.purple : Color(.systemGray5))
                                        .foregroundColor(msg.role == .user ? .white : .primary)
                                        .clipShape(RoundedRectangle(cornerRadius: 12))
                                        .frame(maxWidth: 260, alignment: msg.role == .user ? .trailing : .leading)
                                    if msg.role == .avatar { Spacer() }
                                }
                            }
                        }
                    }
                    .frame(height: 200)

                    HStack {
                        TextField("输入消息...", text: $inputText)
                            .textFieldStyle(.roundedBorder)
                            .disabled(isStreaming)
                        Button("发送") { sendMessage() }
                            .disabled(inputText.isEmpty || isStreaming)
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .padding()
        }
        .navigationBarTitleDisplayMode(.inline)
    }

    private func sendMessage() {
        guard !inputText.isEmpty else { return }
        let msg = inputText
        chatMessages.append(ChatMessage(role: .user, content: msg))
        inputText = ""
        isStreaming = true

        // TODO: P1 — 调用 AI 虚拟人 API
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            chatMessages.append(ChatMessage(role: .avatar, content: "[Mock] 收到你的消息：\(msg)"))
            isStreaming = false
        }
    }
}

// MARK: - 空间创作页

struct CreationView: View {
    @State private var title = ""
    @State private var description = ""
    @State private var visibility = "PUBLIC"
    @State private var isSubmitting = false

    var body: some View {
        Form {
            Section("空间信息") {
                TextField("空间名称", text: $title)
                TextField("空间描述", text: $description, axis: .vertical)
                    .lineLimit(3...6)
            }

            Section("可见性") {
                Picker("可见性", selection: $visibility) {
                    Text("公开").tag("PUBLIC")
                    Text("仅好友").tag("FRIENDS_ONLY")
                    Text("私密").tag("PRIVATE")
                }
            }

            Section {
                Button(action: createSpace) {
                    if isSubmitting {
                        ProgressView()
                    } else {
                        Text("创建空间")
                    }
                }
                .disabled(title.isEmpty || isSubmitting)
            }
        }
    }

    private func createSpace() {
        isSubmitting = true
        // TODO: P1 — 调用后端 API 创建空间
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            isSubmitting = false
        }
    }
}

// MARK: - 虚拟人对话页

struct AvatarView: View {
    @State private var conversations: [AvatarConversation] = AvatarConversation.samples

    var body: some View {
        List(conversations) { conv in
            VStack(alignment: .leading, spacing: 4) {
                Text(conv.avatarName)
                    .font(.headline)
                Text(conv.lastMessage)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
                Text(conv.updatedAt, style: .relative)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            .padding(.vertical, 4)
        }
    }
}

// MARK: - 个人中心页

struct ProfileView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        List {
            Section {
                HStack(spacing: 16) {
                    Circle()
                        .fill(LinearGradient(colors: [.purple, .blue], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: 64, height: 64)
                        .overlay(Text("👤").font(.largeTitle))

                    VStack(alignment: .leading, spacing: 4) {
                        Text(appState.currentUser?.displayName ?? "探索者")
                            .font(.title3).bold()
                        Text("信仰等级 Lv.12")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.vertical, 8)
            }

            Section("统计") {
                HStack {
                    StatItem(value: "12", label: "信仰等级")
                    Divider()
                    StatItem(value: "0", label: "创建空间")
                    Divider()
                    StatItem(value: "3", label: "虚拟人")
                }
            }

            Section {
                NavigationLink("成就") { Text("成就系统开发中") }
                NavigationLink("我的空间") { Text("空间列表开发中") }
                NavigationLink("设置") { Text("设置开发中") }
            }

            Section {
                Button("退出登录", role: .destructive) {
                    appState.isLoggedIn = false
                    appState.currentUser = nil
                }
            }
        }
    }
}

struct StatItem: View {
    let value: String
    let label: String
    var body: some View {
        VStack(spacing: 4) {
            Text(value).font(.title3).bold().foregroundColor(.purple)
            Text(label).font(.caption).foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - 数据模型

struct SpaceItem: Identifiable {
    let id = UUID()
    let spaceId: String
    let title: String
    let description: String
    let tags: [String]
    let onlineCount: Int

    static let samples = [
        SpaceItem(spaceId: "1", title: "赛博茶馆", description: "聊天交友的虚拟茶室", tags: ["社交", "聊天"], onlineCount: 42),
        SpaceItem(spaceId: "2", title: "星空画廊", description: "AI艺术展览空间", tags: ["艺术", "AI"], onlineCount: 18),
        SpaceItem(spaceId: "3", title: "代码峡谷", description: "程序员聚集地", tags: ["技术", "学习"], onlineCount: 67),
        SpaceItem(spaceId: "4", title: "音乐森林", description: "虚拟演唱会现场", tags: ["音乐", "演出"], onlineCount: 103),
        SpaceItem(spaceId: "5", title: "禅意庭院", description: "冥想放松空间", tags: ["冥想", "自然"], onlineCount: 9),
        SpaceItem(spaceId: "6", title: "赛博竞技场", description: "对抗竞技空间", tags: ["游戏", "竞技"], onlineCount: 55),
    ]
}

struct ChatMessage: Identifiable {
    let id = UUID()
    let role: MessageRole
    let content: String
}

enum MessageRole {
    case user, avatar
}

struct AvatarConversation: Identifiable {
    let id = UUID()
    let avatarName: String
    let lastMessage: String
    let updatedAt: Date

    static let samples = [
        AvatarConversation(avatarName: "茶馆小助手", lastMessage: "欢迎来到赛博茶馆！", updatedAt: Date()),
        AvatarConversation(avatarName: "艺术导览员", lastMessage: "这幅作品由AI生成...", updatedAt: Date().addingTimeInterval(-3600)),
        AvatarConversation(avatarName: "代码导师", lastMessage: "你的代码写得不错！", updatedAt: Date().addingTimeInterval(-86400)),
    ]
}

#Preview {
    ContentView()
        .environmentObject(AppState())
}
