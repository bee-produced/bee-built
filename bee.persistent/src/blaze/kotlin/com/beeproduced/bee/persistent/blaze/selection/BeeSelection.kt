package com.beeproduced.bee.persistent.blaze.selection

/**
 * @author Kacper Urbaniec
 * @version 2023-12-31
 */
interface BeeSelection {
  // This will return true if the field selection set matches a specified "glob" pattern
  // matching ie the glob pattern matching supported by java.nio.file.FileSystem.getPathMatcher.
  fun contains(fieldGlobPattern: String): Boolean

  // Get selector for specified field if existing
  fun subSelect(fieldGlobPattern: String): BeeSelection?

  fun merge(vararg selections: BeeSelection): BeeSelection

  fun merge(selections: Collection<BeeSelection>): BeeSelection

  fun typeSelect(typeName: String): BeeSelection?

  fun immediateFields(): List<FieldNodeDefinition>

  companion object {
    class SelectionBuilder internal constructor() {
      internal val fields = mutableSetOf<FieldNodeDefinition>()

      fun field(name: String, type: String? = null) {
        fields.add(DefaultBeeSelection.TypedFieldNode(name, type))
      }

      fun fields(vararg names: String) {
        val nodes = names.map { DefaultBeeSelection.FieldNode(it) }
        fields.addAll(nodes)
      }

      fun fields(vararg fields: Pair<String, String?>) {
        val nodes = fields.map { (name, type) -> DefaultBeeSelection.TypedFieldNode(name, type) }
        this.fields.addAll(nodes)
      }

      fun field(name: String, type: String? = null, subFields: SelectionBuilder.() -> Unit) {
        val subNodes = SelectionBuilder()
        subFields(subNodes)
        fields.add(DefaultBeeSelection.TypedFieldNode(name, type, subNodes.fields))
      }
    }

    // fun test() {
    //     val selection = BeeSelection.create {
    //         field("foo")
    //         field("bar") {
    //             field("foxtrot")
    //         }
    //         fields("alpha" to "beta", "delta" to null)
    //     }
    // }
    @JvmStatic
    fun create(create: SelectionBuilder.() -> Unit): BeeSelection {
      val selectionBuilder = SelectionBuilder()
      selectionBuilder.create()
      return DefaultBeeSelection(selectionBuilder.fields)
    }

    @JvmStatic
    fun empty(): BeeSelection {
      return DefaultBeeSelection(emptySet())
    }
  }
}

inline fun <reified C> BeeSelection.typeSelect(): BeeSelection? {
  val typeName = C::class.simpleName ?: throw Exception("Given class has no name")
  return typeSelect(typeName)
}
