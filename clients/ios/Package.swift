// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SolraApp",
    platforms: [
        .iOS(.v17),
    ],
    products: [
        .library(name: "SolraApp", targets: ["SolraApp"]),
    ],
    dependencies: [
        // TODO: P1 — gRPC Swift / Protobuf Swift 依赖
    ],
    targets: [
        .target(
            name: "SolraApp",
            dependencies: []
        ),
    ]
)
