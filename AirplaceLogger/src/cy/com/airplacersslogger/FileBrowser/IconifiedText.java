/*
* Copyright (c) 2011, KIOS Research Center and Data Management Systems Lab,
* University of Cyprus. All rights reserved.
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

package cy.com.airplacersslogger.FileBrowser;

import android.graphics.drawable.Drawable;

public class IconifiedText implements Comparable<IconifiedText> {

	private String mText = "";
	private Drawable mIcon;
	private boolean mSelectable = true;

	public IconifiedText(String text, Drawable bullet) {
		mIcon = bullet;
		mText = text;
	}

	public boolean isSelectable() {
		return mSelectable;
	}

	public void setSelectable(boolean selectable) {
		mSelectable = selectable;
	}

	public String getText() {
		return mText;
	}

	public void setText(String text) {
		mText = text;
	}

	public void setIcon(Drawable icon) {
		mIcon = icon;
	}

	public Drawable getIcon() {
		return mIcon;
	}

	/** Make IconifiedText comparable by its name */
	@Override
	public int compareTo(IconifiedText other) {
		if (this.mText != null)
			return this.mText.compareTo(other.getText());
		else
			throw new IllegalArgumentException();
	}
}
