java_library(
    name = "api",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//src/main/java/build/buildfarm/admin/cache",
        "//src/main/java/build/buildfarm/admin/cache/model",
        "//src/main/java/build/buildfarm/common",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:javax_ws_rs_javax_ws_rs_api",
        "@maven//:javax_inject_javax_inject",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
    ],
)