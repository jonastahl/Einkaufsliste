package de.secretj12.ekl.listhelper;

import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import de.secretj12.ekl.R;

public class SimpleItemTouchHelperCallback
        extends ItemTouchHelper.Callback {
    private final ItemTouchHelperAdapter adapter;

    public SimpleItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        ItemViewHolder vh = (ItemViewHolder) viewHolder;
        int swipeFlags = 0;
        // User wants: Swipe RIGHT to CANCEL (turn gray), Swipe LEFT to ENABLE (restore)
        if (vh.isCancelAble()) swipeFlags |= ItemTouchHelper.RIGHT;
        if (vh.isEnableAble()) swipeFlags |= ItemTouchHelper.LEFT;
        
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, swipeFlags);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean canDropOver(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder current, @NonNull RecyclerView.ViewHolder target) {
        return (current.getItemViewType() == 1 && target.getItemViewType() == 1)
                || (target.getAdapterPosition() != 0 && current.getItemViewType() == 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        ItemViewHolder vh = (ItemViewHolder) viewHolder;
        vh.setSwiped();
        
        if (direction == ItemTouchHelper.RIGHT) {
            adapter.onItemCancel(vh.getAdapterPosition(), vh);
        } else {
            adapter.onItemEnable(vh.getAdapterPosition(), vh);
        }
        
        // Immediate reset to avoid visual artifacts
        View swipeContent = vh.itemView.findViewById(R.id.swipeContent);
        if (swipeContent != null) {
            swipeContent.setTranslationX(0);
        }
        adapter.onItemUpdated(vh.getAdapterPosition(), vh);
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE
                && ItemTouchHelper.ACTION_STATE_DRAG == actionState
                && viewHolder instanceof ItemTouchHelperViewHolder) {
            ((ItemTouchHelperViewHolder) viewHolder).onItemSelected();
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        adapter.onActionEnded();
        if (viewHolder instanceof ItemTouchHelperViewHolder) {
            ((ItemTouchHelperViewHolder) viewHolder).onItemClear();
        }
        
        View swipeContent = viewHolder.itemView.findViewById(R.id.swipeContent);
        if (swipeContent != null) {
            swipeContent.setTranslationX(0);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            ItemViewHolder vh = (ItemViewHolder) viewHolder;
            if (vh.isSwiped()) return;

            // Swiping RIGHT (dX > 0) -> Mark as Done (Green requested)
            // Swiping LEFT (dX < 0) -> Restore (Red requested)
            if (dX > 0) {
                vh.onItemSwipeCancel();
            } else if (dX < 0) {
                vh.onItemSwipeEnable();
            }
            vh.onSwipeProgress(dX);

            View swipeContent = viewHolder.itemView.findViewById(R.id.swipeContent);
            if (swipeContent != null) {
                float width = (float) viewHolder.itemView.getWidth();
                float translationX;
                if (Math.abs(dX) < width * 0.3f) {
                    translationX = dX;
                } else {
                    float extra = Math.abs(dX) - width * 0.3f;
                    translationX = Math.signum(dX) * (width * 0.3f + extra * 0.4f);
                }
                swipeContent.setTranslationX(translationX);
            }
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}
