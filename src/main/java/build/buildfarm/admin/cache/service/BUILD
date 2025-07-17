java_library(
    name = "service",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//src/main/java/build/buildfarm/admin/cache/model",
        "//src/main/java/build/buildfarm/admin/cache/adapter/ac:ac",
        "//src/main/java/build/buildfarm/admin/cache/adapter/cas:cas",
        "@maven//:com_google_guava_guava",
    ],
)