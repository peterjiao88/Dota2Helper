package com.fangxu.dota2helper.ui.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fangxu.dota2helper.R;
import com.fangxu.dota2helper.callback.WatchedVideoSelectCountCallback;
import com.fangxu.dota2helper.ui.widget.TickButton;
import com.youku.service.download.DownloadInfo;
import com.youku.service.download.DownloadManager;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.Bind;

/**
 * Created by dear33 on 2016/7/27.
 */
public class CachingVideoAdapter extends BaseCacheVideoAdapter {
    private int mCurrentCachingPos = -1;
    private int mPauseCount = 0;

    private HeaderViewHolder mHeaderViewHolder;
    private HideEditCallback mHideCallback;

    public interface HideEditCallback {
        void onEditShouldHide();
    }

    public CachingVideoAdapter(Context context) {
        this(context, null);
    }

    public CachingVideoAdapter(Context context, WatchedVideoSelectCountCallback callback) {
        super(context, callback);
    }

    public void setHideEditCallback(HideEditCallback callback) {
        mHideCallback = callback;
    }

    public void setPauseCount(int pauseCount) {
        mPauseCount = pauseCount;
    }

    public void updateDownloadingView(DownloadInfo downloadInfo) {
        if (mCurrentCachingPos != -1) {
            setItem(mCurrentCachingPos, downloadInfo);
            notifyItemChanged(mCurrentCachingPos);
        } else {
            for (int i = 0, count = getItemCount(); i < count; i++) {
                DownloadInfo info = getItem(i);
                if (info == null) {
                    continue;
                }
                if (info.videoid.equals(downloadInfo.videoid)) {
                    mCurrentCachingPos = i;
                    setItem(mCurrentCachingPos, downloadInfo);
                    notifyItemChanged(mCurrentCachingPos);
                    break;
                }
            }
        }
    }

    public void deleteDownloadedView() {
        updateData();
    }

    @Override
    public void updateState(boolean isEditState) {
        setHasHeader(!isEditState && !mData.isEmpty());
        super.updateState(isEditState);
    }

    @Override
    public void setData(List<DownloadInfo> data) {
        setHasHeader(!mIsEditState && !data.isEmpty());
        super.setData(data);
    }

    private void updateData() {
        mData.clear();
        Set<String> selectVideos = null;
        final boolean reCalcSelectVideosCount = !mSelectedVideos.isEmpty();
        if (reCalcSelectVideosCount) {
            selectVideos = new HashSet<>();
        }

        Iterator iterator = DownloadManager.getInstance().getDownloadingData().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            DownloadInfo info = (DownloadInfo) entry.getValue();
            if (reCalcSelectVideosCount && mSelectedVideos.contains(info.videoid)) {
                selectVideos.add(info.videoid);
            }
            mData.add(info);
        }

        mCurrentCachingPos = -1;
        if (reCalcSelectVideosCount) {
            mSelectedVideos = selectVideos;
        }

