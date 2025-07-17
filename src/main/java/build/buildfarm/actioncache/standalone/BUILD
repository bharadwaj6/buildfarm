package(default_visibility = ["//visibility:public"])

java_library(
    name = "standalone",
    srcs = [
        "ActionCache.java",
        "StandaloneActionCache.java",
    ],
    deps = [
        "//src/main/java/build/buildfarm/admin/cache/adapter/ac",
        "//src/main/java/build/buildfarm/admin/cache/model",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@remoteapis//build/bazel/remote/execution/v2:remote_execution_java_proto",
    ],
)