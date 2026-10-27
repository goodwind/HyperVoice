package io.github.goodwind.hypervoice.core

/** Whether what the app needs is in place, from what the app can see of the TV without any help. */
object SetupStatus {
    enum class Item { Permission, Accessibility, Recognizer, Key, RemoteMic }

    class Check(val item: Item, val ok: Boolean)

    /** [canRepair]: the app may write the secure settings itself (it was given WRITE_SECURE_SETTINGS at setup). */
    data class Snapshot(
        val audioPermission: Boolean,
        val accessibilityOn: Boolean,
        val recognizerIsOurs: Boolean,
        val hasKey: Boolean,
        val remoteMicFound: Boolean,
        val canRepair: Boolean,
    )

    fun checks(s: Snapshot): List<Check> = listOf(
        Check(Item.Permission, s.audioPermission),
        Check(Item.Accessibility, s.accessibilityOn),
        Check(Item.Recognizer, s.recognizerIsOurs),
        Check(Item.Key, s.hasKey),
        Check(Item.RemoteMic, s.remoteMicFound),
    )

    /** Ready to use. The remote not being found does not count against it: it may only be asleep. */
    fun ready(s: Snapshot) = s.audioPermission && s.accessibilityOn && s.recognizerIsOurs && s.hasKey

    /** What the app can put right by itself: the two secure settings, if it may write them. */
    fun repairable(s: Snapshot): List<Item> = buildList {
        if (!s.canRepair) return@buildList
        if (!s.accessibilityOn) add(Item.Accessibility)
        if (!s.recognizerIsOurs) add(Item.Recognizer)
    }
}
