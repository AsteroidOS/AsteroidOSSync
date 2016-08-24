/*
 * Copyright (C) 2016 - Florent Revest <revestflo@gmail.com>
 *                      Doug Koellmer <dougkoellmer@hotmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.asteroidos.sync;

import com.idevicesinc.sweetblue.BleManager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.Window;

public class AlertManager implements BleManager.UhOhListener
{
	private final Context m_context;
	private final BleManager m_bleMngr;
	
	public AlertManager(Context context, BleManager bleMngr)
	{
		m_context = context;
		m_bleMngr = bleMngr;
		
		m_bleMngr.setListener_UhOh(this);
	}
	
	public void showBleNotSupported()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(m_context);
		final AlertDialog dialog = builder.create();
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		String dismiss = m_context.getResources().getString(R.string.generic_ok);
		String message = m_context.getResources().getString(R.string.ble_not_supported);

		OnClickListener clickListener = new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		};
		
		dialog.setMessage(message);
		dialog.setButton(DialogInterface.BUTTON_NEUTRAL, dismiss, clickListener);
		dialog.show();
	}

	@Override public void onEvent(UhOhEvent event)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(m_context);
		final AlertDialog dialog = builder.create();

		OnClickListener clickListener = new OnClickListener()
		{
			@Override public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
				
				if( which == DialogInterface.BUTTON_POSITIVE )
				{
					m_bleMngr.reset();
				}
			}
		};

		dialog.setTitle(event.uhOh().name());
		
		if( event.uhOh().getRemedy() == Remedy.RESET_BLE )
		{
			dialog.setMessage(m_context.getResources().getString(R.string.uhoh_message_nuke));
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, m_context.getResources().getString(R.string.uhoh_message_nuke_drop), clickListener);
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, m_context.getResources().getString(R.string.uhoh_message_nuke_cancel), clickListener);
		}
		else if( event.uhOh().getRemedy() == Remedy.RESTART_PHONE )
		{
			dialog.setMessage(m_context.getResources().getString(R.string.uhoh_message_phone_restart));
			dialog.setButton(DialogInterface.BUTTON_NEUTRAL, m_context.getResources().getString(R.string.uhoh_message_phone_restart_ok), clickListener);
		}
		else if( event.uhOh().getRemedy() == Remedy.WAIT_AND_SEE )
		{
			dialog.setMessage(m_context.getResources().getString(R.string.uhoh_message_weirdness));
			dialog.setButton(DialogInterface.BUTTON_NEUTRAL, m_context.getResources().getString(R.string.uhoh_message_weirdness_ok), clickListener);
		}
		
		dialog.show();
	}
}
