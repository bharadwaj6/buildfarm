java_library(
    name = "cas",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//src/main/java/build/buildfarm/admin/cache/model",
        "//src/main/java/build/buildfarm/common",
        "//src/main/java/build/buildfarm/common/redis",
        "//src/main/java/build/buildfarm/cas",
        "//src/main/java/build/buildfarm/instance/shard",
        "//src/main/protobuf/build/buildfarm/v1test:buildfarm_java_proto",
        "//src/main/protobuf/build/buildfarm/v1test:buildfarm_proto",
        "@remoteapis//build/bazel/remote/execution/v2:remote_execution_java_proto",
        "@remoteapis//build/bazel/remote/execution/v2:remote_execution_proto",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:io_prometheus_simpleclient",
        "@maven//:redis_clients_jedis",
        # Removed javax.inject dependency as we're not using @Inject annotations
        # "@maven//:javax_inject_javax_inject",
    ],
)