        if (reCalcSelectVideosCount) {
            mCountCallback.onWatchedVideoSelect(mSelectedVideos.size());
        }
        if (mData.isEmpty()) {
            updateState(false);
            if (mHideCallback != null) {
                mHideCallback.onEditShouldHide();
            }
        } else {
            notifyDataSetChanged();
        }
    }

    @Override
    protected void deleteCache(DownloadInfo downloadInfo) {
        DownloadManager.getInstance().deleteDownloading(downloadInfo.taskId);
    }

    @Override
    public CommonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CommonViewHolder viewHolder = null;
        if (viewType == ITEM_HEADER) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_header_caching_controller, parent, false);
            mHeaderViewHolder = new HeaderViewHolder(view);
            return mHeaderViewHolder;
        } else if (viewType == ITEM_NORMAL) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_caching_video, parent, false);
            viewHolder = new CachingVideoViewHolder(view);
        }
        return viewHolder;
    }

    @Override
    protected void onClickHeader() {
        super.onClickHeader();
        TextView controller = mHeaderViewHolder.getController();
        if (controller.getText() == mContext.getResources().getString(R.string.pause_all)) {
            for (DownloadInfo info : mData) {
                if (info.state == DownloadInfo.STATE_DOWNLOADING
                        || info.state == DownloadInfo.STATE_WAITING
                        || info.state == DownloadInfo.STATE_INIT
                        || info.state == DownloadInfo.STATE_EXCEPTION) {
                    DownloadManager.getInstance().pauseDownload(info.taskId);
                }
            }
            mPauseCount = getItemCount() - 1;
        } else {
            for (DownloadInfo info : mData) {
                if (info.state == DownloadInfo.STATE_PAUSE) {
                    DownloadManager.getInstance().startDownload(info.taskId);
                }
            }
            mPauseCount = 0;
        }
        updateData();
    }

    @Override
    protected void onClickItem(int position) {
        super.onClickItem(position);
        if (!mIsEditState) {
            DownloadInfo info = getItem(position);
            if (info.state == DownloadInfo.STATE_DOWNLOADING
                    || info.state == DownloadInfo.STATE_WAITING
                    || info.state == DownloadInfo.STATE_INIT
                    || info.state == DownloadInfo.STATE_EXCEPTION) {
                DownloadManager.getInstance().pauseDownload(info.taskId);
                mPauseCount++;
            } else if (info.state == DownloadInfo.STATE_PAUSE) {
                DownloadManager.getInstance().startDownload(info.taskId);
                mPauseCount--;
            }
            updateData();
        }
    }

    public class HeaderViewHolder extends CommonViewHolder {
        @Bind(R.id.tv_controller_header)
        TextView mController;
        @Bind(R.id.iv_icon)
        ImageView mPlayIcon;

        public HeaderViewHolder(View itemView) {
            super(itemView);
        }

        public TextView getController() {
            return mController;
        }

        @Override
        public void fillView(int position) {
            if (mPauseCount < getItemCount() - 1) {
                mController.setText(R.string.pause_all);
                mPlayIcon.setBackgroundResource(R.drawable.ic_pause_all);
            } else {
                mController.setText(R.string.begin_all);
                mPlayIcon.setBackgroundResource(R.drawable.ic_play_all);
            }
        }
    }

    public class CachingVideoViewHolder extends CommonViewHolder {
        @Bind(R.id.iv_background)
        ImageView mBackground;
        @Bind(R.id.fl_alpha_layer)
        FrameLayout mAlphaLayer;
        @Bind(R.id.tv_title)
        TextView mTitle;
        @Bind(R.id.tb_select)
        TickButton mTickButton;
        @Bind(R.id.tv_video_size)
        TextView mVideoSize;
        @Bind(R.id.progressbar)
        ProgressBar mProgressBar;
        @Bind(R.id.tv_cached_size)
        TextView mCachedSize;
        @Bind(R.id.tv_cache_state)
        TextView mCacheState;

        public CachingVideoViewHolder(View itemView) {
            super(itemView);
            mProgressBar.setMax(100);
        }

        @Override
        public void fillView(int position) {
            DownloadInfo info = getItem(position);
            Glide.with(mContext).load(info.imgUrl).placeholder(R.drawable.img_background_default).into(mBackground);
            mTitle.setText(info.title);
            mVideoSize.setText(getVideoSize(info.size));
            mCachedSize.setText(getVideoSize(info.downloadedSize));

            if (info.state == DownloadInfo.STATE_DOWNLOADING) {
                mCacheState.setText(R.string.downloading);
                mAlphaLayer.setVisibility(View.GONE);
            } else if (info.state == DownloadInfo.STATE_PAUSE) {
                mCacheState.setText(R.string.pausing);
                mAlphaLayer.setVisibility(View.VISIBLE);
            } else if (info.state == DownloadInfo.STATE_INIT || info.state == DownloadInfo.STATE_EXCEPTION
                    || info.state == DownloadInfo.STATE_WAITING) {
                mCacheState.setText(R.string.waiting);
                mAlphaLayer.setVisibility(View.GONE);
            }

            mProgressBar.setProgress((int) info.progress);

            if (mIsEditState) {
                mTickButton.setVisibility(View.VISIBLE);
                if (mSelectedVideos.contains(info.videoid)) {
                    mTickButton.setSelected(true);
                } else {
                    mTickButton.setSelected(false);
                }
            } else {
                mTickButton.setVisibility(View.GONE);
            }
        }
    }
}
