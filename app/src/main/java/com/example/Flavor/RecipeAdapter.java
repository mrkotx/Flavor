package com.example.Flavor;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.Flavor.Models.Recipe;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    private List<Recipe> recipes;
    private OnRecipeClickListener clickListener;
    private OnRecipeLikeListener likeListener;

    // Для определения двойного клика
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingRunnable;
    private static final long DOUBLE_CLICK_TIME = 300;

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    public interface OnRecipeLikeListener {
        void onRecipeLike(Recipe recipe, int position, boolean isLiked);
    }

    public RecipeAdapter(List<Recipe> recipes,
                         OnRecipeClickListener clickListener,
                         OnRecipeLikeListener likeListener) {
        this.recipes = recipes;
        this.clickListener = clickListener;
        this.likeListener = likeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        holder.titleText.setText(recipe.getTitle());
        holder.descriptionText.setText(recipe.getDescription());

        // Устанавливаем иконку сердечка (аутлайн или заполненное)
        if (recipe.isSaved()) {
            holder.likeButton.setImageResource(R.drawable.heart_filled);
        } else {
            holder.likeButton.setImageResource(R.drawable.heart_outline);
        }

        // Обработка клика по сердечку
        holder.likeButton.setOnClickListener(v -> {
            boolean newLikeState = !recipe.isSaved();
            recipe.setSaved(newLikeState);

            // Меняем иконку
            if (newLikeState) {
                holder.likeButton.setImageResource(R.drawable.heart_filled);
            } else {
                holder.likeButton.setImageResource(R.drawable.heart_outline);
            }

            if (likeListener != null) {
                likeListener.onRecipeLike(recipe, position, newLikeState);
            }
        });

        // Обработка двойного клика по карточке
        holder.cardView.setOnClickListener(v -> {
            if (pendingRunnable == null) {
                // Первый клик - ждём второй
                pendingRunnable = () -> {
                    // Одиночный клик - открываем рецепт
                    if (clickListener != null) {
                        clickListener.onRecipeClick(recipe);
                    }
                    pendingRunnable = null;
                };
                handler.postDelayed(pendingRunnable, DOUBLE_CLICK_TIME);
            } else {
                // Второй клик - отменяем одиночный и лайкаем
                handler.removeCallbacks(pendingRunnable);
                pendingRunnable = null;

                // Лайкаем рецепт
                boolean newLikeState = !recipe.isSaved();
                recipe.setSaved(newLikeState);

                if (newLikeState) {
                    holder.likeButton.setImageResource(R.drawable.heart_filled);
                } else {
                    holder.likeButton.setImageResource(R.drawable.heart_outline);
                }

                if (likeListener != null) {
                    likeListener.onRecipeLike(recipe, position, newLikeState);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes == null ? 0 : recipes.size();
    }

    public void updateRecipes(List<Recipe> newRecipes) {
        this.recipes = newRecipes;
        notifyDataSetChanged();
    }

    public void updateLikeStatus(int position, boolean isLiked) {
        if (position >= 0 && position < recipes.size()) {
            recipes.get(position).setSaved(isLiked);
            notifyItemChanged(position);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView titleText;
        TextView descriptionText;
        ImageButton likeButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            titleText = itemView.findViewById(R.id.recipeTitle);
            descriptionText = itemView.findViewById(R.id.recipeDescription);
            likeButton = itemView.findViewById(R.id.likeButton);
        }
    }
}