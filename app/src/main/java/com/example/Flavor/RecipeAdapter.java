package com.example.Flavor;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.Flavor.Models.Recipe;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    private List<Recipe> recipes;
    private OnRecipeClickListener clickListener;
    private OnRecipeLikeListener likeListener;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingRunnable;
    private static final long DOUBLE_CLICK_TIME = 300;

    // Cache author nicknames to avoid repeated Firebase queries
    private Map<String, String> authorCache = new HashMap<>();
    private DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

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

        // Date
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", new Locale("ru"));
        holder.dateText.setText(sdf.format(new Date(recipe.getTimestamp())));

        // Author
        String userId = recipe.getUserId();
        if (userId != null && !userId.isEmpty()) {
            if (authorCache.containsKey(userId)) {
                holder.authorText.setText(authorCache.get(userId));
            } else {
                holder.authorText.setText("Загрузка...");
                usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String nickname = snapshot.child("nickname").getValue(String.class);
                        String displayName = (nickname != null && !nickname.isEmpty())
                                ? nickname : recipe.getUserEmail();
                        if (displayName == null) displayName = "Пользователь";
                        authorCache.put(userId, displayName);
                        final String name = displayName;
                        new Handler(Looper.getMainLooper()).post(() -> holder.authorText.setText(name));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.authorText.setText("Пользователь");
                    }
                });
            }
        } else {
            holder.authorText.setText("Пользователь");
        }

        // Like button
        if (recipe.isSaved()) {
            holder.likeButton.setImageResource(R.drawable.heart_filled);
        } else {
            holder.likeButton.setImageResource(R.drawable.heart_outline);
        }

        holder.likeButton.setOnClickListener(v -> {
            boolean newLikeState = !recipe.isSaved();
            recipe.setSaved(newLikeState);
            holder.likeButton.setImageResource(newLikeState ? R.drawable.heart_filled : R.drawable.heart_outline);
            if (likeListener != null) {
                likeListener.onRecipeLike(recipe, holder.getAdapterPosition(), newLikeState);
            }
        });

        // Double-click to like, single click to open
        holder.cardView.setOnClickListener(v -> {
            if (pendingRunnable == null) {
                pendingRunnable = () -> {
                    if (clickListener != null) clickListener.onRecipeClick(recipe);
                    pendingRunnable = null;
                };
                handler.postDelayed(pendingRunnable, DOUBLE_CLICK_TIME);
            } else {
                handler.removeCallbacks(pendingRunnable);
                pendingRunnable = null;
                boolean newLikeState = !recipe.isSaved();
                recipe.setSaved(newLikeState);
                holder.likeButton.setImageResource(newLikeState ? R.drawable.heart_filled : R.drawable.heart_outline);
                if (likeListener != null) {
                    likeListener.onRecipeLike(recipe, holder.getAdapterPosition(), newLikeState);
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
        TextView authorText;
        TextView dateText;
        ImageView authorAvatarSmall;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            titleText = itemView.findViewById(R.id.recipeTitle);
            descriptionText = itemView.findViewById(R.id.recipeDescription);
            likeButton = itemView.findViewById(R.id.likeButton);
            authorText = itemView.findViewById(R.id.recipeAuthor);
            dateText = itemView.findViewById(R.id.recipeDate);
            authorAvatarSmall = itemView.findViewById(R.id.authorAvatarSmall);
        }
    }
}
