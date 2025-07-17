java_test(
    name = "FilesystemCASAdapterTest",
    srcs = ["FilesystemCASAdapterTest.java"],
    deps = [
        "//src/main/java/build/buildfarm/admin/cache/adapter/cas",
        "//src/main/java/build/buildfarm/admin/cache/model",
        "//src/main/java/build/buildfarm/cas/cfc:cfc",
        "//src/main/java/build/buildfarm/common",
        "//src/main/protobuf:build_buildfarm_v1test_buildfarm_java_proto",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)

java_test(
    name = "InMemoryLRUCASAdapterTest",
    srcs = ["InMemoryLRUCASAdapterTest.java"],
    deps = [
        "//src/main/java/build/buildfarm/admin/cache/adapter/cas",
        "//src/main/java/build/buildfarm/admin/cache/model",
        "//src/main/java/build/buildfarm/cas:cas",
        "//src/main/java/build/buildfarm/common",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)

java_test(
    name = "RedisCASWorkerMapAdapterTest",
    srcs = ["RedisCASWorkerMapAdapterTest.java"],
    deps = [
        "//src/main/java/build/buildfarm/admin/cache/adapter/cas",
        "//src/main/java/build/buildfarm/admin/cache/model",
        "//src/main/java/build/buildfarm/common",
        "//src/main/java/build/buildfarm/instance/shard",
        "@maven//:com_google_guava_guava",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:redis_clients_jedis",
    ],
)