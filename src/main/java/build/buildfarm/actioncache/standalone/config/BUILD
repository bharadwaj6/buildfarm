java_library(
    name = "config",
    srcs = [
        "ActionCacheConfig.java",
        "ActionCacheConfigLoader.java",
        "ActionCacheFactory.java",
        "AdapterConfig.java",
        "AdapterType.java",
        "CacheEvictionPolicy.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//src/main/java/build/buildfarm/actioncache/standalone",
        "//src/main/java/build/buildfarm/admin/cache/adapter/ac",
        "//src/main/java/build/buildfarm/admin/cache/model",
        "@com_google_guava//jar",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:org_yaml_snakeyaml",
    ],
)
