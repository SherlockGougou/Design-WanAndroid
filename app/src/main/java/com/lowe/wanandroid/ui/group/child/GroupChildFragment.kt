package com.lowe.wanandroid.ui.group.child

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lowe.multitype.paging.MultiTypePagingAdapter
import com.lowe.wanandroid.R
import com.lowe.wanandroid.base.app.AppViewModel
import com.lowe.wanandroid.databinding.FragmentChildGroupBinding
import com.lowe.wanandroid.services.model.Article
import com.lowe.wanandroid.services.model.CollectEvent
import com.lowe.wanandroid.ui.ArticleDiffCalculator
import com.lowe.wanandroid.ui.BaseFragment
import com.lowe.wanandroid.ui.group.GroupViewModel
import com.lowe.wanandroid.ui.home.item.ArticleAction
import com.lowe.wanandroid.ui.home.item.HomeArticleItemBinderV2
import com.lowe.wanandroid.ui.web.WebActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class GroupChildFragment :
    BaseFragment<GroupChildViewModel, FragmentChildGroupBinding>(R.layout.fragment_child_group) {

    companion object {

        const val KEY_BUNDLE_GROUP_CHILD_ID = "key_bundle_group_child_id"

        fun newInstance(id: Int) =
            with(GroupChildFragment()) {
                arguments = bundleOf(
                    KEY_BUNDLE_GROUP_CHILD_ID to id
                )
                this
            }
    }

    @Inject
    lateinit var appViewModel: AppViewModel

    private val authorId by lazy(LazyThreadSafetyMode.NONE) {
        arguments?.getInt(
            KEY_BUNDLE_GROUP_CHILD_ID, 0
        ) ?: 0
    }

    private val articlesAdapter =
        MultiTypePagingAdapter(ArticleDiffCalculator.getCommonArticleDiffItemCallback()).apply {
            register(HomeArticleItemBinderV2(this@GroupChildFragment::onArticleClick))
        }
    private val groupViewModel by viewModels<GroupViewModel>(this::requireParentFragment)

    override val viewModel: GroupChildViewModel by viewModels()

    override fun init(savedInstanceState: Bundle?) {
        initView()
        initEvents()
    }

    private fun initView() {
        viewBinding.apply {
            with(childList) {
                adapter = articlesAdapter
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
            }
        }
    }

    private fun initEvents() {
        lifecycleScope.launchWhenCreated {
            viewModel.getGroupArticlesFlow(authorId).collectLatest(articlesAdapter::submitData)
        }
        groupViewModel.apply {
            scrollToTopLiveData.observe(viewLifecycleOwner) {
                if (it == authorId) scrollToTop()
            }
            parentRefreshLiveData.observe(viewLifecycleOwner) {
                if (it == authorId) articlesAdapter.refresh()
            }
        }
        appViewModel.collectArticleEvent.observe(viewLifecycleOwner) { event ->
            articlesAdapter.snapshot().run {
                val index = indexOfFirst { it is Article && it.id == event.id }
                if (index >= 0) {
                    (this[index] as? Article)?.collect = event.isCollected
                    index
                } else null
            }?.apply(articlesAdapter::notifyItemChanged)
        }
    }

    private fun onArticleClick(articleAction: ArticleAction) {
        when (articleAction) {
            is ArticleAction.ItemClick -> WebActivity.loadUrl(
                requireContext(),
                articleAction.article.link
            )
            is ArticleAction.CollectClick -> {
                appViewModel.articleCollectAction(
                    CollectEvent(
                        articleAction.article.id,
                        articleAction.article.link,
                        articleAction.article.collect.not()
                    )
                )
            }
        }
    }

    private fun scrollToTop() {
        viewBinding.childList.scrollToPosition(0)
    }
}