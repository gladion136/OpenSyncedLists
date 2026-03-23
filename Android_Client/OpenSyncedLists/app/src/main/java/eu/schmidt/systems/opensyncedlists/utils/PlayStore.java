/*
 * Copyright (C) 2025  Etienne Schmidt (eschmidt@schmidt-ti.eu)
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package eu.schmidt.systems.opensyncedlists.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import eu.schmidt.systems.opensyncedlists.BuildConfig;

public class PlayStore {

    public static void askForPlayStoreReview(Activity context) {
        if (!BuildConfig.ENABLE_PLAY_REVIEW) {
            Log.d(
                "PlayStoreReview",
                "Play Store review disabled in this build variant"
            );
            return;
        }

        try {
            Class.forName(
                "com.google.android.play.core.review.ReviewManagerFactory"
            );
        } catch (ClassNotFoundException e) {
            Log.e(
                "PlayStoreReview",
                "Play Core library not available in this build variant"
            );
            return;
        }

        com.google.android.play.core.review.ReviewManager manager =
            com.google.android.play.core.review.ReviewManagerFactory.create(
                context
            );
        com.google.android.gms.tasks.Task<
            com.google.android.play.core.review.ReviewInfo
        > request = manager.requestReviewFlow();
        request.addOnCompleteListener(task_info -> {
            if (task_info.isSuccessful()) {
                com.google.android.play.core.review.ReviewInfo reviewInfo =
                    task_info.getResult();
                Log.d(
                    "PlayStoreReview",
                    "ReviewInfo object created successfully"
                );

                com.google.android.gms.tasks.Task<Void> flow =
                    manager.launchReviewFlow(context, reviewInfo);
                flow.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(
                            "PlayStoreReview",
                            "Review flow launched successfully"
                        );
                    } else {
                        Log.e(
                            "PlayStoreReview",
                            "Error launching review flow: " +
                                task.getException()
                        );
                    }
                });
            } else {
                Log.e(
                    "PlayStoreReview",
                    "Error requesting review flow (play store) "
                );
            }
        });
    }
}
