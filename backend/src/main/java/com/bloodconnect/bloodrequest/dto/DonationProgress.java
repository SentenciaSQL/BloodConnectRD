package com.bloodconnect.bloodrequest.dto;

public record DonationProgress(
        long completedUnits,
        long pendingUnits,
        int progressPercent,
        double progress
) {
    public static DonationProgress of(int unitsRequired, long completedUnits) {
        long safeCompleted = Math.max(0, completedUnits);
        long pending = Math.max(0, unitsRequired - safeCompleted);
        double ratio = unitsRequired <= 0
                ? 0.0
                : Math.min(1.0, Math.max(0.0, (double) safeCompleted / (double) unitsRequired));
        int percent = (int) Math.round(ratio * 100.0);
        return new DonationProgress(safeCompleted, pending, percent, ratio);
    }

    public boolean isFulfilled(int unitsRequired) {
        return completedUnits >= unitsRequired && unitsRequired > 0;
    }
}
