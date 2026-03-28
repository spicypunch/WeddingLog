package kr.jm.weddingledger.core.platform

import java.util.UUID

actual fun randomUuidString(): String = UUID.randomUUID().toString()
