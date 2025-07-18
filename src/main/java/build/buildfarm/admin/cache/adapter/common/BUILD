java_library(
    name = "common",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//src/main/java/build/buildfarm/admin/cache/model",
        "//src/main/java/build/buildfarm/common",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@remoteapis//build/bazel/remote/execution/v2:remote_execution_java_proto",
    ],
)