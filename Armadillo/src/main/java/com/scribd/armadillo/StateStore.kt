package com.scribd.armadillo

import com.scribd.armadillo.actions.Action
import com.scribd.armadillo.actions.ClearErrorAction
import com.scribd.armadillo.error.MissingDataException
import com.scribd.armadillo.models.ArmadilloState
import io.reactivex.subjects.BehaviorSubject

internal interface StateStore {
    interface Initializer {
        fun init(state: ArmadilloState)
    }

    interface Modifier {
        fun dispatch(action: Action)
    }

    interface Provider {
        /**
         * emits the most recently emitted state and all the subsequent states when an observer subscribes to it.
         */
        val stateSubject: BehaviorSubject<ArmadilloState>

        val currentState: ArmadilloState
    }
}

internal class ArmadilloStateStore(private val reducer: Reducer) :
        StateStore.Modifier, StateStore.Provider, StateStore.Initializer {

    private companion object {
        const val TAG = "ArmadilloStateStore"
    }

    private val armadilloStateObservable = BehaviorSubject.create<ArmadilloState>()

    override fun init(state: ArmadilloState) = armadilloStateObservable.onNext(state)

    override fun dispatch(action: Action) {
        val newAppState = reducer.reduce(currentState, action)
        armadilloStateObservable.onNext(newAppState)

        if (currentState.error != null) {
            dispatch(ClearErrorAction)
        }
    }

    override val stateSubject: BehaviorSubject<ArmadilloState>
        get() = armadilloStateObservable

    override val currentState: ArmadilloState
        get() = armadilloStateObservable.value ?: throw MissingDataException("Armadillo's State should never be null")
}