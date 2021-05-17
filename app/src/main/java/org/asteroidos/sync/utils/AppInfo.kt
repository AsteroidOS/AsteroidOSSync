package org.asteroidos.sync.utils

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable

/**
 * Appears to represent an app and its information such that it is presented to the user
 *
 *
 * - Copied from https://github.com/jensstein/oandbackup, used under MIT license
 *
 * - Edited by Doomsdayrs as of May 17th 2021. Kotlin conversion with some proper standards.
 *
 * @param mLabel Label of the application, Nullable
 * @param mPackageName Package name of the application, Nullable
 * @param isSystem Is this application a system app
 * @param isInstalled Is this application installed (??)
 * @param isChecked Is this application checked (If this is for the UI, please move it out)
 * @param isDisabled Is this application disabled by the OS
 * @param icon Icon of the application
 *
 * @author https://github.com/jensstein & https://github.com/doomsdayrs
 * @see <a href="https://github.com/jensstein">jensstein</a>
 * @see <a href="https://github.com/doomsdayrs">Doomsdayrs</a>
 * @see <a href="https://github.com/jensstein/oandbackup">oandbackup</a>
 */
data class AppInfo internal constructor(
	private val mLabel: String?,
	private val mPackageName: String?,

	val isSystem: Boolean = false,
	val isInstalled: Boolean = false,
	val isChecked: Boolean = false,
	var isDisabled: Boolean = false,

	@JvmField
	var icon: Bitmap? = null
) : Comparable<AppInfo>, Parcelable {

	/** Accessor for the package name of the application */
	val packageName: String?
		get() = mPackageName

	/** Accessor for the application label */
	val label: String?
		get() = mLabel

	/**
	 * Constructor as used by java code
	 *
	 * Note, Can be deprecated once the code base is further migrated to kotlin.
	 */
	constructor(packageName: String?, label: String?, system: Boolean, installed: Boolean) : this(
		mLabel = label,
		mPackageName = packageName,
		isSystem = system,
		isInstalled = installed,
	)

	/**
	 * Support constructor to handle [booleanArray] from [Parcel]
	 */
	internal constructor(
		label: String?,
		packageName: String?,
		booleanArray: BooleanArray,
		icon: Bitmap?
	) : this(
		mLabel = label,
		mPackageName = packageName,
		isSystem = booleanArray[0],
		isInstalled = booleanArray[1],
		isChecked = booleanArray[2],
		icon = icon,
	)

	/**
	 * Creates an [AppInfo] from a [Parcel]
	 */
	internal constructor(input: Parcel) : this(
		label = input.readString(),
		packageName = input.readString(),
		booleanArray = BooleanArray(4).apply { input.readBooleanArray(this) },
		icon = input.readParcelable(object {}.javaClass.classLoader)
	)

	override fun compareTo(other: AppInfo): Int =
		mLabel!!.compareTo(other.mLabel!!, ignoreCase = true)

	override fun toString(): String = "$mLabel : $mPackageName"

	override fun describeContents(): Int = 0

	override fun writeToParcel(out: Parcel, flags: Int) {
		out.writeString(mLabel)
		out.writeString(mPackageName)
		out.writeBooleanArray(booleanArrayOf(isSystem, isInstalled, isChecked))
		out.writeParcelable(icon, flags)
	}

	companion object {
		@JvmField
		val CREATOR: Parcelable.Creator<AppInfo> = object : Parcelable.Creator<AppInfo> {
			override fun createFromParcel(input: Parcel): AppInfo = AppInfo(input)

			override fun newArray(size: Int): Array<AppInfo?> = arrayOfNulls(size)
		}
	}
}