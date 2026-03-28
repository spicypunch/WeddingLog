package kr.jm.weddingledger.core.platform

import platform.Foundation.NSUUID

actual fun randomUuidString(): String = NSUUID().UUIDString()
