package com.qonversion.android.sdk

import java.lang.reflect.Modifier

fun Any.mockPrivateField(fieldName: String, field: Any?) {
    javaClass.declaredFields
        .filter { it.modifiers.and(Modifier.PRIVATE) > 0 || it.modifiers.and(Modifier.PROTECTED) > 0 }
        .firstOrNull { it.name == fieldName }
        ?.also { it.isAccessible = true }
        ?.set(this, field)
}

fun <T> Any.getPrivateField(name: String): T {
    val field = this::class.java.getDeclaredField(name)
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return field.get(this) as T
}

fun Boolean.toInt() = if (this) 1 else 0
