java_test(
    name = "ActionCacheAdapterTest",
    srcs = ["ActionCacheAdapterTest.java"],
    deps = [
        "//src/main/java/build/buildfarm/admin/cache/adapter/ac",
        "//src/main/java/build/buildfarm/admin/cache/model",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@remoteapis//build/bazel/remote/execution/v2:remote_execution_java_proto",
    ],
)

java_test(
    name = "RedisActionCacheAdapterTest",
    srcs = ["RedisActionCacheAdapterTest.java"],
    deps = [
        "//src/main/java/build/buildfarm/admin/cache/adapter/ac",
        "//src/main/java/build/buildfarm/admin/cache/model",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:redis_clients_jedis",
    ],
)

java_test(
    name = "InMemoryActionCacheAdapterTest",
    srcs = ["InMemoryActionCacheAdapterTest.java"],
    deps = [
        "//src/main/java/build/buildfarm/admin/cache/adapter/ac",
        "//src/main/java/build/buildfarm/admin/cache/model",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@remoteapis//build/bazel/remote/execution/v2:remote_execution_java_proto",
    ],
)