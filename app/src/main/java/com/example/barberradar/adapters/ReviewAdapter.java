package com.example.barberradar.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barberradar.R;
import com.example.barberradar.models.Review;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final List<Review> reviewList;
    private final Context context;
    private final OnReviewActionListener listener;

    public interface OnReviewActionListener {
        void onEditReview(Review review);
    }

    public ReviewAdapter(Context context, List<Review> reviewList, OnReviewActionListener listener) {
        this.context = context;
        this.reviewList = reviewList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        
        holder.userName.setText(review.getUserName());
        holder.ratingBar.setRating(review.getRating());
        holder.reviewText.setText(review.getReviewText());
        holder.shopName.setText(review.getShopName());
        
        // Format timestamp
        if (review.getTimestamp() != null) {
            Timestamp timestamp = review.getTimestamp();
            Date date = timestamp.toDate();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.reviewDate.setText(dateFormat.format(date));
        } else {
            holder.reviewDate.setText("Just now");
        }
        
        // Set edit action if the listener is provided
        if (listener != null) {
            holder.itemView.setOnLongClickListener(v -> {
                listener.onEditReview(review);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView userName, reviewText, reviewDate, shopName;
        RatingBar ratingBar;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.review_username);
            reviewText = itemView.findViewById(R.id.review_text);
            reviewDate = itemView.findViewById(R.id.review_date);
            ratingBar = itemView.findViewById(R.id.review_rating_bar);
            shopName = itemView.findViewById(R.id.review_shop_name);
        }
    }
}
