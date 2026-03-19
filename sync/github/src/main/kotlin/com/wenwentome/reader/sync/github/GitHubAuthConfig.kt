package com.wenwentome.reader.sync.github

data class GitHubAuthConfig(
    val owner: String,
    val repo: String,
    val branch: String,
    val token: String,
)
