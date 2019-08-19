package com.gykj.zhumulangma.home.fragment;


import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.gykj.zhumulangma.common.AppConstants;
import com.gykj.zhumulangma.common.adapter.NavigatorAdapter;
import com.gykj.zhumulangma.common.bean.AlbumTrackBean;
import com.gykj.zhumulangma.common.event.KeyCode;
import com.gykj.zhumulangma.common.mvvm.BaseMvvmFragment;
import com.gykj.zhumulangma.common.util.ZhumulangmaUtil;
import com.gykj.zhumulangma.home.R;
import com.gykj.zhumulangma.home.adapter.AlbumTrackAdapter;
import com.gykj.zhumulangma.home.mvvm.ViewModelFactory;
import com.gykj.zhumulangma.home.mvvm.viewmodel.AlbumDetailViewModel;
import com.jakewharton.rxbinding3.view.RxView;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;
import com.ximalaya.ting.android.opensdk.model.PlayableModel;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.track.CommonTrackList;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;
import com.ximalaya.ting.android.opensdk.player.service.IXmPlayerStatusListener;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayerException;
import com.ximalaya.ting.android.sdkdownloader.XmDownloadManager;
import com.ximalaya.ting.android.sdkdownloader.downloadutil.DownloadState;
import com.ximalaya.ting.android.sdkdownloader.downloadutil.IXmDownloadTrackCallBack;
import com.ximalaya.ting.android.sdkdownloader.task.Callback;

