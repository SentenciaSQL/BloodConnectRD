package com.bloodconnect.bloodrequest.dto;

public record DonationProgress(
        long completedUnits,
        long pendingUnits,
        int progressPercent
) {
    public static DonationProgress of(int unitsRequired, long completedUnits) {
        long safeCompleted = Math.max(0, completedUnits);
        long pending = Math.max(0, unitsRequired - safeCompleted);
        int percent = unitsRequired <= 0
                ? 0
                : (int) Math.min(100, Math.round(safeCompleted * 100.0 / unitsRequired));
        return new DonationProgress(safeCompleted, pending, percent);
    }

    public boolean isFulfilled(int unitsRequired) {
        return completedUnits >= unitsRequired && unitsRequired > 0;
    }
}
