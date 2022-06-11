package com.lowe.wanandroid.ui.navigator

import com.lowe.wanandroid.base.SimpleDiffCalculator
import com.lowe.wanandroid.services.model.Article
import com.lowe.wanandroid.services.model.Classify
import com.lowe.wanandroid.services.model.Navigation
import com.lowe.wanandroid.services.model.Series
import com.lowe.wanandroid.ui.navigator.child.navigator.NavigatorChildPayload
import com.lowe.wanandroid.ui.navigator.child.navigator.TagSelectedChange

object NavigatorDiffCalculator {

    fun getNavigatorDiffCalculator(oldList: List<Any>, newList: List<Any>) =
        SimpleDiffCalculator(
            oldList,
            newList,
            { oldItem: Any, newItem: Any ->
                when {
                    oldItem is Navigation && newItem is Navigation -> oldItem.name == newItem.name
                    oldItem is Article && newItem is Article -> oldItem.id == newItem.id
                    oldItem is Series && newItem is Series -> oldItem.id == newItem.id
                    oldItem is Classify && newItem is Classify -> oldItem.id == newItem.id
                    else -> oldItem.javaClass == newItem.javaClass
                }
            },
            { oldItem: Any, newItem: Any ->
                when {
                    oldItem is Navigation && newItem is Navigation -> oldItem == newItem && oldItem.isSelected == newItem.isSelected
                    oldItem is Series && newItem is Series -> oldItem == newItem
                    oldItem is Classify && newItem is Classify -> oldItem == newItem
                    oldItem is Article && newItem is Article -> oldItem == newItem
                    else -> oldItem.javaClass == newItem.javaClass && oldItem == newItem
                }
            },
            { oldItem: Any, newItem: Any ->
                when {
                    oldItem is Navigation && newItem is Navigation -> {
                        if (oldItem.isSelected != newItem.isSelected) listOf<NavigatorChildPayload>(
                            TagSelectedChange
                        ) else null
                    }
                    else -> null
                }
            }
        )
}