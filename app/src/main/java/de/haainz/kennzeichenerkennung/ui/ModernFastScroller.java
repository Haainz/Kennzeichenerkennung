package de.haainz.kennzeichenerkennung.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import de.haainz.kennzeichenerkennung.R;

public class ModernFastScroller extends FrameLayout {

    private View handle;
    private View dimView;
    private TextView centerText;
    private RecyclerView recyclerView;
    private ListView listView;
    private boolean isAttached = false;

    public interface SectionIndexer {
        String getSectionText(int position);
    }

    public ModernFastScroller(@NonNull Context context) {
        this(context, null);
    }

    public ModernFastScroller(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ModernFastScroller(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_fast_scroller, this, true);
        handle = findViewById(R.id.fastscroll_handle);
        dimView = findViewById(R.id.fastscroll_dim);
        centerText = findViewById(R.id.fastscroll_center_text);
        
        handle.post(() -> handle.setPivotX(handle.getWidth()));
        
        setClickable(false);
        setVisibility(GONE);
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        if (this.recyclerView == recyclerView) return;
        this.recyclerView = recyclerView;
        isAttached = true;
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateVisibilityAndPosition();
            }
        });
        updateVisibilityAndPosition();
    }

    public void attachToListView(ListView listView) {
        if (this.listView == listView) return;
        this.listView = listView;
        isAttached = true;
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                updateVisibilityAndPosition();
            }
        });
        updateVisibilityAndPosition();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (handle != null) {
            handle.setPivotX(handle.getWidth());
        }
        updateVisibilityAndPosition();
    }

    private void updateVisibilityAndPosition() {
        if (!isAttached || handle.isSelected()) return;

        boolean canScroll = false;
        float progress = 0;

        if (recyclerView != null) {
            int offset = recyclerView.computeVerticalScrollOffset();
            int extent = recyclerView.computeVerticalScrollExtent();
            int range = recyclerView.computeVerticalScrollRange();
            if (range > extent) {
                canScroll = true;
                progress = (float) offset / (range - extent);
            }
        } else if (listView != null) {
            int visibleCount = listView.getChildCount();
            int totalCount = listView.getCount();
            if (totalCount > visibleCount) {
                canScroll = true;
                int firstVisible = listView.getFirstVisiblePosition();
                progress = (float) firstVisible / (totalCount - visibleCount);
            }
        }

        setVisibility(canScroll ? VISIBLE : GONE);
        if (canScroll) {
            setHandlePosition(progress);
        }
    }

    private void setHandlePosition(float progress) {
        int height = getHeight();
        int handleHeight = handle.getHeight();
        if (handleHeight == 0) return; // Not laid out yet
        handle.setY(progress * (height - handleHeight));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (getVisibility() != VISIBLE) return false;
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // Check if touch is in the right area (100dp from edge)
            float edgeThreshold = 100 * getResources().getDisplayMetrics().density;
            if (ev.getX() >= getWidth() - edgeThreshold) {
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float edgeThreshold = 100 * getResources().getDisplayMetrics().density;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getX() < getWidth() - edgeThreshold) return false;
                handle.setSelected(true);
                animateHandle(true);
                showOverlay();
                getParent().requestDisallowInterceptTouchEvent(true);
                // fall through
            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(true);
                float y = event.getY();
                float progress = Math.max(0, Math.min(1, y / getHeight()));
                setHandlePosition(progress);
                scrollTo(progress);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handle.setSelected(false);
                animateHandle(false);
                hideOverlay();
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void animateHandle(boolean active) {
        handle.animate().cancel();
        // Ensure pivot is at the right edge so it expands to the left
        handle.setPivotX(handle.getWidth());
        handle.animate()
                .scaleX(active ? 3.5f : 1f)
                .setDuration(150)
                .start();
    }

    private void scrollTo(float progress) {
        int count = 0;
        SectionIndexer indexer = null;

        if (recyclerView != null) {
            count = recyclerView.getAdapter() != null ? recyclerView.getAdapter().getItemCount() : 0;
            if (recyclerView.getAdapter() instanceof SectionIndexer) {
                indexer = (SectionIndexer) recyclerView.getAdapter();
            }
        } else if (listView != null) {
            count = listView.getAdapter() != null ? listView.getAdapter().getCount() : 0;
            if (listView.getAdapter() instanceof SectionIndexer) {
                indexer = (SectionIndexer) listView.getAdapter();
            }
        }

        if (count > 0) {
            int pos = (int) (progress * (count - 1));
            if (recyclerView != null) {
                recyclerView.scrollToPosition(pos);
            } else if (listView != null) {
                listView.setSelection(pos);
            }

            if (indexer != null) {
                String text = indexer.getSectionText(pos);
                centerText.setText(text);
                if (text.length() > 2) {
                    centerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35);
                } else {
                    centerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 110);
                }
            }
        }
    }

    private void showOverlay() {
        dimView.animate().cancel();
        dimView.setVisibility(VISIBLE);
        dimView.animate().alpha(1f).setDuration(200).setListener(null).start();

        centerText.animate().cancel();
        centerText.setVisibility(VISIBLE);
        centerText.animate().alpha(1f).setDuration(200).setListener(null).start();
    }

    private void hideOverlay() {
        dimView.animate().cancel();
        dimView.animate().alpha(0f).setDuration(200).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dimView.setVisibility(GONE);
            }
        }).start();

        centerText.animate().cancel();
        centerText.animate().alpha(0f).setDuration(200).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                centerText.setVisibility(GONE);
            }
        }).start();
    }
}