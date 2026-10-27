package io.github.goodwind.hypervoice.core

/** What the remote's microphone button does when it goes down. */
object KeyPlan {
    enum class Action { Ignore, Dictate, PressMicButton, OpenSearch, NeedKey }

    /**
     * Only a field to type into, or a button that leads to one, is a reason to do anything. With neither, nothing happens:
     * no recording, no request, no note on the screen. An app's own microphone button is preferred to a search
     * button, and a search button is pressed only when the option is on and there is a key to use afterwards.
     */
    fun decide(
        recognitionActive: Boolean,
        fieldFocused: Boolean,
        pressMicOption: Boolean,
        micButtonFound: Boolean,
        openSearchOption: Boolean,
        searchButtonFound: Boolean,
        hasKey: Boolean,
    ): Action = when {
        recognitionActive -> Action.Ignore
        fieldFocused -> if (hasKey) Action.Dictate else Action.NeedKey
        pressMicOption && micButtonFound -> Action.PressMicButton
        openSearchOption && searchButtonFound && hasKey -> Action.OpenSearch
        else -> Action.Ignore
    }
}
