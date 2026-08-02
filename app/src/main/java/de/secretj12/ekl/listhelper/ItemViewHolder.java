package de.secretj12.ekl.listhelper;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import de.secretj12.ekl.Database.Group;
import de.secretj12.ekl.Database.ListItem;
import de.secretj12.ekl.Listener.ItemChangeListener;
import de.secretj12.ekl.Listener.Receiver;
import de.secretj12.ekl.R;

class ItemViewHolder extends RecyclerView.ViewHolder
        implements ItemTouchHelperViewHolder {
    private boolean cancelled;

    private final TextView textView;
    private final ImageView handleView;
    private final ImageButton add_item;
    private final LinearLayout background;
    private final View swipe_content;
    private final ImageView swipeIconLeft;
    private final ImageView swipeIconRight;
    private final int position;
    private ListItem item;
    private boolean swiped;

    private int listenerID;

    public ItemViewHolder(@NonNull View itemView, ViewGroup viewGroup) {
        super(itemView);

        textView = itemView.findViewById(R.id.text);
        handleView = itemView.findViewById(R.id.drag_handle);
        add_item = itemView.findViewById(R.id.add_item);
        background = itemView.findViewById(R.id.background);
        swipe_content = itemView.findViewById(R.id.swipeContent);
        swipeIconLeft = itemView.findViewById(R.id.swipe_icon_left);
        swipeIconRight = itemView.findViewById(R.id.swipe_icon_right);
        position = -1;
        swiped = false;

        listenerID = -1;
    }

    @SuppressLint("ClickableViewAccessibility")
    void setItem(ListItem item, int position, OnStartDragListener onStartDragListener) {
        if (this.item != null)
            this.item.resetItemListener(listenerID);

        resetSwipeUI();
        this.item = item;
        swiped = false;

        handleView.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                onStartDragListener.onStartDrag(ItemViewHolder.this);
            return false;
        });

        listenerID = item.setItemListener(new ItemChangeListener() {
            @Override
            public void onNameChange(String name) {
                textView.setText(name);
            }

            @Override
            public void onStateChanged(boolean cancelled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    itemView.setTransitionAlpha(1f);

                ItemViewHolder.this.cancelled = cancelled;
                com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) itemView;
                int color;
                if (cancelled) {
                    color = card.getResources().getColor(item instanceof Group ? R.color.cancelledGroup : R.color.cancelledItem);
                    textView.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END);
                    swipe_content.setAlpha(0.6f);
                } else {
                    color = card.getResources().getColor(item instanceof Group ? R.color.notCancelledGroup : R.color.notCancelledItem);
                    textView.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
                    swipe_content.setAlpha(1.0f);
                }
                card.setCardBackgroundColor(color);
                swipe_content.setBackgroundColor(color);
            }
        });

        textView.setLongClickable(true);
        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                edit();
                return true;
            }
        });

        if (item instanceof Group) {
            add_item.setOnClickListener(view -> addItem());
        }
    }

    private void addItem() {
        View dialogview = LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_name_input, null);
        EditText input = dialogview.findViewById(R.id.input);
        input.setHint(itemView.getContext().getString(R.string.name));
        new AlertDialog.Builder(itemView.getContext())
                .setTitle(itemView.getContext().getString(R.string.add_item))
                .setView(dialogview)
                .setPositiveButton(itemView.getContext().getString(R.string.add), (DialogInterface dialogInterface, int i) -> {
                    ((Group) item).addItem(input.getText().toString(), new Receiver() {
                        @Override
                        public void onFailure() {
                            Toast.makeText(itemView.getContext(), R.string.item_creation_failed, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNeutralButton(itemView.getContext().getString(R.string.cancel), (DialogInterface dialogInterface, int i) -> {
                    dialogInterface.dismiss();
                })
                .show();
    }

    private void edit() {
        View dialogview = LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_name_input, null);
        EditText input = dialogview.findViewById(R.id.input);
        input.setText(textView.getText());
        new AlertDialog.Builder(itemView.getContext())
                .setTitle(itemView.getContext().getString(R.string.edit_item))
                .setView(dialogview)
                .setPositiveButton(itemView.getContext().getString(R.string.save), (DialogInterface dialogInterface, int i) -> {
                    item.setName(input.getText().toString());
                })
                .setNegativeButton(itemView.getContext().getString(R.string.cancel), (DialogInterface dialogInterface, int i) -> {
                    dialogInterface.dismiss();
                })
                .setNeutralButton(itemView.getContext().getString(R.string.delete), (DialogInterface dialogInterface, int i) -> {
                    item.delete();
                })
                .show();
    }

    void deleteItem() {
        item.delete();
    }

    void setCancelled(boolean cancelled) {
        item.setCancel(cancelled);
    }

    boolean isEnableAble() {
        return item.isEnableAble();
    }

    boolean isCancelAble() {
        return item.isCancelAble();
    }

    private void resetSwipeUI() {
        swipe_content.setTranslationX(0);
        background.setBackgroundColor(Color.TRANSPARENT);
        if (swipeIconLeft != null) {
            swipeIconLeft.setVisibility(View.INVISIBLE);
            swipeIconLeft.setAlpha(0f);
        }
        if (swipeIconRight != null) {
            swipeIconRight.setVisibility(View.INVISIBLE);
            swipeIconRight.setAlpha(0f);
        }
        swipe_content.setAlpha(cancelled ? 0.6f : 1.0f);
    }

    @Override
    public void onItemSelected() {
        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) itemView;
        card.setCardBackgroundColor(Color.LTGRAY);
        swipe_content.setBackgroundColor(Color.LTGRAY);
    }

    @Override
    public void onItemClear() {
        resetSwipeUI();
        
        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) itemView;
        int color;
        if (cancelled) {
            color = card.getResources().getColor(item instanceof Group ? R.color.cancelledGroup : R.color.cancelledItem);
            swipe_content.setAlpha(0.6f);
        } else {
            color = card.getResources().getColor(item instanceof Group ? R.color.notCancelledGroup : R.color.notCancelledItem);
            swipe_content.setAlpha(1.0f);
        }
        card.setCardBackgroundColor(color);
        swipe_content.setBackgroundColor(color);
    }

    @Override
    public void onItemSwipeCancel() {
        background.setBackgroundColor(swipe_content.getResources().getColor(R.color.colorYes, swipe_content.getContext().getTheme()));
        if (swipeIconLeft != null) {
            swipeIconLeft.setVisibility(View.VISIBLE);
            swipeIconLeft.setAlpha(1.0f);
        }
        if (swipeIconRight != null) swipeIconRight.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onItemSwipeEnable() {
        background.setBackgroundColor(swipe_content.getResources().getColor(R.color.colorNo, swipe_content.getContext().getTheme()));
        if (swipeIconRight != null) {
            swipeIconRight.setVisibility(View.VISIBLE);
            swipeIconRight.setAlpha(1.0f);
        }
        if (swipeIconLeft != null) swipeIconLeft.setVisibility(View.INVISIBLE);
    }

    public void onSwipeProgress(float dX) {
        float width = (float) itemView.getWidth();
        float progress = Math.min(Math.abs(dX) / (width * 0.4f), 1.0f);
        
        if (dX > 0) { // Swiping Right (to Mark as Done)
            if (swipeIconLeft != null) {
                swipeIconLeft.setVisibility(View.VISIBLE);
                swipeIconLeft.setAlpha(progress);
            }
            if (swipeIconRight != null) swipeIconRight.setVisibility(View.INVISIBLE);
            if (!cancelled) {
                swipe_content.setAlpha(1.0f - (progress * 0.4f));
            }
        } else if (dX < 0) { // Swiping Left (to Restore)
            if (swipeIconRight != null) {
                swipeIconRight.setVisibility(View.VISIBLE);
                swipeIconRight.setAlpha(progress);
            }
            if (swipeIconLeft != null) swipeIconLeft.setVisibility(View.INVISIBLE);
            if (cancelled) {
                swipe_content.setAlpha(0.6f + (progress * 0.4f));
            }
        }
    }

    void setSwiped() {
        swiped = true;
    }

    boolean isSwiped() {
        return swiped;
    }
}