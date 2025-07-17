java_library(
    name = "model",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//src/main/java/build/buildfarm/common",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        # Temporarily comment out Jackson annotations due to visibility issues
        # "@maven//:com_fasterxml_jackson_core_jackson_annotations",
    ],
)