package club.asynclab.limboauthemail.misc

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class HackField<T : Any, V> : ReadWriteProperty<T, V> {
    override fun getValue(thisRef: T, property: KProperty<*>): V {
        val field = thisRef::class.java.superclass
            ?.getDeclaredField(property.name)
            ?: throw NoSuchFieldException("${property.name} not found in superclass")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(thisRef) as V
    }

    override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        val field = thisRef::class.java.superclass
            ?.getDeclaredField(property.name)
            ?: throw NoSuchFieldException("${property.name} not found in superclass")
        field.isAccessible = true
        field.set(thisRef, value)
    }
}