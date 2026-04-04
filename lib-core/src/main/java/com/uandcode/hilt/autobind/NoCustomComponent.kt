package com.uandcode.hilt.autobind

/**
 * Sentinel class used as the default value for [AutoBinds.installInCustomComponent],
 * [AutoBindsIntoSet.installInCustomComponent], and [AutoBindsIntoMap.installInCustomComponent].
 *
 * Has no runtime meaning. When a user specifies this class (the default), the processor
 * falls back to auto-detecting the component via scope annotations or standard [HiltComponent] enum.
 */
public class NoCustomComponent private constructor()
