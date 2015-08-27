/*
 * AirPlace:  The Airplace Project is an OpenSource Indoor and Outdoor
 * Localization solution using WiFi RSS (Receive Signal Strength).
 * The AirPlace Project consists of three parts:
 *
 *  1) The AirPlace Logger (Ideal for collecting RSS Logs)
 *  2) The AirPlace Server (Ideal for transforming the collected RSS logs
 *  to meaningful RadioMap files)
 *  3) The AirPlace Tracker (Ideal for using the RadioMap files for
 *  indoor localization)
 *
 * It is ideal for spaces where GPS signal is not sufficient.
 *
 * Authors:
 * C. Laoudias, G.Larkou, G. Constantinou, M. Constantinides, S. Nicolaou,
 *
 * Supervisors:
 * D. Zeinalipour-Yazti and C. G. Panayiotou
 *
 * Copyright (c) 2011, KIOS Research Center and Data Management Systems Lab (DMSL),
 * University of Cyprus. All rights reserved.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Υou should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/


/*

 * Copyright (c) 2010, Sony Ericsson Mobile Communication AB. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 *    * Redistributions of source code must retain the above copyright notice, this 
 *      list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 *    * Neither the name of the Sony Ericsson Mobile Communication AB nor the names
 *      of its contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cy.com.zoom;

import android.content.Context;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Listener for controlling zoom state through touch events
 */
public class LongPressZoomListener implements View.OnTouchListener {

	/**
	 * Enum defining listener modes. Before the view is touched the listener is
	 * in the UNDEFINED mode. Once touch starts it can enter either one of the
	 * other two modes: If the user scrolls over the view the listener will
	 * enter PAN mode, if the user lets his finger rest and makes a longpress
	 * the listener will enter ZOOM mode.
	 */
	private enum Mode {
		UNDEFINED, PAN, ZOOM
	}

	/** Time of tactile feedback vibration when entering zoom mode */
	private static final long VIBRATE_TIME = 50;

	/** Current listener mode */
	private Mode mMode = Mode.UNDEFINED;

	/** Zoom control to manipulate */
	private DynamicZoomControl mZoomControl;

	/** X-coordinate of previously handled touch event */
	private float mX;

	/** Y-coordinate of previously handled touch event */
	private float mY;

	/** X-coordinate of latest down event */
	private float mDownX;

	/** Y-coordinate of latest down event */
	private float mDownY;

	/** Velocity tracker for touch events */
	private VelocityTracker mVelocityTracker;

	/** Distance touch can wander before we think it's scrolling */
	private final int mScaledTouchSlop;

	/** Duration in ms before a press turns into a long press */
	private final int mLongPressTimeout;

	/** Vibrator for tactile feedback */
	private final Vibrator mVibrator;

	/** Maximum velocity for fling */
	private final int mScaledMaximumFlingVelocity;

	private final ClickPoint curClick = new ClickPoint(0, 0);
	
	/**
	 * Creates a new instance
	 * 
	 * @param context
	 *            Application context
	 */
	public LongPressZoomListener(Context context) {
		mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
		mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		mScaledMaximumFlingVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
		mVibrator = (Vibrator) context.getSystemService("vibrator");
	}

	/**
	 * Sets the zoom control to manipulate
	 * 
	 * @param control
	 *            Zoom control
	 */
	public void setZoomControl(DynamicZoomControl control) {
		mZoomControl = control;
	}
	
	public ClickPoint getClickPoint() {
		return curClick;
	}

	/**
	 * Runnable that enters zoom mode
	 */
	private final Runnable mLongPressRunnable = new Runnable() {
		public void run() {
			mMode = Mode.ZOOM;
			mVibrator.vibrate(VIBRATE_TIME);
		}
	};

	// implements View.OnTouchListener
	public boolean onTouch(View v, MotionEvent event) {
		final int action = event.getAction();
		final float x = event.getX();
		final float y = event.getY();

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(event);

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mZoomControl.stopFling();
			v.postDelayed(mLongPressRunnable, mLongPressTimeout);
			mDownX = x;
			mDownY = y;
			mX = x;
			mY = y;
			break;

		case MotionEvent.ACTION_MOVE: {
			final float dx = (x - mX) / v.getWidth();
			final float dy = (y - mY) / v.getHeight();
			
			if (mMode == Mode.ZOOM) {
				mZoomControl.zoom((float) Math.pow(20, -dy), mDownX / v.getWidth(), mDownY / v.getHeight());
			} else if (mMode == Mode.PAN) {
				mZoomControl.pan(-dx, -dy);
			} else {
				final float scrollX = mDownX - x;
				final float scrollY = mDownY - y;

				final float dist = (float) Math.sqrt(scrollX * scrollX + scrollY * scrollY);

				if (dist >= mScaledTouchSlop) {
					v.removeCallbacks(mLongPressRunnable);
					mMode = Mode.PAN;
				}
			}

			mX = x;
			mY = y;
			break;
		}

		case MotionEvent.ACTION_UP:
			if (mMode == Mode.PAN) {
				mVelocityTracker.computeCurrentVelocity(1000, mScaledMaximumFlingVelocity);
				mZoomControl.startFling(-mVelocityTracker.getXVelocity() / v.getWidth(), -mVelocityTracker.getYVelocity() / v.getHeight());
			} else {
				mZoomControl.startFling(0, 0);
			}
			mVelocityTracker.recycle();
			mVelocityTracker = null;
			v.removeCallbacks(mLongPressRunnable);
			mMode = Mode.UNDEFINED;
			
			
			int xDiff = (int) Math.abs(x - mDownX);
			int yDiff = (int) Math.abs(y - mDownY);
			if (xDiff < 8 && yDiff < 8) {
				curClick.setClickPoint(x, y);
				curClick.notifyObservers();
			}
			break;

			
		case MotionEvent.ACTION_POINTER_DOWN:
			mMode = Mode.ZOOM;
			break;
		case MotionEvent.ACTION_POINTER_UP:
			mMode = Mode.UNDEFINED;
			break;

		default:
			mVelocityTracker.recycle();
			mVelocityTracker = null;
			v.removeCallbacks(mLongPressRunnable);
			mMode = Mode.UNDEFINED;
			break;

		}

		return true;
	}


}
