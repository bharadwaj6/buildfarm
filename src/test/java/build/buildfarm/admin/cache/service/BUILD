java_test(
    name = "CacheFlushServiceConcurrencyTest",
    srcs = ["CacheFlushServiceConcurrencyTest.java"],
    test_class = "build.buildfarm.admin.cache.service.CacheFlushServiceConcurrencyTest",
    deps = [
        "//src/main/java/build/buildfarm/admin/cache/adapter/ac",
        "//src/main/java/build/buildfarm/admin/cache/adapter/cas",
        "//src/main/java/build/buildfarm/admin/cache/concurrency",
        "//src/main/java/build/buildfarm/admin/cache/model",
        "//src/main/java/build/buildfarm/admin/cache/service",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)