import net.lucode.hackware.magicindicator.MagicIndicator;
import net.lucode.hackware.magicindicator.ViewPagerHelper;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Route(path = AppConstants.Router.Home.F_ALBUM_DETAIL)
public class AlbumDetailFragment extends BaseMvvmFragment<AlbumDetailViewModel> implements
        ViewPager.OnPageChangeListener, OnLoadMoreListener, BaseQuickAdapter.OnItemClickListener,
        BaseQuickAdapter.OnItemChildClickListener, IXmDownloadTrackCallBack, IXmPlayerStatusListener {

    @Autowired(name = KeyCode.Home.ALBUMID)
    public long mAlbumId;

    private MagicIndicator magicIndicator;
    private ViewPager viewpager;
    private ConstraintLayout clActionbar;

    private ViewGroup layoutDetail;
    private ViewGroup layoutTracks;

    private RefreshLayout refreshLayout;
    private RecyclerView recyclerView;

    private Album mAlbum;
    private String mSort = "asc";
    private boolean isRefresh = true;

    private ImageView ivCover;
    private TextView tvAlbum;
    private TextView tvAuthor;
    private TextView tvPlaycount;
    private TextView tvTrackcount;
    private TextView tvSbcount;
    private String[] tabs = {"详情", "节目"};

    private AlbumTrackAdapter mAlbumTrackAdapter;


    public AlbumDetailFragment() {
    }


    @Override
    public void initView(View view) {

        magicIndicator = view.findViewById(R.id.magic_indicator);
        viewpager = view.findViewById(R.id.viewpager);
        clActionbar = view.findViewById(R.id.cl_actionbar);

        ivCover = view.findViewById(R.id.iv_cover);
        tvAlbum = view.findViewById(R.id.tv_album);
        tvAuthor = view.findViewById(R.id.tv_author);
        tvPlaycount = view.findViewById(R.id.tv_playcount);
        tvTrackcount = view.findViewById(R.id.tv_trackcount);
        tvSbcount = view.findViewById(R.id.tv_sbcount);

        layoutDetail = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.home_layout_album_detail, null);
        layoutTracks = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.common_layout_refresh_loadmore, null);
        recyclerView = layoutTracks.findViewById(R.id.rv);
        refreshLayout = layoutTracks.findViewById(R.id.refreshLayout);
        refreshLayout.setEnableRefresh(false);

        viewpager.setAdapter(new AlbumPagerAdapter());
        final CommonNavigator commonNavigator = new CommonNavigator(mContext);
        commonNavigator.setAdjustMode(true);
        commonNavigator.setAdapter(new NavigatorAdapter(Arrays.asList(tabs), viewpager, 125));
        magicIndicator.setNavigator(commonNavigator);
        ViewPagerHelper.bind(magicIndicator, viewpager);
        viewpager.setCurrentItem(1);

        mAlbumTrackAdapter = new AlbumTrackAdapter(R.layout.home_item_album_detail);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        recyclerView.setHasFixedSize(true);
        mAlbumTrackAdapter.bindToRecyclerView(recyclerView);


    }

    @Override
    public void initListener() {
        super.initListener();
        XmDownloadManager.getInstance().addDownloadStatueListener(this);
        XmPlayerManager.getInstance(mContext).addPlayerStatusListener(this);
        refreshLayout.setOnLoadMoreListener(this);
        viewpager.addOnPageChangeListener(this);
        mAlbumTrackAdapter.setOnItemClickListener(this);
        mAlbumTrackAdapter.setOnItemChildClickListener(this);
        addDisposable(RxView.clicks(fd(R.id.iv_sort))
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribe(unit -> {
                    isRefresh = true;
                    mSort = mSort.equals("asc") ? "desc" : "asc";
                    mViewModel.getTrackList(String.valueOf(mAlbumId), mSort);
                }));
    }

    @Override
    public void initData() {
        mViewModel.getAlbumDetail(String.valueOf(mAlbumId));
        mViewModel.getTrackList(String.valueOf(mAlbumId), mSort);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        clActionbar.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }


    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        mViewModel.getTrackList(String.valueOf(mAlbumId), mSort);
    }


    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {

        XmPlayerManager.getInstance(mContext).playList(mViewModel.getCommonTrackList(),position);

    }

    @Override
    public void onItemChildClick(final BaseQuickAdapter adapter, View view, final int position) {
        int id = view.getId();
        if (R.id.iv_download == id) {
            XmDownloadManager.getInstance().downloadSingleTrack(
                    mAlbumTrackAdapter.getData().get(position).getDataId(), null);
        }
    }

    private void updateDownloadStatus(Track track) {
        List<Track> tracks = mAlbumTrackAdapter.getData();
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getDataId() == track.getDataId()) {
                DownloadState downloadStatus = XmDownloadManager.getInstance()
                        .getSingleTrackDownloadStatus(tracks.get(i).getDataId());

                View ivDownload = mAlbumTrackAdapter.getViewByPosition(i, R.id.iv_download);
                View progress = mAlbumTrackAdapter.getViewByPosition(i, R.id.progressBar);
                View ivDownloadSucc = mAlbumTrackAdapter.getViewByPosition(i, R.id.iv_downloadsucc);
                switch (downloadStatus) {
                    case FINISHED:
                        ivDownloadSucc.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                        ivDownload.setVisibility(View.GONE);
                        break;
                    case STARTED:
                    case WAITING:
                        ivDownloadSucc.setVisibility(View.GONE);
                        progress.setVisibility(View.VISIBLE);
                        ivDownload.setVisibility(View.GONE);
                        break;
                    case STOPPED:
                    case NOADD:
                    case ERROR:
                        ivDownloadSucc.setVisibility(View.GONE);
                        progress.setVisibility(View.GONE);
                        ivDownload.setVisibility(View.VISIBLE);
                        break;
                }
            }
        }
    }
    private void updatePlayStatus() {
        Track track = XmPlayerManager.getInstance(mContext).getCurrSoundIgnoreKind(true);
        if(null==track){
            return;
        }
        List<Track> tracks = mAlbumTrackAdapter.getData();
        for (int i = 0; i < tracks.size(); i++) {
            View ivPlaying = mAlbumTrackAdapter.getViewByPosition(i, R.id.iv_playing);
            if(null!=ivPlaying){
                ivPlaying.setVisibility(tracks.get(i).getDataId() == track.getDataId()?View.VISIBLE:View.GONE);
            }
        }
    }
    private void updatePlayStatus(int currPos, int duration) {
        Track track = XmPlayerManager.getInstance(mContext).getCurrSoundIgnoreKind(true);
        if(null==track){
            return;
        }
        List<Track> tracks = mAlbumTrackAdapter.getData();
        for (int i = 0; i < tracks.size(); i++) {
            TextView tvHasplay = (TextView) mAlbumTrackAdapter.getViewByPosition(i, R.id.tv_hasplay);
            if(null!=tvHasplay&&tracks.get(i).getDataId() == track.getDataId()){
                tvHasplay.setText(getString(R.string.hasplay, 100 * currPos /duration));
            }
        }
    }
    @Override
    public void onWaiting(Track track) {
        updateDownloadStatus(track);
    }

    @Override
    public void onStarted(Track track) {
        updateDownloadStatus(track);
    }

    @Override
    public void onSuccess(Track track) {
        updateDownloadStatus(track);
    }

    @Override
    public void onError(Track track, Throwable throwable) {
        updateDownloadStatus(track);
        throwable.printStackTrace();
    }

    @Override
    public void onCancelled(Track track, Callback.CancelledException e) {
        updateDownloadStatus(track);
    }

    @Override
    public void onProgress(Track track, long l, long l1) {

    }

    @Override
    public void onRemoved() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        XmDownloadManager.getInstance().removeDownloadStatueListener(this);
        XmPlayerManager.getInstance(mContext).removePlayerStatusListener(this);
    }

    @Override
    protected int onBindLayout() {
        return R.layout.home_fragment_album_detail;
    }

    @Override
    protected String[] onBindBarTitleText() {
        return new String[]{"专辑详情"};
    }

    @Override
    protected Integer[] onBindBarRightIcon() {
        return new Integer[]{R.drawable.ic_common_share};
    }

    @Override
    protected int onBindBarRightStyle() {
        return BarStyle.RIGHT_ICON;
    }

    @Override
    public Class<AlbumDetailViewModel> onBindViewModel() {
        return AlbumDetailViewModel.class;
    }

    @Override
    public ViewModelProvider.Factory onBindViewModelFactory() {
        return ViewModelFactory.getInstance(mApplication);
    }

    @Override
    public void initViewObservable() {
        mViewModel.getAlbumSingleLiveEvent().observe(this, album -> {
            mAlbum = album;
            Glide.with(mContext).load(mAlbum.getCoverUrlMiddle()).into(ivCover);
            tvAlbum.setText(mAlbum.getAlbumTitle());
            tvAuthor.setText((String.format(getResources().getString(R.string.zhubo),
                    mAlbum.getAnnouncer().getNickname())));
            tvPlaycount.setText(String.format(getResources().getString(R.string.ci)
                    , ZhumulangmaUtil.toWanYi(mAlbum.getPlayCount())));
            tvTrackcount.setText(String.format(getResources().getString(R.string.gong_ji),
                    mAlbum.getIncludeTrackCount()));
            tvSbcount.setText(String.format(getResources().getString(R.string.sb)
                    , ZhumulangmaUtil.toWanYi(mAlbum.getSubscribeCount())));
        });

        mViewModel.getTracksSingleLiveEvent().observe(this, tracks -> {
            if (null == tracks || (mAlbumTrackAdapter.getData().size() == 0 &&
                    tracks.size() == 0)) {
                showNoDataView(true);
                return;
            }
            if (tracks.size() > 0) {
                if (isRefresh) {
                    mAlbumTrackAdapter.setNewData(tracks);
                    isRefresh = false;
                } else {
                    mAlbumTrackAdapter.addData(tracks);
                }
                refreshLayout.finishLoadMore();
            } else {
                refreshLayout.finishLoadMoreWithNoMoreData();
            }
        });
    }

    @Override
    public void onPlayStart() {
        updatePlayStatus();
    }

    @Override
    public void onPlayPause() {
        updatePlayStatus();
    }

    @Override
    public void onPlayStop() {
        updatePlayStatus();
    }

    @Override
    public void onSoundPlayComplete() {
        updatePlayStatus();
    }

    @Override
    public void onSoundPrepared() {
        updatePlayStatus();
    }

    @Override
    public void onSoundSwitch(PlayableModel playableModel, PlayableModel playableModel1) {
        updatePlayStatus();
    }

    @Override
    public void onBufferingStart() {

    }

    @Override
    public void onBufferingStop() {

    }

    @Override
    public void onBufferProgress(int i) {

    }

    @Override
    public void onPlayProgress(int i, int i1) {
        updatePlayStatus(i,i1);
    }


    @Override
    public boolean onError(XmPlayerException e) {
        return false;
    }

    class AlbumPagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return 2;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = position == 0 ? layoutDetail : layoutTracks;
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }
}
