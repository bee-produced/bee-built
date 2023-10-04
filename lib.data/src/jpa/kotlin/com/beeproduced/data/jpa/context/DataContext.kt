package com.beeproduced.data.jpa.context

import com.beeproduced.data.selection.DataSelection

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2023-03-10
 */
class DataContext(
    val selection: DataSelection
) {
    companion object {
        val context = ThreadLocal<DataContext>()

        fun selection(selection: DataSelection) {
            context.set(DataContext(selection))
        }
        
        fun selection(): DataSelection {
            return context.get().selection
        }
    }
}