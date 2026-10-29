package com.bc.notivault.data.model

enum class InteractionStatus(
    val displayName: String,
    val isConfirmed: Boolean,
    val description: String
) {
    POSTED(
        displayName = "Posted",
        isConfirmed = true,
        description = "Notification received and recorded."
    ),
    UPDATED(
        displayName = "Updated",
        isConfirmed = true,
        description = "Notification contents updated by the posting application."
    ),
    REMOVED(
        displayName = "Removed",
        isConfirmed = true,
        description = "Notification removed by app, channel configuration, or system lifecycle."
    ),
    DISMISSED(
        displayName = "Dismissed",
        isConfirmed = true,
        description = "User manually dismissed or swiped away the notification."
    ),
    OPENED(
        displayName = "Opened",
        isConfirmed = true,
        description = "User tapped the notification (confirmed by system click reason)."
    ),
    UNKNOWN(
        displayName = "Unknown",
        isConfirmed = false,
        description = "Interaction outcome could not be reliably determined."
    )
}
