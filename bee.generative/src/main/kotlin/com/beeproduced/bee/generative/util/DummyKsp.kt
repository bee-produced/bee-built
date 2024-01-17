package com.beeproduced.bee.generative.util

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-12-16
 */


import com.google.devtools.ksp.symbol.*

object DummyKsp {
    val propertyDeclaration = DummyKSPropertyDeclaration()
    val type = DummyKSType()
}

class DummyKSPropertyDeclaration : KSPropertyDeclaration {
    override val annotations: Sequence<KSAnnotation>
        get() = emptySequence() // Empty sequence for dummy implementation

    override val containingFile: KSFile?
        get() = null // Null for dummy implementation

    override val docString: String?
        get() = null // Null for dummy implementation

    override val extensionReceiver: KSTypeReference?
        get() = null // Null for dummy implementation

    override val getter: KSPropertyGetter?
        get() = null // Null for dummy implementation

    override val hasBackingField: Boolean
        get() = false // Default to false

    override val isActual: Boolean
        get() = false // Default to false

    override val isExpect: Boolean
        get() = false // Default to false

    override val isMutable: Boolean
        get() = false // Default to false

    override val location: Location
        get() = NonExistLocation // Using Location.None for simplicity

    override val modifiers: Set<Modifier>
        get() = emptySet() // Empty set for dummy implementation

    override val origin: Origin
        get() = Origin.SYNTHETIC // Using SYNTHETIC for dummy implementation

    override val packageName: KSName
        get() = DummyKSName()

    override val parent: KSNode?
        get() = null // Null for dummy implementation

    override val parentDeclaration: KSDeclaration?
        get() = null // Null for dummy implementation

    override val qualifiedName: KSName?
        get() = DummyKSName()

    override val setter: KSPropertySetter?
        get() = null // Null for dummy implementation

    override val simpleName: KSName
        get() = DummyKSName()

    override val type: KSTypeReference
        get() = DummyKSTypeReference() // A dummy type reference

    override val typeParameters: List<KSTypeParameter>
        get() = emptyList() // Empty list for dummy implementation

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        throw NotImplementedError("Dummy implementation does not support this method.")
    }

    override fun asMemberOf(containing: KSType): KSType {
        throw NotImplementedError("Dummy implementation does not support this method.")
    }

    override fun findActuals(): Sequence<KSDeclaration> {
        return emptySequence() // Empty sequence for dummy implementation
    }

    override fun findExpects(): Sequence<KSDeclaration> {
        return emptySequence() // Empty sequence for dummy implementation
    }

    override fun findOverridee(): KSPropertyDeclaration? {
        return null // Null for dummy implementation
    }

    override fun isDelegated(): Boolean {
        return false // Default to false
    }
}

class DummyKSName : KSName {
    override fun asString(): String = ""

    override fun getQualifier(): String = ""

    override fun getShortName(): String = ""
}

class DummyKSTypeReference : KSTypeReference {
    override val annotations: Sequence<KSAnnotation>
        get() = emptySequence()

    override val element: KSReferenceElement
        get() = throw NotImplementedError("Dummy implementation does not support this property.")
    override val location: Location
        get() = NonExistLocation
    override val modifiers: Set<Modifier>
        get() = emptySet()

    override val origin: Origin
        get() = Origin.SYNTHETIC

    override val parent: KSNode?
        get() = null

    override fun resolve(): KSType {
        throw NotImplementedError("Dummy implementation does not support this method.")
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        throw NotImplementedError("Dummy implementation does not support this method.")
    }
}

class DummyKSType : KSType {
    override val annotations: Sequence<KSAnnotation>
        get() = emptySequence()
    override val arguments: List<KSTypeArgument>
        get() = emptyList()
    override val declaration: KSDeclaration
        get() = TODO("Not yet implemented")
    override val isError: Boolean
        get() = false
    override val isFunctionType: Boolean
        get() = false
    override val isMarkedNullable: Boolean
        get() = false
    override val isSuspendFunctionType: Boolean
        get() = false
    override val nullability: Nullability
        get() = Nullability.NOT_NULL

    override fun isAssignableFrom(that: KSType): Boolean = false

    override fun isCovarianceFlexible(): Boolean = false

    override fun isMutabilityFlexible(): Boolean = false

    override fun makeNotNullable(): KSType = DummyKSType()

    override fun makeNullable(): KSType = DummyKSType()

    override fun replace(arguments: List<KSTypeArgument>): KSType = DummyKSType()

    override fun starProjection(): KSType = DummyKSType()

}
