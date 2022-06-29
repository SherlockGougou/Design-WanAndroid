package com.lowe.wanandroid.ui.home.child.square

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.lowe.multitype.paging.MultiTypePagingAdapter
import com.lowe.wanandroid.R
import com.lowe.wanandroid.base.app.AppViewModel
import com.lowe.wanandroid.databinding.FragmentHomeChildSquareBinding
import com.lowe.wanandroid.services.model.Article
import com.lowe.wanandroid.services.model.CollectEvent
import com.lowe.wanandroid.ui.ArticleDiffCalculator
import com.lowe.wanandroid.ui.BaseFragment
import com.lowe.wanandroid.ui.home.HomeChildFragmentAdapter
import com.lowe.wanandroid.ui.home.HomeFragment
import com.lowe.wanandroid.ui.home.HomeTabBean
import com.lowe.wanandroid.ui.home.HomeViewModel
import com.lowe.wanandroid.ui.home.item.ArticleAction
import com.lowe.wanandroid.ui.home.item.HomeArticleItemBinderV2
import com.lowe.wanandroid.ui.web.WebActivity
import com.lowe.wanandroid.utils.isEmpty
import com.lowe.wanandroid.utils.isRefreshing
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class SquareFragment :
    BaseFragment<SquareViewModel, FragmentHomeChildSquareBinding>(R.layout.fragment_home_child_square) {

    companion object {
        fun newInstance(homeTabBean: HomeTabBean) = with(SquareFragment()) {
            arguments = bundleOf(
                HomeFragment.KEY_CHILD_HOME_TAB_PARCELABLE to homeTabBean
            )
            this
        }
    }

    @Inject
    lateinit var appViewModel: AppViewModel

    private val homeViewModel by viewModels<HomeViewModel>(this::requireParentFragment)
    private val squareTabBean by lazy(LazyThreadSafetyMode.NONE) {
        arguments?.getParcelable(HomeFragment.KEY_CHILD_HOME_TAB_PARCELABLE) ?: HomeTabBean(
            HomeChildFragmentAdapter.HOME_TAB_SQUARE
        )
    }
    private val squareAdapter =
        MultiTypePagingAdapter(ArticleDiffCalculator.getCommonArticleDiffItemCallback()).apply {
            register(HomeArticleItemBinderV2(this@SquareFragment::onItemClick))
        }

    override val viewModel: SquareViewModel by viewModels()

    override fun init(savedInstanceState: Bundle?) {
        initView()
        initEvents()
    }

    private fun initView() {
        viewBinding.apply {
            with(squareList) {
                layoutManager = LinearLayoutManager(context)
                adapter = squareAdapter
                setHasFixedSize(true)
            }
        }
    }

    private fun initEvents() {
        lifecycleScope.launchWhenCreated {
            viewModel.getSquareFlow().collectLatest(squareAdapter::submitData)
        }
        lifecycleScope.launchWhenCreated {
            squareAdapter.loadStateFlow.collect(this@SquareFragment::updateLoadStates)
        }
        homeViewModel.apply {
            scrollToTopLiveData.observe(viewLifecycleOwner) {
                if (it.title == squareTabBean.title) scrollToTop()
            }
            refreshLiveData.observe(viewLifecycleOwner) {
                if (it.title == squareTabBean.title) onRefresh()
            }
        }
        appViewModel.collectArticleEvent.observe(viewLifecycleOwner) { event ->
            squareAdapter.snapshot().run {
                val index = indexOfFirst { it is Article && it.id == event.id }
                if (index >= 0) {
                    (this[index] as? Article)?.collect = event.isCollected
                    index
                } else null
            }?.apply(squareAdapter::notifyItemChanged)
        }
    }

    private fun scrollToTop() {
        viewBinding.squareList.scrollToPosition(0)
    }

    private fun onRefresh() {
        squareAdapter.refresh()
    }

    private fun onItemClick(articleAction: ArticleAction) {
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

    private fun updateLoadStates(loadStates: CombinedLoadStates) {
        viewBinding.loadingContainer.apply {
            emptyLayout.isVisible =
                loadStates.refresh is LoadState.NotLoading && squareAdapter.isEmpty()
            loadingProgress.isVisible = squareAdapter.isEmpty() && loadStates.isRefreshing
        }
    }
}