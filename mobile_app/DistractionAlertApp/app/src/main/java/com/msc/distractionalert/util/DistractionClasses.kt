package com.msc.distractionalert.util

/**
 * The 10 classes the on-board MobileNetV2 model was trained on. Kept as a fixed,
 * ordered list (rather than deriving it from whatever happens to be in the alert
 * history) so the stats chart always shows every class in the same order and
 * color, even ones with zero alerts so far.
 */
object DistractionClasses {

    val ALL = listOf(
        "safe_driving",
        "texting_right",
        "phone_right",
        "texting_left",
        "phone_left",
        "adjusting_radio",
        "drinking",
        "reaching_behind",
        "hair_or_makeup",
        "talking_to_passenger"
    )

    fun displayName(className: String): String = className
        .split("_")
        .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
}
