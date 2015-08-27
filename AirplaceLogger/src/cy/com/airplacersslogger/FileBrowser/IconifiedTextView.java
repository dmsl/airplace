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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IconifiedTextView extends LinearLayout {

	private TextView mText;
	private ImageView mIcon;

	public IconifiedTextView(Context context, IconifiedText aIconifiedText) {
		super(context);

		/*
		 * First Icon and the Text to the right (horizontal), not above and
		 * below (vertical)
		 */
		this.setOrientation(HORIZONTAL);

		mIcon = new ImageView(context);
		mIcon.setImageDrawable(aIconifiedText.getIcon());
		// left, top, right, bottom
		mIcon.setPadding(0, 2, 5, 0); // 5px to the right

		addView(mIcon, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		mText = new TextView(context);
		mText.setText(aIconifiedText.getText());
		
		/* Now the text (after the icon) */
		addView(mText, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}

	public void setText(String words) {
		mText.setText(words);
	}

	public void setIcon(Drawable bullet) {
		mIcon.setImageDrawable(bullet);
	}
}