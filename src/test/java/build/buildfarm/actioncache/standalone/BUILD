package(default_visibility = ["//visibility:public"])

java_test(
    name = "ActionCacheTest",
    srcs = ["ActionCacheTest.java"],
    deps = [
        "//src/main/java/build/buildfarm/actioncache/standalone",
        "//src/main/java/build/buildfarm/admin/cache/model",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@remoteapis//build/bazel/remote/execution/v2:remote_execution_java_proto",
    ],
)

java_test(
    name = "StandaloneActionCacheTest",
    srcs = ["StandaloneActionCacheTest.java"],
    deps = [
        "//src/main/java/build/buildfarm/actioncache/standalone",
        "//src/main/java/build/buildfarm/admin/cache/adapter/ac",
        "//src/main/java/build/buildfarm/admin/cache/model",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@remoteapis//build/bazel/remote/execution/v2:remote_execution_java_proto",
    ],
)